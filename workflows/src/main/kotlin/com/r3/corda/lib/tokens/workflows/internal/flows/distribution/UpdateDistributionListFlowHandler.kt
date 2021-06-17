package com.r3.corda.lib.tokens.workflows.internal.flows.distribution

import com.r3.corda.lib.tokens.workflows.utilities.addPartyToDistributionList
import com.r3.corda.lib.tokens.workflows.utilities.requireKnownConfidentialIdentity
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.application.flows.unwrap
import net.corda.v5.base.annotations.Suspendable

@InitiatedBy(UpdateDistributionListFlow::class)
class UpdateDistributionListFlowHandler(val otherSession: FlowSession) : Flow<Unit> {

    @CordaInject
    lateinit var identityService: IdentityService

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @Suspendable
    override fun call() {
        val distListUpdate = otherSession.receive<DistributionListUpdate>().unwrap {
            // Check that the request comes from that party.
            check(it.sender == otherSession.counterparty) {
                "Got distribution list update request from a counterparty: ${otherSession.counterparty} " +
                        "that isn't a signer of request: ${it.sender}."
            }
            it
        }
        // Check that receiver is well known party.
        identityService.requireKnownConfidentialIdentity(distListUpdate.receiver)
        addPartyToDistributionList(persistenceService, distListUpdate.receiver, distListUpdate.linearId)
    }
}