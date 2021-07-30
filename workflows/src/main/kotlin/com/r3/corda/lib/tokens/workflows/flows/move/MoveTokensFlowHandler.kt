package com.r3.corda.lib.tokens.workflows.flows.move

import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler
import com.r3.corda.lib.tokens.workflows.utilities.isOurIdentity
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.MemberLookupService
import net.corda.v5.base.annotations.Suspendable

/**
 * Responder flow for [MoveTokensFlow], [MoveFungibleTokensFlow], [MoveNonFungibleTokensFlow]
 */
class MoveTokensFlowHandler(val otherSession: FlowSession) : Flow<Unit> {
    @CordaInject
    lateinit var memberLookupService: MemberLookupService

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() {
        // Resolve the move transaction.
        if (!memberLookupService.isOurIdentity(otherSession.counterparty)) {
            flowEngine.subFlow(ObserverAwareFinalityFlowHandler(otherSession))
        }
    }
}