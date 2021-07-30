package com.r3.corda.lib.tokens.selection.memory.selector

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import com.r3.corda.lib.tokens.selection.api.Selector
import com.r3.corda.lib.tokens.selection.issuerAndPredicate
import com.r3.corda.lib.tokens.selection.memory.internal.Holder
import com.r3.corda.lib.tokens.selection.memory.services.VaultWatcherService
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.serialization.SerializationToken
import net.corda.v5.serialization.SerializeAsToken
import net.corda.v5.serialization.SerializeAsTokenContext
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicReference

/**
 * Selector that should be used from your flow when you want to do in memory token selection opposed to [DatabaseTokenSelection].
 * This is experimental feature for now. It was designed to remove potential performance bottleneck and remove the requirement
 * for database specific SQL to be provided for each backend.
 * To use it, you need to have `VaultWatcherService` installed as a `CordaService` on node startup. Indexing
 * strategy could be specified via cordapp configuration, see [ConfigSelection]. You can index either by PublicKey or by ExternalId if using accounts feature.
 *
 * @property vaultWatcherService corda service that watches and caches new states
 * @property autoUnlockDelay Time after which the tokens that are not spent will be automatically released. Defaults to Duration.ofMinutes(5).
 */
class LocalTokenSelector (
    private val vaultWatcherService: VaultWatcherService,
    private val autoUnlockDelay: Duration,
    state: Pair<List<StateAndRef<FungibleToken>>, String>? // Used for deserializing
) : SerializeAsToken, Selector() {

    constructor(
        vaultWatcherService: VaultWatcherService,
    ) : this(vaultWatcherService, Duration.ofMinutes(5), null)

    constructor(
        vaultWatcherService: VaultWatcherService,
        autoUnlockDelay: Duration,
    ) : this(vaultWatcherService, autoUnlockDelay, null)

    constructor(
        vaultWatcherService: VaultWatcherService,
        state: Pair<List<StateAndRef<FungibleToken>>, String>?,
    ) : this(vaultWatcherService, Duration.ofMinutes(5), state)

    private val mostRecentlyLocked = AtomicReference<Pair<List<StateAndRef<FungibleToken>>, String>>(state)

    override fun selectTokens(
        holder: Holder,
        lockId: UUID,
        requiredAmount: Amount<TokenType>,
        queryBy: TokenQueryBy
    ): List<StateAndRef<FungibleToken>> {
        synchronized(mostRecentlyLocked) {
            if (mostRecentlyLocked.get() == null) {
                val additionalPredicate = queryBy.issuerAndPredicate()
                return vaultWatcherService.selectTokens(
                    holder, requiredAmount, additionalPredicate, false, autoUnlockDelay, lockId.toString()
                ).also { mostRecentlyLocked.set(it to lockId.toString()) }
            } else {
                throw IllegalStateException("Each instance can only used to select tokens once")
            }
        }
    }

    // For manual rollback
    fun rollback() {
        val lockedStates = mostRecentlyLocked.get()
        lockedStates?.first?.forEach {
            vaultWatcherService.unlockToken(it, lockedStates.second)
        }
        mostRecentlyLocked.set(null)
    }

    override fun toToken(context: SerializeAsTokenContext): SerializationToken {
        val lockedStateAndRefs = mostRecentlyLocked.get() ?: listOf<StateAndRef<FungibleToken>>() to ""
        return SerialToken(vaultWatcherService, lockedStateAndRefs.first, lockedStateAndRefs.second, autoUnlockDelay)
    }

    private class SerialToken(
        val vaultWatcherService: VaultWatcherService,
        val lockedStateAndRefs: List<StateAndRef<FungibleToken>>,
        val selectionId: String,
        val autoUnlockDelay: Duration
    ) : SerializationToken {
        override fun fromToken(context: SerializeAsTokenContext): LocalTokenSelector {
            vaultWatcherService.lockTokensExternal(lockedStateAndRefs, knownSelectionId = selectionId)
            return LocalTokenSelector(
                vaultWatcherService,
                state = lockedStateAndRefs to selectionId,
                autoUnlockDelay = autoUnlockDelay
            )
        }
    }
}
