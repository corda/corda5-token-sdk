package com.r3.corda.lib.tokens.selection.memory.services

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.withoutIssuer
import com.r3.corda.lib.tokens.selection.InsufficientBalanceException
import com.r3.corda.lib.tokens.selection.InsufficientNotLockedBalanceException
import com.r3.corda.lib.tokens.selection.memory.config.InMemorySelectionConfig
import com.r3.corda.lib.tokens.selection.memory.internal.Holder
import com.r3.corda.lib.tokens.selection.memory.internal.lookupExternalIdFromKey
import net.corda.v5.application.cordapp.CordappProvider
import net.corda.v5.application.flows.flowservices.dependencies.CordaInjectPreStart
import net.corda.v5.application.services.CordaService
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.KeyManagementService
import net.corda.v5.application.services.lifecycle.ServiceLifecycleEvent
import net.corda.v5.application.services.lifecycle.ServiceStart
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.internal.uncheckedCast
import net.corda.v5.base.util.contextLogger
import net.corda.v5.base.util.seconds
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.services.vault.DEFAULT_PAGE_NUM
import net.corda.v5.ledger.services.vault.VaultEventType
import net.corda.v5.ledger.services.vault.events.VaultStateEvent
import net.corda.v5.ledger.services.vault.events.VaultStateEventService
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

val UPDATER: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
val EMPTY_BUCKET = TokenBucket()

const val PLACE_HOLDER: String = "THIS_IS_A_PLACE_HOLDER"

class VaultWatcherService : CordaService {

    private lateinit var tokenObserver: TokenObserver
    private lateinit var providedConfig: InMemorySelectionConfig

    private val __backingMap: ConcurrentMap<StateAndRef<FungibleToken>, String> = ConcurrentHashMap()
    private val __indexed: ConcurrentMap<Class<out Holder>, ConcurrentMap<TokenIndex, TokenBucket>> = ConcurrentHashMap(
        providedConfig.indexingStrategies.associate { it.ownerType to ConcurrentHashMap() }
    )

    private val indexViewCreationLock: ReentrantReadWriteLock = ReentrantReadWriteLock()

    enum class IndexingType(val ownerType: Class<out Holder>) {

        EXTERNAL_ID(Holder.MappedIdentity::class.java),
        PUBLIC_KEY(Holder.KeyIdentity::class.java);

        companion object {
            fun fromHolder(holder: Class<out Holder>): IndexingType {
                return when (holder) {
                    Holder.MappedIdentity::class.java -> EXTERNAL_ID
                    Holder.KeyIdentity::class.java -> PUBLIC_KEY
                    else -> throw IllegalArgumentException("Unknown Holder type: $holder")
                }
            }
        }
    }

    @CordaInjectPreStart
    lateinit var cordappProvider: CordappProvider

    @CordaInjectPreStart
    lateinit var persistenceService: PersistenceService

    @CordaInjectPreStart
    lateinit var identityService: IdentityService

    @CordaInjectPreStart
    lateinit var keyManagementService: KeyManagementService

    @CordaInjectPreStart
    lateinit var vaultStateEventService: VaultStateEventService

    override fun onEvent(event: ServiceLifecycleEvent) {
        if (event is ServiceStart) {
            tokenObserver = getObservableFromAppConfig()
            providedConfig = InMemorySelectionConfig.parse(cordappProvider.appConfig, this)

            addTokensToCache(tokenObserver.initialValues)
            tokenObserver.startLoading(::onVaultUpdate)
        }
    }

    companion object {
        val LOG = contextLogger()
    }

