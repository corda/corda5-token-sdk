package com.r3.corda.lib.tokens.workflows.flows.redeem

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.AnonymousPartyImpl
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.dependencies.CordaInject
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.services.KeyManagementService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.transactions.SignedTransaction

/**
 * Version of [RedeemFungibleTokensFlow] using confidential identity for a change owner.
 * There is no [NonFungibleToken] version of this flow, because there is no output paid.
 * Identities are synchronised during normal redeem call.
 *
 * @param amount amount of token to redeem
 * @param issuerSession session with the issuer tokens should be redeemed with
 * @param observerSessions optional sessions with the observer nodes, to witch the transaction will be broadcasted
 * @param changeHolder optional change key, if using accounts you should generate the change key prior to calling this
 *                     flow then pass it in to the flow via this parameter
 */
class ConfidentialRedeemFungibleTokensFlow
@JvmOverloads
constructor(
    val amount: Amount<TokenType>,
    val issuerSession: FlowSession,
    val observerSessions: List<FlowSession> = emptyList(),
    val changeHolder: AbstractParty? = null
) : Flow<SignedTransaction> {
    @CordaInject
    lateinit var keyManagementService: KeyManagementService

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(): SignedTransaction {
        // If a change holder key is not specified then one will be created for you. NB. If you want to use accounts
        // with tokens, then you must generate and allocate the key to an account up-front and pass the key in as the
        // "changeHolder".
        val confidentialHolder = changeHolder ?: let {
            val key = keyManagementService.freshKey()
            AnonymousPartyImpl(key)
        }
        return flowEngine.subFlow(
            RedeemFungibleTokensFlow(
                amount = amount,
                issuerSession = issuerSession,
                changeHolder = confidentialHolder,  // This will never be null.
                observerSessions = observerSessions,
            )
        )
    }
}