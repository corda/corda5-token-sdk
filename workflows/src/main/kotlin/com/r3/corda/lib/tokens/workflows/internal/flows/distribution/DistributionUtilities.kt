@file:JvmName("DistributionUtilities")

package com.r3.corda.lib.tokens.workflows.internal.flows.distribution

import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.workflows.internal.schemas.DistributionRecord
import com.r3.corda.lib.tokens.workflows.utilities.addPartyToDistributionList
import com.r3.corda.lib.tokens.workflows.utilities.requireKnownConfidentialIdentity
import com.r3.corda.lib.tokens.workflows.utilities.toParty
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.identity.Party
import net.corda.v5.application.node.services.IdentityService
import net.corda.v5.application.node.services.PersistenceService
import net.corda.v5.application.node.services.runWithEntityManager
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.services.StateRefLoaderService
import net.corda.v5.ledger.services.VaultService
import java.util.*
import javax.persistence.criteria.CriteriaQuery

@CordaSerializable
data class DistributionListUpdate(val sender: Party, val receiver: Party, val linearId: UniqueIdentifier)

// Gets the distribution list for a particular token.
fun getDistributionList(persistenceService: PersistenceService, linearId: UniqueIdentifier): List<DistributionRecord> {
    return persistenceService.runWithEntityManager {
        val query: CriteriaQuery<DistributionRecord> = criteriaBuilder.createQuery(DistributionRecord::class.java)
        query.apply {
            val root = from(DistributionRecord::class.java)
            where(criteriaBuilder.equal(root.get<UUID>("linearId"), linearId.id))
            select(root)
        }
        createQuery(query).resultList
    }
}

// Gets the distribution record for a particular token and party.
fun getDistributionRecord(persistenceService: PersistenceService, linearId: UniqueIdentifier, party: Party): DistributionRecord? {
    return persistenceService.runWithEntityManager {
        val query: CriteriaQuery<DistributionRecord> = criteriaBuilder.createQuery(DistributionRecord::class.java)
        query.apply {
            val root = from(DistributionRecord::class.java)
            val linearIdEq = criteriaBuilder.equal(root.get<UUID>("linearId"), linearId.id)
            val partyEq = criteriaBuilder.equal(root.get<Party>("party"), party)
            where(criteriaBuilder.and(linearIdEq, partyEq))
            select(root)
        }
        createQuery(query).resultList
    }.singleOrNull()
}

fun hasDistributionRecord(persistenceService: PersistenceService, linearId: UniqueIdentifier, party: Party): Boolean {
    return getDistributionRecord(persistenceService, linearId, party) != null
}

/**
 * Utility function to persist a new entity pertaining to a distribution record.
 * TODO: Add some error handling.
 * TODO: Don't duplicate pairs of linearId and party.
 */
fun PersistenceService.addPartyToDistributionList(party: Party, linearId: UniqueIdentifier) {
    // Create an persist a new entity.
    val distributionRecord = DistributionRecord(linearId.id, party)
    runWithEntityManager { persist(distributionRecord) }
}

@Suspendable
fun Flow<*>.addToDistributionList(
    identityService: IdentityService,
    persistenceService: PersistenceService,
    tokens: List<AbstractToken>
) {
    tokens.filter { it.tokenType as? TokenPointer<*> != null }.forEach { token ->
        val tokenType = token.tokenType as TokenPointer<*>
        val pointer = tokenType.pointer.pointer
        val holder = token.holder.toParty(identityService)
        addPartyToDistributionList(persistenceService, holder, pointer)
    }
}

@Suspendable
fun Flow<*>.updateDistributionList(
    identityService: IdentityService,
    vaultService: VaultService,
    stateRefLoaderService: StateRefLoaderService,
    flowMessaging: FlowMessaging,
    ourIdentity: Party,
    tokens: List<AbstractToken>
) {
    tokens.filter { it.tokenType as? TokenPointer<*> != null }.forEach { token ->
        val tokenPointer = token.tokenType as TokenPointer<*>
        val holderParty = identityService.requireKnownConfidentialIdentity(token.holder)
        val evolvableToken = tokenPointer.pointer.resolve(vaultService, stateRefLoaderService).state.data
        val distributionListUpdate = DistributionListUpdate(ourIdentity, holderParty, evolvableToken.linearId)
        val maintainers = evolvableToken.maintainers
        val maintainersSessions = maintainers.map(flowMessaging::initiateFlow)
        maintainersSessions.forEach {
            it.send(distributionListUpdate)
        }
    }
}