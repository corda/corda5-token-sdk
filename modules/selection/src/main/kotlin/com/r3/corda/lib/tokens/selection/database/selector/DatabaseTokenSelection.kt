package com.r3.corda.lib.tokens.selection.database.selector

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.datatypes.NamedQueryAndParameters
import com.r3.corda.lib.tokens.selection.*
import com.r3.corda.lib.tokens.selection.api.Selector
import com.r3.corda.lib.tokens.selection.database.config.MAX_RETRIES_DEFAULT
import com.r3.corda.lib.tokens.selection.database.config.PAGE_SIZE_DEFAULT
import com.r3.corda.lib.tokens.selection.database.config.RETRY_CAP_DEFAULT
import com.r3.corda.lib.tokens.selection.database.config.RETRY_SLEEP_DEFAULT
import com.r3.corda.lib.tokens.selection.memory.internal.Holder
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.contextLogger
import net.corda.v5.base.util.millis
import net.corda.v5.base.util.seconds
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.StateAndRef
import java.util.*

/**
 * TokenType selection using Hibernate. It uses roughly the same logic that the coin selection algorithm used in
 * AbstractCoinSelection within the finance module. The only difference is that now there are not specific database
 * implementations, instead hibernate is used for an agnostic approach.
 *
 * The default behaviour is to order all states by [StateRef] and query for a specific token type. The default behaviour is probably not
 * very efficient but the behaviour can be customised if necessary.
 *
 * This is only really here as a stopgap solution until in-memory token selection is implemented.
 *
 * @param services for performing vault queries.
 */
