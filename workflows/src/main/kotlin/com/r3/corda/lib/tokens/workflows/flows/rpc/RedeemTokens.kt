package com.r3.corda.lib.tokens.workflows.flows.rpc

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.redeem.ConfidentialRedeemFungibleTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.redeem.ConfidentialRedeemFungibleTokensFlowHandler
import com.r3.corda.lib.tokens.workflows.flows.redeem.RedeemFungibleTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.redeem.RedeemNonFungibleTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.redeem.RedeemTokensFlowHandler
import com.r3.corda.lib.tokens.workflows.utilities.sessionsForParties
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.flows.StartableByService
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.flows.flowservices.dependencies.CordaInject
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.IdentityService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.services.vault.QueryCriteria
import net.corda.v5.ledger.transactions.SignedTransaction

@StartableByService
@StartableByRPC
@InitiatingFlow
class RedeemFungibleTokens
@JvmOverloads
constructor(
    val amount: Amount<TokenType>,
    val issuer: Party,
    val observers: List<Party> = emptyList(),
    val queryCriteria: QueryCriteria? = null,
    val changeHolder: AbstractParty? = null
) : Flow<SignedTransaction> {

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @CordaInject
    lateinit var identityService: IdentityService

    @Suspendable
    override fun call(): SignedTransaction {
        val observerSessions = sessionsForParties(identityService, flowMessaging, observers)
        val issuerSession = flowMessaging.initiateFlow(issuer)
        return flowEngine.subFlow(
            RedeemFungibleTokensFlow(
                amount, issuerSession, changeHolder
                    ?: flowIdentity.ourIdentity, observerSessions, queryCriteria
            )
        )
    }
}

@InitiatedBy(RedeemFungibleTokens::class)
open class RedeemFungibleTokensHandler(val otherSession: FlowSession) : Flow<Unit> {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() {
        flowEngine.subFlow(RedeemTokensFlowHandler(otherSession))
    }
}

@StartableByService
@StartableByRPC
@InitiatingFlow
class RedeemNonFungibleTokens
@JvmOverloads
constructor(
    val heldToken: TokenType,
    val issuer: Party,
    val observers: List<Party> = emptyList()
) : Flow<SignedTransaction> {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var identityService: IdentityService

    @Suspendable
    override fun call(): SignedTransaction {
        val observerSessions = sessionsForParties(identityService, flowMessaging, observers)
        val issuerSession = flowMessaging.initiateFlow(issuer)
        return flowEngine.subFlow(RedeemNonFungibleTokensFlow(heldToken, issuerSession, observerSessions))
    }
}

@InitiatedBy(RedeemNonFungibleTokens::class)
open class RedeemNonFungibleTokensHandler(val otherSession: FlowSession) : Flow<Unit> {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() {
        flowEngine.subFlow(RedeemTokensFlowHandler(otherSession))
    }
}

/* Confidential flows. */
// We don't need confidential non fungible redeem, because there are no outputs.
@StartableByService
@StartableByRPC
@InitiatingFlow
class ConfidentialRedeemFungibleTokens
@JvmOverloads
constructor(
    val amount: Amount<TokenType>,
    val issuer: Party,
    val observers: List<Party> = emptyList(),
    val queryCriteria: QueryCriteria? = null,
    val changeHolder: AbstractParty? = null
) : Flow<SignedTransaction> {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var identityService: IdentityService

    @Suspendable
    override fun call(): SignedTransaction {
        val observerSessions = sessionsForParties(identityService, flowMessaging, observers)
        val issuerSession = flowMessaging.initiateFlow(issuer)
        return flowEngine.subFlow(
            ConfidentialRedeemFungibleTokensFlow(
                amount = amount,
                issuerSession = issuerSession,
                observerSessions = observerSessions,
                additionalQueryCriteria = queryCriteria,
                changeHolder = changeHolder
            )
        )
    }
}

@InitiatedBy(ConfidentialRedeemFungibleTokens::class)
open class ConfidentialRedeemFungibleTokensHandler(val otherSession: FlowSession) : Flow<Unit> {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() {
        flowEngine.subFlow(ConfidentialRedeemFungibleTokensFlowHandler(otherSession))
    }
}