    private fun getObservableFromAppConfig(): TokenObserver {
        val config = cordappProvider.appConfig
        val configOptions: InMemorySelectionConfig = InMemorySelectionConfig.parse(config, this)

        if (!configOptions.enabled) {
            LOG.info("Disabling inMemory token selection - refer to documentation on how to enable")
            return TokenObserver(emptyList(), { _, _ ->
                Holder.UnmappedIdentity()
            })
        }

        val ownerProvider: (StateAndRef<FungibleToken>, IndexingType) -> Holder = { token, indexingType ->
            when (indexingType) {
                IndexingType.PUBLIC_KEY -> Holder.KeyIdentity(token.state.data.holder.owningKey)
                IndexingType.EXTERNAL_ID -> {
                    val owningKey = token.state.data.holder.owningKey
                    lookupExternalIdFromKey(owningKey, identityService, keyManagementService)
                }
            }
        }

        val pageSize = 1000

        vaultStateEventService.subscribe("Vault Watcher Service") { _, vaultEvent ->
            if(vaultEvent.stateAndRef.state.data is FungibleToken) {
                onVaultUpdate(uncheckedCast(vaultEvent))
            }
        }

        // we use the UPDATER thread for two reasons
        // 1 this means we return the service before all states are loaded, and so do not hold up the node startup
        // 2 because all updates to the cache (addition / removal) are also done via UPDATER, this means that until we have finished loading all updates are buffered preventing out of order updates
        val asyncLoader = object : ((VaultStateEvent<FungibleToken>) -> Unit) -> Unit {
            override fun invoke(callback: (VaultStateEvent<FungibleToken>) -> Unit) {
                LOG.info("Starting async token loading from vault")
                UPDATER.submit {
                    try {
                        val cursor = persistenceService.query<StateAndRef<FungibleToken>>("FungibleTokenSchemaV1.PersistentFungibleToken.findAllUnconsumed", emptyMap())
                        do {
                            val newlyLoadedStates = cursor.poll(pageSize, 10.seconds)
                            LOG.info("publishing ${newlyLoadedStates.values.size} to async state loading callback")
                            newlyLoadedStates.values.forEach {
                                callback(
                                    object : VaultStateEvent<FungibleToken> {
                                        override val eventType = VaultEventType.PRODUCE
                                        override val stateAndRef = it
                                        override val timestamp = Instant.now()
                                    }
                                )
                            }
                        } while (!newlyLoadedStates.isLastResult)
                        LOG.info("finished token loading")
                    } catch (t: Throwable) {
                        LOG.error("Token Loading Failed due to: ", t)
                    }
                }
            }
        }
        return TokenObserver(emptyList(), ownerProvider, asyncLoader)
    }

    private fun processToken(token: StateAndRef<FungibleToken>, indexingType: IndexingType): TokenIndex {
        val owner = tokenObserver.ownerProvider(token, indexingType)
        val type = token.state.data.amount.token.tokenType.tokenClass
        val typeId = token.state.data.amount.token.tokenType.tokenIdentifier
        return TokenIndex(owner, type, typeId)
    }

    private fun onVaultUpdate(t: VaultStateEvent<FungibleToken>) {
        try {
            when(t.eventType) {
                VaultEventType.CONSUME -> {
                    LOG.info("received token vault update for consumed state")
                    removeTokenFromCache(t.stateAndRef)
                }
                VaultEventType.PRODUCE -> {
                    LOG.info("received token vault update for produced state")
                    addTokenToCache(t.stateAndRef)
                }
            }
        } catch (t: Throwable) {
            //we DO NOT want to kill the observable - as a single exception will terminate the feed
            LOG.error("Failure during token cache update", t)
        }
    }

    private fun removeTokenFromCache(stateAndRef: StateAndRef<FungibleToken>) {
        indexViewCreationLock.read {
            val existingMark = __backingMap.remove(stateAndRef)
            existingMark
                ?: LOG.warn("Attempted to remove existing token ${stateAndRef.ref}, but it was not found this suggests incorrect vault behaviours")
            for (key in __indexed.keys) {
                val index = processToken(stateAndRef, IndexingType.fromHolder(key))
                val indexedViewForHolder = __indexed[key]
                indexedViewForHolder
                    ?: LOG.warn("tried to obtain an indexed view for holder type: $key but was not found in set of indexed views")

                val bucketForIndex: TokenBucket? = indexedViewForHolder?.get(index)
                bucketForIndex?.remove(stateAndRef)
            }
        }
    }

