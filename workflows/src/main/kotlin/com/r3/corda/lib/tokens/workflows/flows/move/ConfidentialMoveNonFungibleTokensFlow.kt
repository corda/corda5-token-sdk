package com.r3.corda.lib.tokens.workflows.flows.move

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.confidential.ConfidentialTokensFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.TransactionRole
import com.r3.corda.lib.tokens.workflows.internal.selection.generateMoveNonFungible
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.dependencies.CordaInject
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.services.VaultService
import net.corda.v5.ledger.services.vault.QueryCriteria
import net.corda.v5.ledger.transactions.SignedTransaction

/**
 * Version of [MoveNonFungibleTokensFlow] using confidential identities. Confidential identities are generated and
 * exchanged for all parties that receive tokens states.
 *
 * Call this for one [TokenType] at a time. If you need to do multiple token types in one transaction then create a new
 * flow, calling [addMoveFungibleTokens] for each token type and handle confidential identities exchange yourself.
 *
 * @param partyAndToken list of pairing party - token that is to be moved to that party
 * @param participantSessions sessions with the participants of move transaction
 * @param observerSessions optional sessions with the observer nodes, to witch the transaction will be broadcasted
 * @param queryCriteria additional criteria for token selection
 */
class ConfidentialMoveNonFungibleTokensFlow
@JvmOverloads
constructor(
    val partyAndToken: PartyAndToken,
    val participantSessions: List<FlowSession>,
    val observerSessions: List<FlowSession> = emptyList(),
    val queryCriteria: QueryCriteria? = null
) : Flow<SignedTransaction> {

    @CordaInject
    lateinit var vaultService: VaultService

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(): SignedTransaction {
        val (input, output) = generateMoveNonFungible(partyAndToken, vaultService, queryCriteria)
        // TODO Not pretty fix, because we decided to go with sessions approach, we need to make sure that right responders are started depending on observer/participant role
        participantSessions.forEach { it.send(TransactionRole.PARTICIPANT) }
        observerSessions.forEach { it.send(TransactionRole.OBSERVER) }
        val confidentialOutput = flowEngine.subFlow(ConfidentialTokensFlow(listOf(output), participantSessions)).single()
        return flowEngine.subFlow(MoveTokensFlow(input, confidentialOutput, participantSessions, observerSessions))
    }
}