class DatabaseTokenSelection @JvmOverloads constructor(
    private val persistenceService: PersistenceService,
    private val identityService: IdentityService,
    private val flowEngine: FlowEngine,
    private val maxRetries: Int = MAX_RETRIES_DEFAULT,
    private val retrySleep: Int = RETRY_SLEEP_DEFAULT,
    private val retryCap: Int = RETRY_CAP_DEFAULT,
    private val pageSize: Int = PAGE_SIZE_DEFAULT
) : Selector() {

    companion object {
        val logger = contextLogger()
    }

    /**
     * Queries for held token amounts with the specified token to the specified requiredAmount.
     *
     * @return the amount of claimed tokens (effectively the sum of values of the states in [stateAndRefs]
     * */
    private fun executeQuery(
        requiredAmount: Amount<TokenType>,
        namedQuery: String,
        queryParams: Map<String, Any>,
        stateAndRefs: MutableList<StateAndRef<FungibleToken>>,
    ): Amount<TokenType> {
        // Didn't need to select any tokens.
        if (requiredAmount.quantity == 0L) {
            return Amount(0, requiredAmount.token)
        }

        var claimedAmount = 0L

        val cursor = persistenceService.query<StateAndRef<FungibleToken>>(namedQuery, queryParams)
        do {
            val tokens = cursor.poll(pageSize, 10.seconds)
            for (state in tokens.values) {
                stateAndRefs += state
                claimedAmount += state.state.data.amount.quantity
                if (claimedAmount >= requiredAmount.quantity) {
                    break
                }
            }
        } while (claimedAmount < requiredAmount.quantity && !tokens.isLastResult)

        val claimedAmountWithToken = Amount(claimedAmount, requiredAmount.token)
        // No tokens available.
        if (stateAndRefs.isEmpty()) return Amount(0, requiredAmount.token)

        return claimedAmountWithToken
    }

    /**
     * Queries for held token amounts with the specified token to the specified requiredAmount
     * AND tries to soft lock the selected tokens.
     */
    private fun executeQueryAndReserve(
        requiredAmount: Amount<TokenType>,
        namedQueryString: String,
        queryParams: Map<String, Any>,
        stateAndRefs: MutableList<StateAndRef<FungibleToken>>,
    ): Boolean {
        // not including soft locked tokens
        val claimedAmount = executeQuery(requiredAmount, namedQueryString, queryParams, stateAndRefs)
        return if (claimedAmount >= requiredAmount) {
            // We picked enough tokensToIssue, continue
            logger.trace("TokenType selection for $requiredAmount retrieved ${stateAndRefs.count()} states totalling $claimedAmount: $stateAndRefs")
            true
        } else {
            logger.trace("TokenType selection requested $requiredAmount but retrieved $claimedAmount with state refs: ${stateAndRefs.map { it.ref }}")
            false
        }
    }

    @Suspendable
    override fun selectTokens(
        holder: Holder,
        lockId: UUID,
        requiredAmount: Amount<TokenType>,
        queryBy: TokenQueryBy
    ): List<StateAndRef<FungibleToken>> {
        val (namedQuery, queryParams) = getNamedQuery(requiredAmount, holder, queryBy)
        val stateAndRefs = mutableListOf<StateAndRef<FungibleToken>>()
        for (retryCount in 1..maxRetries) {
            if (!executeQueryAndReserve(requiredAmount, namedQuery, queryParams, stateAndRefs)) {
                // TODO: Need to specify exactly why it fails. Locked states or literally _no_ states!
                // No point in retrying if there will never be enough...
                logger.warn("TokenType selection failed on attempt $retryCount.")
                // TODO: revisit the back off strategy for contended spending.
                if (retryCount != maxRetries) {
                    stateAndRefs.clear()
                    val durationMillis = (minOf(retrySleep.shl(retryCount), retryCap / 2) * (1.0 + Math.random())).toInt()
                    flowEngine.sleep(durationMillis.millis)
                } else {
                    // if there is enough tokens available to satisfy the amount then we need to throw
                    // [InsufficientNotLockedBalanceException] instead
                    val amount =
                        executeQuery(requiredAmount, namedQuery, queryParams, mutableListOf())
                    if (amount < requiredAmount) {
                        logger.warn("Insufficient spendable states identified for $requiredAmount.")
                        throw InsufficientBalanceException("Insufficient spendable states identified for $requiredAmount.")
                    } else {
                        logger.warn("Insufficient not locked spendable states identified for $requiredAmount.")
                        throw InsufficientNotLockedBalanceException("Insufficient not locked spendable states identified for $requiredAmount.")
                    }
                }
            } else {
                break
            }
        }
        return if (queryBy.predicate != { true }) {
            stateAndRefs.toList().filter { stateAndRef ->
                queryBy.predicate.invoke(stateAndRef)
            }
        } else stateAndRefs
    }

    private fun getNamedQuery(requiredAmount: Amount<TokenType>, holder: Holder, queryBy: TokenQueryBy): NamedQueryAndParameters {
        // This is due to the fact, that user can pass Amount<IssuedTokenType>, this usually shouldn't happen, but just in case
        val amountToken = requiredAmount.token
        val (token, issuer) = when (amountToken) {
            is IssuedTokenType -> Pair(amountToken.tokenType, amountToken.issuer)
            else -> Pair(amountToken, queryBy.issuer)
        }

        return holderToNamedQuery(holder, token, issuer)
    }

    private fun holderToNamedQuery(
        holder: Holder,
        token: TokenType,
        issuer: Party?,
    ): NamedQueryAndParameters {
        return when (holder) {
            is Holder.KeyIdentity -> {
                // We want the AbstractParty that this key refers to, unfortunately, partyFromKey returns always well known party
                // for that key, so afterwards we need to construct AnonymousParty.
                val knownParty: AbstractParty = identityService.nameFromKey(holder.owningKey)?.let {
                    identityService.partyFromName(it)
                } ?: identityService.anonymousPartyFromKey(holder.owningKey)
                val holderParty = if (knownParty.owningKey == holder.owningKey) knownParty else identityService.anonymousPartyFromKey(holder.owningKey)
                if(issuer == null) {
                    namedQueryForTokenClassIdentifierAndHolder(token, holderParty)
                } else {
                    namedQueryForTokenClassIdentifierHolderAndIssuer(token, holderParty, issuer)
                }

            }
            is Holder.MappedIdentity -> if(issuer == null) {
                namedQueryForTokenClassIdentifierAndExternalId(token, holder.uuid)
            } else {
                namedQueryForTokenClassIdentifierIssuerAndExternalId(token, issuer, holder.uuid)
            }
            // TODO After looking at VaultQueryCriteria implemenation of querying by external id we don't really support querying for keys not mapped to external id!
            is Holder.UnmappedIdentity -> if(issuer == null) {
                namedQueryForTokenClassAndIdentifier(token)
            } else {
                namedQueryForTokenClassIdentifierAndIssuer(token, issuer)
            }
            is Holder.TokenOnly ->  if(issuer == null) {
                namedQueryForTokenClassAndIdentifier(token)
            } else {
                namedQueryForTokenClassIdentifierAndIssuer(token, issuer)
            }
        }
    }
}
