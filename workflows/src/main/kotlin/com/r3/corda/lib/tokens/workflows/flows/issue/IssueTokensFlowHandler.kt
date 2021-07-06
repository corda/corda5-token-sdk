package com.r3.corda.lib.tokens.workflows.flows.issue

import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.node.MemberInfo
import net.corda.v5.base.annotations.Suspendable

/**
 * The in-line flow handler for [IssueTokensFlow].
 */
class IssueTokensFlowHandler(val otherSession: FlowSession) : Flow<Unit> {
    @CordaInject
    lateinit var memberInfo: MemberInfo

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() {
        if (!memberInfo.hasParty(otherSession.counterparty)) {
            flowEngine.subFlow(ObserverAwareFinalityFlowHandler(otherSession))
        }
    }
}