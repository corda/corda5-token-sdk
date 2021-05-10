package com.r3.corda.lib.tokens.workflows.flows.redeem

import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.dependencies.CordaInject
import net.corda.v5.base.annotations.Suspendable

/**
 * Responder flow to [ConfidentialRedeemFungibleTokensFlow].
 */
class ConfidentialRedeemFungibleTokensFlowHandler(val otherSession: FlowSession) : Flow<Unit> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() {
        flowEngine.subFlow(RedeemTokensFlowHandler(otherSession))
    }
}