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
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.IdentityService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.transactions.SignedTransaction

@StartableByService
@StartableByRPC
@InitiatingFlow
class RedeemFungibleTokens (
    val amount: Amount<TokenType>,
    val issuer: Party,
    val observers: List<Party>,
    val customPostProcessorName: String?,
    val changeHolder: AbstractParty?
) : Flow<SignedTransaction> {

    constructor(
        amount: Amount<TokenType>,
        issuer: Party,
    ) : this(amount, issuer, emptyList(), null, null)

    constructor(
        amount: Amount<TokenType>,
        issuer: Party,
        observers: List<Party>,
    ) : this(amount, issuer, observers, null, null)

    constructor(
        amount: Amount<TokenType>,
        issuer: Party,
        customPostProcessorName: String?,
    ) : this(amount, issuer, emptyList(), customPostProcessorName, null)

    constructor(
        amount: Amount<TokenType>,
        issuer: Party,
        changeHolder: AbstractParty?
    ) : this(amount, issuer, emptyList(), null, changeHolder)

    constructor(
        amount: Amount<TokenType>,
        issuer: Party,
        observers: List<Party>,
        customPostProcessorName: String?,
    ) : this(amount, issuer, observers, customPostProcessorName, null)

    constructor(
        amount: Amount<TokenType>,
        issuer: Party,
        observers: List<Party>,
        changeHolder: AbstractParty?
    ) : this(amount, issuer, observers, null, changeHolder)

    constructor(
        amount: Amount<TokenType>,
        issuer: Party,
        customPostProcessorName: String?,
        changeHolder: AbstractParty?
    ) : this(amount, issuer, emptyList(), customPostProcessorName, changeHolder)

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
                amount,
                issuerSession,
                changeHolder ?: flowIdentity.ourIdentity,
                observerSessions,
                customPostProcessorName
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
class RedeemNonFungibleTokens (
    val heldToken: TokenType,
    val issuer: Party,
    val observers: List<Party>
) : Flow<SignedTransaction> {

    constructor(
        heldToken: TokenType,
        issuer: Party,
    ) : this(heldToken, issuer, emptyList())

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
class ConfidentialRedeemFungibleTokens (
    val amount: Amount<TokenType>,
    val issuer: Party,
    val observers: List<Party>,
    val customPostProcessorName: String?,
    val changeHolder: AbstractParty?
) : Flow<SignedTransaction> {

    constructor(
        amount: Amount<TokenType>,
        issuer: Party,
    ) : this(amount, issuer, emptyList(), null, null)

    constructor(
        amount: Amount<TokenType>,
        issuer: Party,
        observers: List<Party>,
    ) : this(amount, issuer, observers, null, null)

    constructor(
        amount: Amount<TokenType>,
        issuer: Party,
        customPostProcessorName: String?,
    ) : this(amount, issuer, emptyList(), customPostProcessorName, null)

    constructor(
        amount: Amount<TokenType>,
        issuer: Party,
        changeHolder: AbstractParty?
    ) : this(amount, issuer, emptyList(), null, changeHolder)

    constructor(
        amount: Amount<TokenType>,
        issuer: Party,
        observers: List<Party>,
        customPostProcessorName: String?,
    ) : this(amount, issuer, observers, customPostProcessorName, null)

    constructor(
        amount: Amount<TokenType>,
        issuer: Party,
        observers: List<Party>,
        changeHolder: AbstractParty?
    ) : this(amount, issuer, observers, null, changeHolder)

    constructor(
        amount: Amount<TokenType>,
        issuer: Party,
        customPostProcessorName: String?,
        changeHolder: AbstractParty?
    ) : this(amount, issuer, emptyList(), customPostProcessorName, changeHolder)

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
                changeHolder = changeHolder,
                customPostProcessorName = customPostProcessorName
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