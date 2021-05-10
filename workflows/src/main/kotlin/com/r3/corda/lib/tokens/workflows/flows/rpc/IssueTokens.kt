package com.r3.corda.lib.tokens.workflows.flows.rpc

import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.workflows.flows.issue.ConfidentialIssueTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.issue.ConfidentialIssueTokensFlowHandler
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlowHandler
import com.r3.corda.lib.tokens.workflows.utilities.sessionsForParticipants
import com.r3.corda.lib.tokens.workflows.utilities.sessionsForParties
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.flows.StartableByService
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.flows.flowservices.dependencies.CordaInject
import net.corda.v5.application.identity.Party
import net.corda.v5.application.node.services.IdentityService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.transactions.SignedTransaction

/**
 * A flow for issuing fungible or non-fungible tokens which initiates its own participantSessions. This is the case when
 * called from the node rpc or in a unit test. However, in the case where you already have a session with another [Party]
 * and you wish to issue tokens as part of a wider workflow, then use [IssueTokensFlow].
 *
 * @property tokensToIssue a list of [AbstractToken]s to issue
 * @property observers a set of observing [Party]s
 */
@StartableByService
@StartableByRPC
@InitiatingFlow
class IssueTokens
@JvmOverloads
constructor(
        val tokensToIssue: List<AbstractToken>,
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
        val participantSessions = sessionsForParticipants(identityService, flowMessaging, tokensToIssue)
        return flowEngine.subFlow(IssueTokensFlow(tokensToIssue, participantSessions, observerSessions))
    }
}

/**
 * Responder flow for [IssueTokens].
 */
@InitiatedBy(IssueTokens::class)
class IssueTokensHandler(val otherSession: FlowSession) : Flow<Unit> {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() = flowEngine.subFlow(IssueTokensFlowHandler(otherSession))
}

/**
 * A flow for issuing fungible or non-fungible tokens which initiates its own participantSessions. This is the case when called
 * from the node rpc or in a unit test. However, in the case where you already have a session with another [Party] and
 * you wish to issue tokens as part of a wider workflow, then use [IssueTokensFlow].
 *
 * @property tokensToIssue a list of [AbstractToken]s to issue
 * @property observers aset of observing [Party]s
 */
@StartableByService
@StartableByRPC
@InitiatingFlow
class ConfidentialIssueTokens
@JvmOverloads
constructor(
        val tokensToIssue: List<AbstractToken>,
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
        val participantSessions = sessionsForParticipants(identityService, flowMessaging, tokensToIssue)
        return flowEngine.subFlow(ConfidentialIssueTokensFlow(tokensToIssue, participantSessions, observerSessions))
    }
}

/**
 * Responder flow for [ConfidentialIssueTokens].
 */
@InitiatedBy(ConfidentialIssueTokens::class)
class ConfidentialIssueTokensHandler(val otherSession: FlowSession) : Flow<Unit> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() = flowEngine.subFlow(ConfidentialIssueTokensFlowHandler(otherSession))
}