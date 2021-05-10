package com.r3.corda.lib.tokens.workflows.flows.move

import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.dependencies.CordaInject
import net.corda.v5.application.node.NodeInfo
import net.corda.v5.base.annotations.Suspendable

/**
 * Responder flow for [MoveTokensFlow], [MoveFungibleTokensFlow], [MoveNonFungibleTokensFlow]
 */
class MoveTokensFlowHandler(val otherSession: FlowSession) : Flow<Unit> {
    @CordaInject
    lateinit var nodeInfo: NodeInfo

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() {
        // Resolve the move transaction.
        if (!nodeInfo.isLegalIdentity(otherSession.counterparty)) {
            flowEngine.subFlow(ObserverAwareFinalityFlowHandler(otherSession))
        }
    }
}