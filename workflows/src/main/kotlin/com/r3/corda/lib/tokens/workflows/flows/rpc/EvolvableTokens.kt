package com.r3.corda.lib.tokens.workflows.flows.rpc

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.workflows.flows.evolvable.CreateEvolvableTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.evolvable.CreateEvolvableTokensFlowHandler
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlow
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlowHandler
import com.r3.corda.lib.tokens.workflows.flows.evolvable.maintainers
import com.r3.corda.lib.tokens.workflows.flows.evolvable.otherMaintainers
import com.r3.corda.lib.tokens.workflows.flows.evolvable.participants
import com.r3.corda.lib.tokens.workflows.flows.evolvable.subscribersForState
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.flows.flowservices.dependencies.CordaInject
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.node.services.IdentityService
import net.corda.v5.application.node.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.contracts.TransactionState
import net.corda.v5.ledger.transactions.SignedTransaction

/**
 * Initiating flow for creating multiple tokens of evolvable token type.
 *
 * @property transactionStates a list of states to create evolvable token types with
 * @param observers optional observing parties to which the transaction will be broadcast
 */
@InitiatingFlow
@StartableByRPC
class CreateEvolvableTokens
@JvmOverloads
constructor(
    val transactionStates: List<TransactionState<EvolvableTokenType>>,
    val observers: List<Party> = emptyList()
) : Flow<SignedTransaction> {

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @CordaInject
    lateinit var identityService: IdentityService

    @JvmOverloads
    constructor(
        transactionState: TransactionState<EvolvableTokenType>,
        observers: List<Party> = emptyList()
    ) : this(listOf(transactionState), observers)

    @Suspendable
    override fun call(): SignedTransaction {
        // Initiate sessions to all observers.
        val observersSessions = (observers + statesObservers).toSet().map { flowMessaging.initiateFlow(it) }
        // Initiate sessions to all maintainers but our node.
        val participantsSessions: List<FlowSession> =
            evolvableTokens.otherMaintainers(flowIdentity.ourIdentity).map { flowMessaging.initiateFlow(it) }
        return flowEngine.subFlow(CreateEvolvableTokensFlow(transactionStates, participantsSessions, observersSessions))
    }

    private val evolvableTokens = transactionStates.map { it.data }

    // TODO Refactor it more.
    private val statesObservers
        get(): List<Party> {
            val observers = evolvableTokens.participants().minus(evolvableTokens.maintainers()).minus(flowIdentity.ourIdentity)
            return observers.map { identityService.wellKnownPartyFromAnonymous(it)!! }
        }
}

/**
 * Responder flow for [CreateEvolvableTokens].
 */
@InitiatedBy(CreateEvolvableTokens::class)
class CreateEvolvableTokensHandler(val otherSession: FlowSession) : Flow<Unit> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() = flowEngine.subFlow(CreateEvolvableTokensFlowHandler(otherSession))
}

/**
 * An initiating flow to update an existing evolvable token type which is already recorded on the ledger. This is an
 *
 * @property oldStateAndRef the existing evolvable token type to update
 * @property newState the new version of the evolvable token type
 * @param observers optional observing parties to which the transaction will be broadcast
 */
@InitiatingFlow
@StartableByRPC
class UpdateEvolvableToken
@JvmOverloads
constructor(
    val oldStateAndRef: StateAndRef<EvolvableTokenType>,
    val newState: EvolvableTokenType,
    val observers: List<Party> = emptyList()
) : Flow<SignedTransaction> {

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @CordaInject
    lateinit var identityService: IdentityService

    @Suspendable
    override fun call(): SignedTransaction {
        // Initiate sessions to all observers.
        val observersSessions = (observers + statesObservers).toSet().map { flowMessaging.initiateFlow(it) }
        // Initiate sessions to all maintainers but our node.
        val participantsSessions: List<FlowSession> =
            evolvableTokens.otherMaintainers(flowIdentity.ourIdentity).map { flowMessaging.initiateFlow(it) }
        return flowEngine.subFlow(UpdateEvolvableTokenFlow(oldStateAndRef, newState, participantsSessions, observersSessions))
    }

    private val oldState get() = oldStateAndRef.state.data
    private val evolvableTokens = listOf(oldState, newState)

    // TODO Refactor it more.
    private val otherObservers
        get(): Set<AbstractParty> {
            return (evolvableTokens.participants() + subscribersForState(newState, persistenceService))
                .minus(evolvableTokens.maintainers()).minus(flowIdentity.ourIdentity)
        }

    private val statesObservers
        get(): List<Party> {
            return otherObservers.map { identityService.wellKnownPartyFromAnonymous(it)!! }
        }
}

/**
 * Responder flow for [UpdateEvolvableToken].
 */
@InitiatedBy(UpdateEvolvableToken::class)
class UpdateEvolvableTokenHandler(val otherSession: FlowSession) : Flow<Unit> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() = flowEngine.subFlow(UpdateEvolvableTokenFlowHandler(otherSession))
}
