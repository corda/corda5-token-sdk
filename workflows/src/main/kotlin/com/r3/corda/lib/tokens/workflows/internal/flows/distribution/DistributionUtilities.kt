@file:JvmName("DistributionUtilities")

package com.r3.corda.lib.tokens.workflows.internal.flows.distribution

import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.workflows.internal.schemas.DistributionRecordSchemaV1.DistributionRecord
import com.r3.corda.lib.tokens.workflows.utilities.addPartyToDistributionList
import com.r3.corda.lib.tokens.workflows.utilities.requireKnownConfidentialIdentity
import com.r3.corda.lib.tokens.workflows.utilities.toParty
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.stream.Cursor
import net.corda.v5.base.util.seconds
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.services.StateLoaderService


@CordaSerializable
data class DistributionListUpdate(val sender: Party, val receiver: Party, val linearId: UniqueIdentifier)

// Gets the distribution list for a particular token.
fun getDistributionList(persistenceService: PersistenceService, linearId: UniqueIdentifier): List<DistributionRecord> {
	val cursor = persistenceService.query<DistributionRecord>(
		"DistributionRecord.findByLinearId",
		mapOf("linearId" to linearId.id)
	)
	val accumulator = mutableListOf<DistributionRecord>()
	var poll: Cursor.PollResult<DistributionRecord>?
	do {
		poll = cursor.poll(50, 10.seconds)
		val elements = poll.values
		accumulator.addAll(elements)
	} while (poll != null && !poll.isLastResult)
	return accumulator
}

// Gets the distribution record for a particular token and party.
fun getDistributionRecord(persistenceService: PersistenceService, linearId: UniqueIdentifier, party: Party): DistributionRecord? {
	return persistenceService.query<DistributionRecord>(
		"DistributionRecord.findByLinearIdAndParty",
		mapOf("linearId" to linearId.id, "party" to party)
	).poll(1, 10.seconds)
		.values
		.singleOrNull()
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
	persist(DistributionRecord(linearId.id, party))
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
fun updateDistributionList(
	identityService: IdentityService,
	stateLoaderService: StateLoaderService,
	flowMessaging: FlowMessaging,
	ourIdentity: Party,
	tokens: List<AbstractToken>
) {
	tokens.filter { it.tokenType as? TokenPointer<*> != null }.forEach { token ->
		val tokenPointer = token.tokenType as TokenPointer<*>
		val holderParty = identityService.requireKnownConfidentialIdentity(token.holder)
		val evolvableToken = stateLoaderService.resolve(tokenPointer.pointer).state.data
		val distributionListUpdate = DistributionListUpdate(ourIdentity, holderParty, evolvableToken.linearId)
		val maintainers = evolvableToken.maintainers
		val maintainersSessions = maintainers.map(flowMessaging::initiateFlow)
		maintainersSessions.forEach {
			it.send(distributionListUpdate)
		}
	}
}