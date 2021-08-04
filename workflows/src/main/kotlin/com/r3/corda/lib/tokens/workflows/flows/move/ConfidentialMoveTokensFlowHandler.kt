package com.r3.corda.lib.tokens.workflows.flows.move

import com.r3.corda.lib.tokens.workflows.flows.confidential.ConfidentialTokensFlowHandler
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.TransactionRole
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.receive
import net.corda.v5.application.flows.unwrap
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.base.annotations.Suspendable

/**
 * Responder flow to confidential move tokens flows: [ConfidentialMoveNonFungibleTokensFlow] and
 * [ConfidentialMoveFungibleTokensFlow].
 */
class ConfidentialMoveTokensFlowHandler(val otherSession: FlowSession) : Flow<Unit> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() {
        val role = otherSession.receive<TransactionRole>().unwrap { it }
        if (role == TransactionRole.PARTICIPANT) {
            flowEngine.subFlow(ConfidentialTokensFlowHandler(otherSession))
        }
        flowEngine.subFlow(ObserverAwareFinalityFlowHandler(otherSession))
    }
}
