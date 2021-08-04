package com.r3.corda.lib.tokens.workflows.internal.flows.confidential

import com.r3.corda.lib.ci.workflows.RequestKeyFlow
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.identity.AnonymousParty
import net.corda.v5.application.identity.Party
import net.corda.v5.base.annotations.Suspendable

/**
 * This flow notifies prospective token holders that they must generate a new key pair. As this is an in-line sub-flow,
 * we must pass it a list of sessions, which _may_ contain sessions for observers. As such, only the parties that need
 * to generate a new key are sent a [ActionRequest.CREATE_NEW_KEY] notification and everyone else is sent
 * [ActionRequest.DO_NOTHING].
 */
class AnonymisePartiesFlow(
    val parties: List<Party>,
    val sessions: List<FlowSession>
) : Flow<Map<Party, AnonymousParty>> {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(): Map<Party, AnonymousParty> {
        val sessionParties = sessions.map(FlowSession::counterparty)
        val partiesWithoutASession = parties.minus(sessionParties)
        require(partiesWithoutASession.isEmpty()) {
            "You must provide sessions for all parties. " +
                    "No sessions provided for parties: $partiesWithoutASession"
        }
        return sessions.mapNotNull { session ->
            val party = session.counterparty
            if (party in parties) {
                session.send(ActionRequest.CREATE_NEW_KEY)
                val anonParty = flowEngine.subFlow(RequestKeyFlow(session))
                Pair(party, anonParty)
            } else {
                session.send(ActionRequest.DO_NOTHING)
                null
            }
        }.toMap()
    }
}