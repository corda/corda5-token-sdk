package com.r3.corda.lib.tokens.workflows.flows.redeem

import com.r3.corda.lib.ci.workflows.SyncKeyMappingFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.TransactionRole
import com.r3.corda.lib.tokens.workflows.utilities.ourSigningKeys
import net.corda.systemflows.CollectSignaturesFlow
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.CustomProgressTracker
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.dependencies.CordaInject
import net.corda.v5.application.node.services.KeyManagementService
import net.corda.v5.application.utilities.ProgressTracker
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.services.TransactionMappingService
import net.corda.v5.ledger.services.TransactionService
import net.corda.v5.ledger.services.VaultService
import net.corda.v5.ledger.transactions.SignedTransaction
import net.corda.v5.ledger.transactions.TransactionBuilder
import net.corda.v5.ledger.transactions.TransactionBuilderFactory

/**
 * Abstract class for the redeem token flows family.
 * You must provide [issuerSession] and optional [observerSessions] for finalization. Override [generateExit] to select
 * tokens for redeeming.
 * The flow performs basic tasks, generates redeem transaction proposal for the issuer, synchronises any confidential
 * identities from the states to redeem with the issuer (bear in mind that issuer usually isn't involved in move of tokens),
 * collects signatures and finalises transaction with observers if present.
 */
abstract class AbstractRedeemTokensFlow : Flow<SignedTransaction>, CustomProgressTracker {

    abstract val issuerSession: FlowSession
    abstract val observerSessions: List<FlowSession>

    companion object {
        object SELECTING_STATES : ProgressTracker.Step("Selecting states to redeem.")
        object SYNC_IDS : ProgressTracker.Step("Synchronising confidential identities.")
        object COLLECT_SIGS : ProgressTracker.Step("Collecting signatures")
        object FINALISING_TX : ProgressTracker.Step("Finalising transaction")

        fun tracker() = ProgressTracker(SELECTING_STATES, SYNC_IDS, COLLECT_SIGS, FINALISING_TX)
    }

    override val progressTracker: ProgressTracker = tracker()

    @CordaInject
    lateinit var transactionBuilderFactory: TransactionBuilderFactory

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var transactionMappingService: TransactionMappingService

    @CordaInject
    lateinit var keyManagementService: KeyManagementService

    @CordaInject
    lateinit var transactionService: TransactionService

    @CordaInject
    lateinit var vaultService: VaultService

    /**
     * Add redeem of tokens to the [transactionBuilder]. Modifies builder.
     */
    @Suspendable
    abstract fun generateExit(transactionBuilder: TransactionBuilder)

    @Suspendable
    override fun call(): SignedTransaction {
        issuerSession.send(TransactionRole.PARTICIPANT)
        observerSessions.forEach { it.send(TransactionRole.OBSERVER) }
        val txBuilder = transactionBuilderFactory.create()
        progressTracker.currentStep = SELECTING_STATES
        generateExit(txBuilder)
        // First synchronise identities between issuer and our states.
        // TODO: Only do this if necessary.
        progressTracker.currentStep = SYNC_IDS
        flowEngine.subFlow(SyncKeyMappingFlow(issuerSession, txBuilder.toWireTransaction()))
        val ourSigningKeys = transactionMappingService.toLedgerTransaction(txBuilder.toWireTransaction())
            .ourSigningKeys(keyManagementService)
        val partialStx = transactionService.signInitial(txBuilder, ourSigningKeys)
        // Call collect signatures flow, issuer should perform all the checks for redeeming states.
        progressTracker.currentStep = COLLECT_SIGS
        val stx = flowEngine.subFlow(CollectSignaturesFlow(partialStx, listOf(issuerSession), ourSigningKeys))
        progressTracker.currentStep = FINALISING_TX
        return flowEngine.subFlow(ObserverAwareFinalityFlow(stx, observerSessions + issuerSession))
    }
}