    private fun addTokensToCache(stateAndRefs: Collection<StateAndRef<FungibleToken>>) = stateAndRefs.forEach(::addTokenToCache)

    private fun addTokenToCache(stateAndRef: StateAndRef<FungibleToken>) {
        indexViewCreationLock.read {
            val existingMark = __backingMap.putIfAbsent(stateAndRef, PLACE_HOLDER)
            existingMark?.let {
                LOG.warn("Attempted to overwrite existing token ${stateAndRef.ref}, this suggests incorrect vault behaviours")
            }
            for (key in __indexed.keys) {
                val index = processToken(stateAndRef, IndexingType.fromHolder(key))
                val indexedViewForHolder = __indexed[key]
                    ?: throw IllegalStateException("tried to obtain an indexed view for holder type: $key but was not found in set of indexed views")
                val bucketForIndex: TokenBucket = indexedViewForHolder.computeIfAbsent(index) {
                    TokenBucket()
                }
                bucketForIndex.add(stateAndRef)
            }
        }
    }

    private fun getOrCreateIndexViewForHolderType(holderType: Class<out Holder>): ConcurrentMap<TokenIndex, TokenBucket> {
        return __indexed[holderType] ?: indexViewCreationLock.write {
            __indexed[holderType] ?: generateNewIndexedView(holderType)
        }
    }

    private fun generateNewIndexedView(holderType: Class<out Holder>): ConcurrentMap<TokenIndex, TokenBucket> {
        val indexedViewForHolder: ConcurrentMap<TokenIndex, TokenBucket> = ConcurrentHashMap()
        for (stateAndRef in __backingMap.keys) {
            val index = processToken(stateAndRef, IndexingType.fromHolder(holderType))
            val bucketForIndex: TokenBucket = indexedViewForHolder.computeIfAbsent(index) {
                TokenBucket()
            }
            bucketForIndex.add(stateAndRef)
        }
        __indexed[holderType] = indexedViewForHolder
        return indexedViewForHolder
    }

    fun lockTokensExternal(list: List<StateAndRef<FungibleToken>>, knownSelectionId: String) {
        list.forEach {
            __backingMap.replace(it, PLACE_HOLDER, knownSelectionId)
        }
    }

