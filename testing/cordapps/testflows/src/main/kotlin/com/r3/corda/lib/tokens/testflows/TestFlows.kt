package com.r3.corda.lib.tokens.testflows

import com.r3.corda.lib.ci.workflows.SyncKeyMappingFlow
import com.r3.corda.lib.ci.workflows.SyncKeyMappingFlowHandler
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.selection.InsufficientBalanceException
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.selection.memory.selector.LocalTokenSelector
import com.r3.corda.lib.tokens.selection.memory.services.VaultWatcherService
import com.r3.corda.lib.tokens.testing.states.House
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.getDistributionList
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler
import com.r3.corda.lib.tokens.workflows.internal.schemas.DistributionRecordSchemaV1.DistributionRecord
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.r3.corda.lib.tokens.workflows.utilities.ourSigningKeys
import net.corda.systemflows.CollectSignaturesFlow
import net.corda.systemflows.ReceiveStateAndRefFlow
import net.corda.systemflows.SendStateAndRefFlow
import net.corda.systemflows.SignTransactionFlow
import net.corda.v5.application.cordapp.CordappProvider
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.flows.unwrap
import net.corda.v5.application.identity.Party
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.node.NodeInfo
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.crypto.KeyManagementService
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.contextLogger
import net.corda.v5.base.util.seconds
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.services.StateLoaderService
import net.corda.v5.ledger.services.TransactionMappingService
import net.corda.v5.ledger.transactions.SignedTransaction
import net.corda.v5.ledger.transactions.TransactionBuilderFactory
import java.time.Duration
import java.time.temporal.ChronoUnit

// This is very simple test flow for DvP.
@CordaSerializable
private class DvPNotification(val amount: Amount<TokenType>)

@StartableByRPC
@InitiatingFlow
class DvPFlow(val house: House, val newOwner: Party) : Flow<SignedTransaction> {

    @CordaInject
    lateinit var transactionBuilderFactory: TransactionBuilderFactory

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var transactionMappingService: TransactionMappingService

    @CordaInject
    lateinit var keyManagementService: KeyManagementService

    @CordaInject
    lateinit var notaryLookupService: NotaryLookupService

    @CordaInject
    lateinit var cordappProvider: CordappProvider

    @Suspendable
    override fun call(): SignedTransaction {
        val txBuilder = transactionBuilderFactory.create().setNotary(getPreferredNotary(notaryLookupService, cordappProvider.appConfig))
        addMoveNonFungibleTokens(txBuilder, persistenceService, house.toPointer<House>(), newOwner)
        val session = flowMessaging.initiateFlow(newOwner)
        // Ask for input stateAndRefs - send notification with the amount to exchange.
        session.send(DvPNotification(house.valuation))
        // TODO add some checks for inputs and outputs
        val inputs = flowEngine.subFlow(ReceiveStateAndRefFlow<FungibleToken>(session))
        // Receive outputs (this is just quick and dirty, we could calculate them on our side of the flow).
        val outputs = session.receive<List<FungibleToken>>().unwrap { it }
        addMoveTokens(txBuilder, inputs, outputs)
        // Synchronise any confidential identities
        flowEngine.subFlow(SyncKeyMappingFlow(session, txBuilder.toWireTransaction()))
        val ourSigningKeys =
            transactionMappingService.toLedgerTransaction(txBuilder.toWireTransaction()).ourSigningKeys(keyManagementService)
        val initialStx = txBuilder.sign(signingPubKeys = ourSigningKeys)
        val stx = flowEngine.subFlow(CollectSignaturesFlow(initialStx, listOf(session), ourSigningKeys))
        // Update distribution list.
        flowEngine.subFlow(UpdateDistributionListFlow(stx))
        return flowEngine.subFlow(ObserverAwareFinalityFlow(stx, listOf(session)))
    }
}

