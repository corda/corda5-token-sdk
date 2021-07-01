package com.r3.corda.lib.tokens.workflows.flows.evolvable

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler
import net.corda.systemflows.SignTransactionFlow
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.receive
import net.corda.v5.application.flows.unwrap
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.requireThat
import net.corda.v5.ledger.transactions.SignedTransaction

/** In-line counter-flow for [UpdateEvolvableTokenFlow]. */
class UpdateEvolvableTokenFlowHandler(val otherSession: FlowSession) : Flow<Unit> {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() {
        // Receive the notification
        val notification = otherSession.receive<UpdateEvolvableTokenFlow.Notification>().unwrap { it }

        // Sign the transaction proposal, if required
        if (notification.signatureRequired) {
            val signTransactionFlow = object : SignTransactionFlow(otherSession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val ledgerTransaction = transactionMappingService.toLedgerTransaction(stx, checkSufficientSignatures = false)
                    val evolvableTokenTypeStateRef =
                        ledgerTransaction.inRefsOfType(EvolvableTokenType::class.java).single()
                    val oldMaintainers = evolvableTokenTypeStateRef.state.data.maintainers
                    require(otherSession.counterparty in oldMaintainers) {
                        "This flow can only be started by existing maintainers of the EvolvableTokenType. However it " +
                                "was started by ${otherSession.counterparty} who is not a maintainer."
                    }
                }
            }
            flowEngine.subFlow(signTransactionFlow)
        }

        // Resolve the update transaction.
        flowEngine.subFlow(ObserverAwareFinalityFlowHandler(otherSession))
    }
}