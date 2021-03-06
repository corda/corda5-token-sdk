package com.r3.corda.lib.tokens.workflows.flows.confidential

import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.workflows.internal.flows.confidential.AnonymisePartiesFlow
import com.r3.corda.lib.tokens.workflows.utilities.toWellKnownParties
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.IdentityService
import net.corda.v5.base.annotations.Suspendable

/**
 * This flow extracts the holders from a list of tokens to be issued on ledger, then requests only the well known
 * holders to generate a new key pair for holding the new asset. The new key pair effectively anonymises them. The
 * newly generated public keys replace the old, well known, keys. The flow doesn't request new keys for
 * [AnonymousParty]s.
 *
 * This is an in-line flow and use of it should be paired with [ConfidentialTokensFlowHandler].
 *
 * Note that this flow should only be called if you are dealing with [Party]s as individual nodes, i.e. not accounts.
 * When issuing tokens to accounts, the public keys + tokens need to be generated up-front as passed into the
 * [IssueTokensFlow].
 *
 * @property tokens a list of [AbstractToken]s.
 * @property sessions a list of participants' sessions which may contain sessions for observers.
 */
class ConfidentialTokensFlow(
    val tokens: List<AbstractToken>,
    val sessions: List<FlowSession>
) : Flow<List<AbstractToken>> {

    @CordaInject
    lateinit var identityService: IdentityService

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(): List<AbstractToken> {
        // Some holders might be anonymous already. E.g. if some token selection has been performed and a confidential
        // change address was requested.
        val tokensWithWellKnownHolders = tokens.filter { it.holder is Party }
        val tokensWithAnonymousHolders = tokens - tokensWithWellKnownHolders
        val wellKnownTokenHolders = tokensWithWellKnownHolders
            .map(AbstractToken::holder)
            .toWellKnownParties(identityService)
        val anonymousParties = flowEngine.subFlow(AnonymisePartiesFlow(wellKnownTokenHolders, sessions))
        // Replace Party with AnonymousParty.
        return tokensWithWellKnownHolders.map { token ->
            val holder = token.holder
            val anonymousParty = anonymousParties[holder]
                ?: throw IllegalStateException("Missing anonymous party for $holder.")
            token.withNewHolder(anonymousParty)
        } + tokensWithAnonymousHolders
    }
}
