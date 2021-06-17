package com.r3.corda.lib.tokens.workflows.flows.confidential

import com.r3.corda.lib.tokens.workflows.internal.flows.confidential.AnonymisePartiesFlowHandler
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.base.annotations.Suspendable

/**
 * Use of this flow should be paired with [ConfidentialTokensFlow]. If asked to do so, this flow begins the generation
 * of a new key pair by calling [RequestConfidentialIdentityFlowHandler].
 */
class ConfidentialTokensFlowHandler(val otherSession: FlowSession) : Flow<Unit> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() = flowEngine.subFlow(AnonymisePartiesFlowHandler(otherSession))
}