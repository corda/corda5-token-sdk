package com.r3.corda.lib.tokens.workflows.flows.evolvable

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import net.corda.systemflows.CollectSignaturesFlow
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.flows.flowservices.dependencies.CordaInject
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.node.services.IdentityService
import net.corda.v5.application.node.services.persistence.PersistenceService
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.transactions.SignedTransaction
import net.corda.v5.ledger.transactions.TransactionBuilderFactory

/**
 * A flow to update an existing evolvable token type which is already recorded on the ledger. This is an IN-LINE flow
 * which means it MUST be invoked with a subFlow call from an Initiating Flow.
 *
 * @property oldStateAndRef the existing evolvable token type to update
 * @property newState the new version of the evolvable token type
 * @property participantSessions a list of sessions for participants in the evolvable token types
 * @property observerSessions a list of sessions for any observers to the create observable token transaction
 */
class UpdateEvolvableTokenFlow @JvmOverloads constructor(
    val oldStateAndRef: StateAndRef<EvolvableTokenType>,
    val newState: EvolvableTokenType,
    val participantSessions: List<FlowSession>,
    val observerSessions: List<FlowSession> = emptyList()
) : Flow<SignedTransaction> {
    /**
     * Simple notification class to inform counterparties of their role. In this instance, informs participants if
     * they are required to sign the command. This is intended to allow maintainers to sign commands while participants
     * and other observers merely finalise the transaction.
     */
    @CordaSerializable
    data class Notification(val signatureRequired: Boolean = false)

    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @CordaInject
    lateinit var transactionBuilderFactory: TransactionBuilderFactory

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var identityService: IdentityService

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @Suspendable
    override fun call(): SignedTransaction {
        require(flowIdentity.ourIdentity in oldStateAndRef.state.data.maintainers) {
            "This flow can only be started by existing maintainers of the EvolvableTokenType."
        }

        // Create a transaction which updates the ledger with the new evolvable token.
        // The tokenHolders listed as maintainers in the old state should be the signers.
        // TODO Should this be both old and new maintainer lists?
        val utx = addUpdateEvolvableToken(
            transactionBuilderFactory.create().setNotary(notary = oldStateAndRef.state.notary),
            oldStateAndRef,
            newState
        )

        // Sign the transaction proposal (creating a partially signed transaction, or ptx)
        val ptx: SignedTransaction = utx.sign()

        // Gather signatures from other maintainers
        val otherMaintainerSessions =
            participantSessions.filter { it.counterparty in evolvableTokens.otherMaintainers(flowIdentity.ourIdentity) }
        otherMaintainerSessions.forEach { it.send(Notification(signatureRequired = true)) }
        val stx = flowEngine.subFlow(
            CollectSignaturesFlow(
                partiallySignedTx = ptx,
                sessionsToCollectFrom = otherMaintainerSessions
            )
        )

        // Distribute to all observers, including maintainers, participants, and subscribers (via distribution list)
        val wellKnownObserverSessions = participantSessions.filter { it.counterparty in wellKnownObservers }
        val allObserverSessions = (wellKnownObserverSessions + observerSessions).toSet()
        observerSessions.forEach { it.send(Notification(signatureRequired = false)) }
        return flowEngine.subFlow(
            ObserverAwareFinalityFlow(
                signedTransaction = stx,
                allSessions = otherMaintainerSessions + allObserverSessions
            )
        )
    }

    // TODO Refactor it more.
    private val oldState get() = oldStateAndRef.state.data
    private val evolvableTokens = listOf(oldState, newState)

    private fun otherObservers(subscribers: Set<Party>): Set<AbstractParty> {
        return (evolvableTokens.participants() + subscribers).minus(evolvableTokens.maintainers()).minus(flowIdentity.ourIdentity)
    }

    private val wellKnownObservers
        get(): List<Party> {
            return otherObservers(
                subscribersForState(
                    newState,
                    persistenceService
                )
            ).map { identityService.wellKnownPartyFromAnonymous(it)!! }
        }
}
