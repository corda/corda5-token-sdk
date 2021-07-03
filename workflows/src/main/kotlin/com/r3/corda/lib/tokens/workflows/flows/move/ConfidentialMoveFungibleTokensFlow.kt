package com.r3.corda.lib.tokens.workflows.flows.move

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.flows.confidential.ConfidentialTokensFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.TransactionRole
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.types.toPairs
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.node.MemberInfo
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.transactions.SignedTransaction

/**
 * Version of [MoveFungibleTokensFlow] using confidential identities. Confidential identities are generated and
 * exchanged for all parties that receive tokens states.
 *
 * Call this for one [TokenType] at a time. If you need to do multiple token types in one transaction then create a new
 * flow, calling [addMoveNonFungibleTokens] for each token type and handle confidential identities exchange yourself.
 *
 * @param partiesAndAmounts list of pairing party - amount of token that is to be moved to that party
 * @param participantSessions sessions with the participants of move transaction
 * @param changeHolder holder of the change outputs, it can be confidential identity
 * @param observerSessions optional sessions with the observer nodes, to witch the transaction will be broadcasted
 */
class ConfidentialMoveFungibleTokensFlow (
    val partiesAndAmounts: List<PartyAndAmount<TokenType>>,
    val participantSessions: List<FlowSession>,
    val changeHolder: AbstractParty,
    val observerSessions: List<FlowSession>,
) : Flow<SignedTransaction> {

    constructor(
        partiesAndAmounts: List<PartyAndAmount<TokenType>>,
        participantSessions: List<FlowSession>,
        changeHolder: AbstractParty
    ) : this (partiesAndAmounts, participantSessions, changeHolder, emptyList())

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @CordaInject
    lateinit var identityService: IdentityService

    @CordaInject
    lateinit var memberInfo: MemberInfo

    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
        participantSessions: List<FlowSession>,
        changeHolder: AbstractParty,
        observerSessions: List<FlowSession>
    ) : this(listOf(partyAndAmount), participantSessions, changeHolder, observerSessions)

    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
        participantSessions: List<FlowSession>,
        changeHolder: AbstractParty,
    ) : this(listOf(partyAndAmount), participantSessions, changeHolder, emptyList())

    @Suspendable
    override fun call(): SignedTransaction {
        // TODO add in memory selection too
        val tokenSelection = DatabaseTokenSelection(persistenceService, identityService, flowEngine)
        val (inputs, outputs) = tokenSelection.generateMove(
            identityService,
            memberInfo,
            lockId = flowEngine.runId.uuid,
            partiesAndAmounts = partiesAndAmounts.toPairs(),
            changeHolder = changeHolder,
            queryBy = TokenQueryBy()
        )
        // TODO Not pretty fix, because we decided to go with sessions approach, we need to make sure that right responders are started depending on observer/participant role
        participantSessions.forEach { it.send(TransactionRole.PARTICIPANT) }
        observerSessions.forEach { it.send(TransactionRole.OBSERVER) }
        val confidentialOutputs = flowEngine.subFlow(ConfidentialTokensFlow(outputs, participantSessions))
        return flowEngine.subFlow(MoveTokensFlow(inputs, confidentialOutputs, participantSessions, observerSessions))
    }
}
