@file:JvmName("FlowUtilities")

package com.r3.corda.lib.tokens.workflows.utilities

import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.hasDistributionRecord
import com.r3.corda.lib.tokens.workflows.internal.schemas.DistributionRecordSchemaV1
import com.r3.corda.lib.tokens.workflows.internal.schemas.DistributionRecordSchemaV1.DistributionRecord
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.KeyManagementService
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.contracts.ContractState
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.transactions.LedgerTransaction
import net.corda.v5.ledger.transactions.TransactionBuilder
import java.security.PublicKey

/**
 * Utility function to persist a new entity pertaining to a distribution record.
 * TODO: Add some error handling.
 */
@Suspendable
fun Flow<*>.addPartyToDistributionList(persistenceService: PersistenceService, party: Party, linearId: UniqueIdentifier) {
    // Create an persist a new entity.
    val hasRecord = hasDistributionRecord(persistenceService, linearId, party)
    if (!hasRecord) {
        val distributionRecord = DistributionRecord(linearId.id, party)
        persistenceService.persist(distributionRecord)
    } else {
        contextLogger().info("Already stored a distribution record for $party and $linearId.")
    }
}

val LedgerTransaction.participants: List<AbstractParty>
    get() {
        val inputParticipants = inputStates.flatMap(ContractState::participants)
        val outputParticipants = outputStates.flatMap(ContractState::participants)
        return inputParticipants + outputParticipants
    }

@Suspendable
fun LedgerTransaction.ourSigningKeys(keyManagementService: KeyManagementService): List<PublicKey> {
    val signingKeys = commands.flatMap(Command<*>::signers)
    return keyManagementService.filterMyKeys(signingKeys).toList()
}

@Suspendable
fun AbstractParty.toParty(identityService: IdentityService) = identityService.requireKnownConfidentialIdentity(this)

@Suspendable
fun List<AbstractParty>.toWellKnownParties(identityService: IdentityService): List<Party> {
    return map(identityService::requireKnownConfidentialIdentity)
}

// Needs to deal with confidential identities.
@Suspendable
fun requireSessionsForParticipants(participants: Collection<Party>, sessions: List<FlowSession>) {
    val sessionParties = sessions.map(FlowSession::counterparty)
    require(sessionParties.containsAll(participants)) {
        val missing = participants - sessionParties
        "There should be a flow session for all state participants. Sessions are missing for $missing."
    }
}

@Suspendable
fun Flow<*>.sessionsForParticipants(
    identityService: IdentityService,
    flowMessaging: FlowMessaging,
    states: List<ContractState>
): List<FlowSession> {
    val stateParties = states.flatMap(ContractState::participants)
    return sessionsForParties(identityService, flowMessaging, stateParties)
}

@Suspendable
fun Flow<*>.sessionsForParties(
    identityService: IdentityService,
    flowMessaging: FlowMessaging,
    parties: List<AbstractParty>
): List<FlowSession> {
    val wellKnownParties = parties.toWellKnownParties(identityService)
    return wellKnownParties.map(flowMessaging::initiateFlow)
}

// Extension function that has nicer error message than the default one from [IdentityService::requireWellKnownPartyFromAnonymous].
@Suspendable
fun IdentityService.requireKnownConfidentialIdentity(party: AbstractParty): Party {
    return partyFromAnonymous(party)
        ?: throw IllegalArgumentException(
            "Called flow with anonymous party that node doesn't know about. " +
                    "Make sure that RequestConfidentialIdentity flow is called before."
        )
}

// Utilities for ensuring that the correct JAR which implements TokenType is added to the transaction.

fun addTokenTypeJar(tokens: List<AbstractToken>, transactionBuilder: TransactionBuilder) {
    tokens.forEach {
        // If there's no JAR hash then we don't need to do anything.
        val hash = it.tokenTypeJarHash ?: return
        if (!transactionBuilder.attachments.contains(hash)) {
            transactionBuilder.addAttachment(hash)
        }
    }
}

fun addTokenTypeJar(tokens: Iterable<StateAndRef<AbstractToken>>, transactionBuilder: TransactionBuilder) {
    addTokenTypeJar(tokens.map { it.state.data }, transactionBuilder)
}

fun addTokenTypeJar(changeOutput: AbstractToken, transactionBuilder: TransactionBuilder) {
    addTokenTypeJar(listOf(changeOutput), transactionBuilder)
}

fun addTokenTypeJar(input: StateAndRef<AbstractToken>, transactionBuilder: TransactionBuilder) {
    addTokenTypeJar(input.state.data, transactionBuilder)
}

