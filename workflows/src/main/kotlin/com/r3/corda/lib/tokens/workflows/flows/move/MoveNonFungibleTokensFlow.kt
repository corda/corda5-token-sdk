package com.r3.corda.lib.tokens.workflows.flows.move

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.transactions.TransactionBuilder

/**
 * Inlined flow used to move non fungible tokens to parties, [partiesAndTokens] specifies what tokens are moved
 * to each participant.
 *
 * Call this for one [TokenType] at a time. If you need to do multiple token types in one transaction then create a new
 * flow, calling [addMoveNonFungibleTokens] for each token type.
 *
 * @param partyAndToken pairing party - token that is to be moved to that party
 * @param participantSessions sessions with the participants of move transaction
 * @param observerSessions optional sessions with the observer nodes, to witch the transaction will be broadcasted
 * @param customPostProcessorName name of custom query post processor for token selection
 */
class MoveNonFungibleTokensFlow (
    val partyAndToken: PartyAndToken,
    override val participantSessions: List<FlowSession>,
    override val observerSessions: List<FlowSession>,
    val customPostProcessorName: String?
) : AbstractMoveTokensFlow() {

    constructor(
        partyAndToken: PartyAndToken,
        participantSessions: List<FlowSession>,
    ) : this(partyAndToken, participantSessions, emptyList(), null)

    constructor(
        partyAndToken: PartyAndToken,
        participantSessions: List<FlowSession>,
        observerSessions: List<FlowSession>,
    ) : this(partyAndToken, participantSessions, observerSessions, null)

    constructor(
        partyAndToken: PartyAndToken,
        participantSessions: List<FlowSession>,
        customPostProcessorName: String?
    ) : this(partyAndToken, participantSessions, emptyList(), customPostProcessorName)

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @Suspendable
    override fun addMove(transactionBuilder: TransactionBuilder) {
        addMoveNonFungibleTokens(transactionBuilder, persistenceService, partyAndToken, TokenQueryBy(customPostProcessorName))
    }
}