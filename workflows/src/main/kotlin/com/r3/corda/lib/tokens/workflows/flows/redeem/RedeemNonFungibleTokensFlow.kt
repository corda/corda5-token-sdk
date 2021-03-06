package com.r3.corda.lib.tokens.workflows.flows.redeem

import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.transactions.TransactionBuilder

/**
 * Inlined flow used to redeem [NonFungibleToken] [heldToken] issued by the particular issuer.
 *
 * @param heldToken non fungible token to redeem
 * @param issuerSession session with the issuer token should be redeemed with
 * @param observerSessions optional sessions with the observer nodes, to which the transaction will be broadcasted
 */
class RedeemNonFungibleTokensFlow(
    val heldToken: TokenType,
    override val issuerSession: FlowSession,
    override val observerSessions: List<FlowSession>
) : AbstractRedeemTokensFlow() {
    @Suspendable
    override fun generateExit(transactionBuilder: TransactionBuilder) {
        addNonFungibleTokensToRedeem(
            transactionBuilder = transactionBuilder,
            persistenceService = persistenceService,
            heldToken = heldToken,
            issuer = issuerSession.counterparty
        )
    }
}