    fun selectTokens(
        owner: Holder,
        requiredAmount: Amount<TokenType>,
        predicate: ((StateAndRef<FungibleToken>) -> Boolean) = { true },
        allowShortfall: Boolean = false,
        autoUnlockDelay: Duration = Duration.ofMinutes(5),
        selectionId: String
    ): List<StateAndRef<FungibleToken>> {
        //we have to handle both cases
        //1 when passed a raw TokenType - it's likely that the selecting entity does not care about the issuer and so we cannot constrain all selections to using IssuedTokenType
        //2 when passed an IssuedTokenType - it's likely that the selecting entity does care about the issuer, and so we must filter all tokens which do not match the issuer.
        val enrichedPredicate: AtomicReference<(StateAndRef<FungibleToken>) -> Boolean> =
            AtomicReference(if (requiredAmount.token is IssuedTokenType) {
                val issuer = (requiredAmount.token as IssuedTokenType).issuer
                { token ->
                    predicate(token) && token.state.data.issuer == issuer
                }
            } else {
                predicate
            })

        val lockedTokens = mutableListOf<StateAndRef<FungibleToken>>()
        val bucket: Iterable<StateAndRef<FungibleToken>> = if (owner is Holder.TokenOnly) {
            val currentPredicate = enrichedPredicate.get()
            //why do we do this? It doesn't really make sense to index on token type, as it's very likely that there will be very few types of tokens in a given vault
            //so instead of relying on an indexed view, we can create a predicate on the fly which will constrain the selection to the correct token type
            //we will revisit in future if this assumption turns out to be wrong
            enrichedPredicate.set {
                val stateTokenType = it.state.data.tokenType
                currentPredicate(it) &&
                        stateTokenType.fractionDigits == requiredAmount.token.fractionDigits &&
                        requiredAmount.token.tokenClass == stateTokenType.tokenClass &&
                        requiredAmount.token.tokenIdentifier == stateTokenType.tokenIdentifier
            }
            __backingMap.keys
        } else {
            val indexedView = getOrCreateIndexViewForHolderType(owner.javaClass)
            getTokenBucket(owner, requiredAmount.token.tokenClass, requiredAmount.token.tokenIdentifier, indexedView)
        }

        val requiredAmountWithoutIssuer = requiredAmount.withoutIssuer()
        var amountLocked: Amount<TokenType> = requiredAmountWithoutIssuer.copy(quantity = 0)
        // this is the running total of soft locked tokens that we encounter until the target token amount is reached
        var amountAlreadySoftLocked: Amount<TokenType> = requiredAmountWithoutIssuer.copy(quantity = 0)
        val finalPredicate = enrichedPredicate.get()
        for (tokenStateAndRef in bucket) {
            // Does the token satisfy the (optional) predicate eg. issuer?
            if (finalPredicate.invoke(tokenStateAndRef)) {
                val tokenAmount = uncheckedCast(tokenStateAndRef.state.data.amount.withoutIssuer())
                // if so, race to lock the token, expected oldValue = PLACE_HOLDER
                if (__backingMap.replace(tokenStateAndRef, PLACE_HOLDER, selectionId)) {
                    // we won the race to lock this token
                    lockedTokens.add(tokenStateAndRef)
                    amountLocked += tokenAmount
                    if (amountLocked >= requiredAmountWithoutIssuer) {
                        break
                    }
                } else {
                    amountAlreadySoftLocked += tokenAmount
                }
            }
        }

        if (!allowShortfall && amountLocked < requiredAmountWithoutIssuer) {
            lockedTokens.forEach {
                unlockToken(it, selectionId)
            }
            if (amountLocked + amountAlreadySoftLocked < requiredAmountWithoutIssuer) {
                throw InsufficientBalanceException("Insufficient spendable states identified for $requiredAmount.")
            } else {
                throw InsufficientNotLockedBalanceException("Insufficient not-locked spendable states identified for $requiredAmount.")
            }
        }

        UPDATER.schedule({
            lockedTokens.forEach {
                unlockToken(it, selectionId)
            }
        }, autoUnlockDelay.toMillis(), TimeUnit.MILLISECONDS)

        return uncheckedCast(lockedTokens)
    }

    fun unlockToken(it: StateAndRef<FungibleToken>, selectionId: String) {
        __backingMap.replace(it, selectionId, PLACE_HOLDER)
    }

    private fun getTokenBucket(
        idx: Holder,
        tokenClass: Class<*>,
        tokenIdentifier: String,
        mapToSelectFrom: ConcurrentMap<TokenIndex, TokenBucket>
    ): TokenBucket {
        return mapToSelectFrom[TokenIndex(idx, tokenClass, tokenIdentifier)] ?: EMPTY_BUCKET
    }
}

class TokenObserver(
    val initialValues: List<StateAndRef<FungibleToken>>,
    val ownerProvider: ((StateAndRef<FungibleToken>, VaultWatcherService.IndexingType) -> Holder),
    inline val asyncLoader: ((VaultStateEvent<FungibleToken>) -> Unit) -> Unit = { _ -> }
) {

    fun startLoading(loadingCallBack: (VaultStateEvent<FungibleToken>) -> Unit) {
        asyncLoader(loadingCallBack)
    }
}

class TokenBucket(set: MutableSet<StateAndRef<FungibleToken>> = ConcurrentHashMap<StateAndRef<FungibleToken>, Boolean>().keySet(true)) :
    MutableSet<StateAndRef<FungibleToken>> by set

data class TokenIndex(val owner: Holder, val tokenClazz: Class<*>, val tokenIdentifier: String)
