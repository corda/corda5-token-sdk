package com.r3.corda.lib.tokens.workflows.flows.issue

import com.r3.corda.lib.tokens.workflows.flows.confidential.ConfidentialTokensFlowHandler
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.TransactionRole
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.dependencies.CordaInject
import net.corda.v5.application.utilities.unwrap
import net.corda.v5.base.annotations.Suspendable

/**
 * The in-line flow handler for [ConfidentialIssueTokensFlow].
 */
class ConfidentialIssueTokensFlowHandler(val otherSession: FlowSession) : Flow<Unit> {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() {
        // TODO This is nasty as soon as our flows become more involved we will end up with crazy responders
        val role = otherSession.receive<TransactionRole>().unwrap { it }
        if (role == TransactionRole.PARTICIPANT) {
            flowEngine.subFlow(ConfidentialTokensFlowHandler(otherSession))
        }
        flowEngine.subFlow(IssueTokensFlowHandler(otherSession))
    }
}