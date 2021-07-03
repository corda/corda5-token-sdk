package com.r3.corda.lib.tokens.workflows.flows.issue

import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.workflows.flows.confidential.ConfidentialTokensFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.TransactionRole
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.transactions.SignedTransaction

/**
 * A flow for issuing tokens to confidential keys. To be used in conjunction with the
 * [ConfidentialIssueTokensFlowHandler].
 *
 * @property tokens a list of tokens to issue.
 * @property participantSessions a list of sessions for the parties being issued tokens.
 * @property observerSessions a list of sessions for any observers.
 */
class ConfidentialIssueTokensFlow (
    val tokens: List<AbstractToken>,
    val participantSessions: List<FlowSession>,
    val observerSessions: List<FlowSession>
) : Flow<SignedTransaction> {

    constructor(
        tokens: List<AbstractToken>,
        participantSessions: List<FlowSession>,
    ) : this(tokens, participantSessions, emptyList())

    @CordaInject
    lateinit var flowEngine: FlowEngine

    /** Issue a single [FungibleToken]. */
    constructor(
        token: FungibleToken,
        participantSessions: List<FlowSession>,
        observerSessions: List<FlowSession>
    ) : this(listOf(token), participantSessions, observerSessions)

    constructor(
        token: FungibleToken,
        participantSessions: List<FlowSession>,
    ) : this(listOf(token), participantSessions, emptyList())

    /** Issue a single [FungibleToken] to self with no observers. */
    constructor(token: FungibleToken) : this(listOf(token), emptyList(), emptyList())

    /** Issue a single [NonFungibleToken]. */
    constructor(
        token: NonFungibleToken,
        participantSessions: List<FlowSession>,
        observerSessions: List<FlowSession>
    ) : this(listOf(token), participantSessions, observerSessions)

    constructor(
        token: NonFungibleToken,
        participantSessions: List<FlowSession>,
    ) : this(listOf(token), participantSessions, emptyList())

    /** Issue a single [NonFungibleToken] to self with no observers. */
    constructor(token: NonFungibleToken) : this(listOf(token), emptyList(), emptyList())

    @Suspendable
    override fun call(): SignedTransaction {
        // TODO Not pretty fix, because we decided to go with sessions approach, we need to make sure that right responders are started depending on observer/participant role
        participantSessions.forEach { it.send(TransactionRole.PARTICIPANT) }
        observerSessions.forEach { it.send(TransactionRole.OBSERVER) }
        // Request new keys pairs from all proposed token holders.
        val confidentialTokens = flowEngine.subFlow(ConfidentialTokensFlow(tokens, participantSessions))
        // Issue tokens using the existing participantSessions.
        return flowEngine.subFlow(IssueTokensFlow(confidentialTokens, participantSessions, observerSessions))
    }
}
