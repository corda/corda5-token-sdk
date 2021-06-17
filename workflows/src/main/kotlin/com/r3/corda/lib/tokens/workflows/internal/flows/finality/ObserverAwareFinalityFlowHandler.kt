package com.r3.corda.lib.tokens.workflows.internal.flows.finality

import net.corda.systemflows.ReceiveFinalityFlow
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.node.NodeInfo
import net.corda.v5.application.flows.unwrap
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.services.StatesToRecord
import net.corda.v5.ledger.transactions.SignedTransaction

class ObserverAwareFinalityFlowHandler(val otherSession: FlowSession) : Flow<SignedTransaction?> {
    @CordaInject
    lateinit var nodeInfo: NodeInfo

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(): SignedTransaction? {
        val role = otherSession.receive<TransactionRole>().unwrap { it }
        val statesToRecord = when (role) {
            TransactionRole.PARTICIPANT -> StatesToRecord.ONLY_RELEVANT
            TransactionRole.OBSERVER -> StatesToRecord.ALL_VISIBLE
        }
        // If states are issued to self, then ReceiveFinalityFlow does not need to be invoked.
        return if (!nodeInfo.isLegalIdentity(otherSession.counterparty)) {
            flowEngine.subFlow(ReceiveFinalityFlow(otherSideSession = otherSession, statesToRecord = statesToRecord))
        } else null
    }
}