@InitiatedBy(DvPFlow::class)
class DvPFlowHandler(val otherSession: FlowSession) : Flow<Unit> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @CordaInject
    lateinit var identityService: IdentityService

    @CordaInject
    lateinit var nodeInfo: NodeInfo

    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @Suspendable
    override fun call() {
        // Receive notification with house price.
        val dvPNotification = otherSession.receive<DvPNotification>().unwrap { it }
        // Chose state and refs to send back.
        // TODO This is API pain, we assumed that we could just modify TransactionBuilder, but... it cannot be sent over the wire, because non-serializable
        // We need custom serializer and some custom flows to do checks.
        val changeHolder = flowIdentity.ourIdentity.anonymise()
        val (inputs, outputs) =
            DatabaseTokenSelection(persistenceService, identityService, flowEngine).generateMove(
                identityService,
                nodeInfo,
                lockId = flowEngine.runId.uuid,
                partiesAndAmounts = listOf(Pair(otherSession.counterparty, dvPNotification.amount)),
                changeHolder = changeHolder
            )
        flowEngine.subFlow(SendStateAndRefFlow(otherSession, inputs))
        otherSession.send(outputs)
        flowEngine.subFlow(SyncKeyMappingFlowHandler(otherSession))
        flowEngine.subFlow(object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) {}
        }
        )
        flowEngine.subFlow(ObserverAwareFinalityFlowHandler(otherSession))
    }
}

@StartableByRPC
class GetDistributionList(val housePtr: TokenPointer<House>) : Flow<List<DistributionRecord>> {
    @CordaInject
    lateinit var persistenceService: PersistenceService

    @Suspendable
    override fun call(): List<DistributionRecord> {
        return getDistributionList(persistenceService, housePtr.pointer.pointer)
    }
}

@StartableByRPC
class CheckTokenPointer(val housePtr: TokenPointer<House>) : Flow<House> {
    @CordaInject
    lateinit var stateLoaderService: StateLoaderService

    @Suspendable
    override fun call(): House {
        return stateLoaderService.resolve(housePtr.pointer).state.data
    }
}

// TODO This is hack that will be removed after fix in Corda 5. startFlowDynamic doesn't handle type parameters properly.
@StartableByRPC
class RedeemNonFungibleHouse(
    val housePtr: TokenPointer<House>,
    val issuerParty: Party
) : Flow<SignedTransaction> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(): SignedTransaction {
        return flowEngine.subFlow(RedeemNonFungibleTokens(housePtr, issuerParty, emptyList()))
    }
}

@StartableByRPC
class RedeemFungibleGBP(
    val amount: Amount<TokenType>,
    val issuerParty: Party
) : Flow<SignedTransaction> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(): SignedTransaction {
        return flowEngine.subFlow(RedeemFungibleTokens(amount, issuerParty, emptyList(), null))
    }
}

// Helper flow for selection testing
@StartableByRPC
class SelectAndLockFlow(val amount: Amount<TokenType>, val delay: Duration = 1.seconds) : Flow<Unit> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var vaultWatcherService: VaultWatcherService

    @Suspendable
    override fun call() {
        val selector = LocalTokenSelector(vaultWatcherService)
        selector.selectTokens(amount)
        flowEngine.sleep(delay)
    }
}

// Helper flow for selection testing
@StartableByRPC
class JustLocalSelect(
    val amount: Amount<TokenType>,
    val timeBetweenSelects: Duration = Duration.of(10, ChronoUnit.SECONDS),
    val maxSelectAttempts: Int = 5
) : Flow<List<StateAndRef<FungibleToken>>> {

    companion object {
        val logger = contextLogger()
    }

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var vaultWatcherService: VaultWatcherService

    @Suspendable
    override fun call(): List<StateAndRef<FungibleToken>> {
        val selector = LocalTokenSelector(vaultWatcherService)
        var selectionAttempts = 0
        while (selectionAttempts < maxSelectAttempts) {
            try {
                return selector.selectTokens(amount)
            } catch (e: InsufficientBalanceException) {
                logger.error("failed to select", e)
                flowEngine.sleep(timeBetweenSelects)
                selectionAttempts++
            }
        }
        throw InsufficientBalanceException("Could not select: ${amount}")
    }
}