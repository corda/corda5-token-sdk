package com.r3.corda.lib.tokens.workflows.internal.flows.confidential

import com.r3.corda.lib.ci.workflows.ProvideKeyFlow
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.flows.unwrap
import net.corda.v5.base.annotations.Suspendable

class AnonymisePartiesFlowHandler(val otherSession: FlowSession) : Flow<Unit> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() {
        val action = otherSession.receive<ActionRequest>().unwrap { it }
        if (action == ActionRequest.CREATE_NEW_KEY) {
            flowEngine.subFlow(ProvideKeyFlow(otherSession))
        }
    }
}