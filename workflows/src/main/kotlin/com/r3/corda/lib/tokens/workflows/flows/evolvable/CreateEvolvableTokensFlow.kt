package com.r3.corda.lib.tokens.workflows.flows.evolvable

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import net.corda.systemflows.CollectSignaturesFlow
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.IdentityService
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.TransactionState
import net.corda.v5.ledger.services.TransactionService
import net.corda.v5.ledger.transactions.SignedTransaction
import net.corda.v5.ledger.transactions.TransactionBuilderFactory

/**
 * Inline sub-flow for creating multiple tokens of evolvable token type. This is just a simple flow for now.
 *
 * @property transactionStates a list of state to create evolvable token types with
 * @property participantSessions a list of sessions for participants in the evolvable token types
 * @property observerSessions a list of sessions for any observers to the create observable token transaction
 */
class CreateEvolvableTokensFlow
@JvmOverloads
constructor(
    val transactionStates: List<TransactionState<EvolvableTokenType>>,
    val participantSessions: List<FlowSession>,
    val observerSessions: List<FlowSession> = emptyList()
) : Flow<SignedTransaction> {
    @JvmOverloads
    constructor(
        transactionState: TransactionState<EvolvableTokenType>,
        participantSessions: List<FlowSession>,
        observerSessions: List<FlowSession> = emptyList()
    ) :
            this(listOf(transactionState), participantSessions, observerSessions)

    @CordaSerializable
    data class Notification(val signatureRequired: Boolean = false)

    private val evolvableTokens = transactionStates.map { it.data }

    @CordaInject
    lateinit var transactionBuilderFactory: TransactionBuilderFactory

    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var identityService: IdentityService

    @Suspendable
    override fun call(): SignedTransaction {
        checkLinearIds(transactionStates)
        // TODO what about... preferred notary
        checkSameNotary()
        val transactionBuilder = transactionBuilderFactory.create().setNotary(transactionStates.first().notary) // todo

        // Create a transaction which updates the ledger with the new evolvable tokens.
        transactionStates.forEach {
            addCreateEvolvableToken(transactionBuilder, it)
        }

        // Sign the transaction proposal
        val ptx: SignedTransaction = transactionBuilder.sign()

        // Gather signatures from other maintainers
        // Check that we have sessions with all maitainers but not with ourselves
        val otherMaintainerSessions =
            participantSessions.filter { it.counterparty in evolvableTokens.otherMaintainers(flowIdentity.ourIdentity) }
        otherMaintainerSessions.forEach { it.send(Notification(signatureRequired = true)) }
        val stx = flowEngine.subFlow(
            CollectSignaturesFlow(
                partiallySignedTx = ptx,
                sessionsToCollectFrom = otherMaintainerSessions
            )
        )
        // Finalise with all participants, including maintainers, participants, and subscribers (via distribution list)
        val wellKnownObserverSessions = participantSessions.filter { it.counterparty in wellKnownObservers }
        val allObserverSessions = (wellKnownObserverSessions + observerSessions).toSet()
        allObserverSessions.forEach { it.send(Notification(signatureRequired = false)) }
        return flowEngine.subFlow(
            ObserverAwareFinalityFlow(
                signedTransaction = stx,
                allSessions = otherMaintainerSessions + allObserverSessions
            )
        )
    }

    private fun checkLinearIds(transactionStates: List<TransactionState<EvolvableTokenType>>) {
        check(transactionStates.map { it.data.linearId }.toSet().size == transactionStates.size) {
            "Shouldn't create evolvable tokens with the same linearId."
        }
    }

    private fun checkSameNotary() {
        check(transactionStates.map { it.notary }.toSet().size == 1) {
            "All states should have the same notary"
        }
    }

    // TODO Refactor it more.
    private val otherObservers
        get(): Set<AbstractParty> {
            return evolvableTokens.participants().minus(evolvableTokens.maintainers()).minus(flowIdentity.ourIdentity)
        }

    private val wellKnownObservers
        get(): List<Party> {
            return otherObservers.map { identityService.partyFromAnonymous(it)!! }
        }
}
