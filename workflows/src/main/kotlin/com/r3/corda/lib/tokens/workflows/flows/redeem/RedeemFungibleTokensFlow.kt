package com.r3.corda.lib.tokens.workflows.flows.redeem

import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.flows.flowservices.dependencies.CordaInject
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.services.IdentityService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.transactions.TransactionBuilder

/**
 * Inlined flow used to redeem amount of [FungibleToken]s issued by the particular issuer with possible change output
 * paid to the [changeHolder].
 *
 * @param amount amount of token to redeem
 * @param changeHolder owner of possible change output, which defaults to the node identity of the calling node
 * @param issuerSession session with the issuer tokens should be redeemed with
 * @param observerSessions optional sessions with the observer nodes, to witch the transaction will be broadcasted
 */
class RedeemFungibleTokensFlow
@JvmOverloads
constructor(
    val amount: Amount<TokenType>,
    override val issuerSession: FlowSession,
    val changeHolder: AbstractParty? = null,
    override val observerSessions: List<FlowSession> = emptyList(),
) : AbstractRedeemTokensFlow() {

    @CordaInject
    lateinit var identityService: IdentityService

    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @Suspendable
    override fun generateExit(transactionBuilder: TransactionBuilder) {
        addFungibleTokensToRedeem(
            transactionBuilder = transactionBuilder,
            persistenceService = persistenceService,
            identityService = identityService,
            flowEngine = flowEngine,
            amount = amount,
            issuer = issuerSession.counterparty,
            changeHolder = changeHolder ?: flowIdentity.ourIdentity,
        )
    }
}