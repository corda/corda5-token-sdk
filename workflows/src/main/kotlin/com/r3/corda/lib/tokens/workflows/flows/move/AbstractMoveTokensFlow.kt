package com.r3.corda.lib.tokens.workflows.flows.move

import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.transactions.SignedTransaction
import net.corda.v5.ledger.transactions.TransactionBuilder
import net.corda.v5.ledger.transactions.TransactionBuilderFactory

/**
 * An abstract class for the move tokens flows family.
 *
 * You must provide [participantSessions] and optional [observerSessions] for finalization. Override [addMove] to select
 * tokens to move. See helper functions in [MoveTokensUtilities] module.
 *
 * The flow performs basic tasks, generates move transaction proposal for all the participants, collects signatures and
 * finalises transaction with observers if present.
 *
 * @property participantSessions a list of flow participantSessions for the transaction participants.
 * @property observerSessions a list of flow participantSessions for the transaction observers.
 */
abstract class AbstractMoveTokensFlow : Flow<SignedTransaction> {
    abstract val participantSessions: List<FlowSession>
    abstract val observerSessions: List<FlowSession>

    @CordaInject
    lateinit var transactionBuilderFactory: TransactionBuilderFactory

    @CordaInject
    lateinit var flowEngine: FlowEngine

    private companion object {
        const val GENERATE = "Generating tokens to move."
        const val RECORDING = "Recording signed transaction."
        const val UPDATING = "Updating data distribution list."

        val logger = contextLogger()

        fun debug(msg: String) = logger.debug("${this::class.java.name}: $msg")
    }

    /**
     * Adds a move of tokens to the [transactionBuilder]. This function mutates the builder.
     */
    @Suspendable
    abstract fun addMove(transactionBuilder: TransactionBuilder)

    @Suspendable
    override fun call(): SignedTransaction {
        // Initialise the transaction builder with no notary.
        val transactionBuilder = transactionBuilderFactory.create()
        debug(GENERATE)
        // Add all the specified inputs and outputs to the transaction.
        // The correct commands and signing keys are also added.
        addMove(transactionBuilder)
        debug(RECORDING)
        // Create new participantSessions if this is started as a top level flow.
        val signedTransaction = flowEngine.subFlow(
            ObserverAwareFinalityFlow(
                transactionBuilder = transactionBuilder,
                allSessions = participantSessions + observerSessions
            )
        )
        debug(UPDATING)
        // Update the distribution list.
        flowEngine.subFlow(UpdateDistributionListFlow(signedTransaction))
        // Return the newly created transaction.
        return signedTransaction
    }
}
