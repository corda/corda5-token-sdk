package com.r3.corda.lib.tokens.workflows.internal.flows.finality

import com.r3.corda.lib.tokens.contracts.commands.RedeemTokenCommand
import com.r3.corda.lib.tokens.workflows.utilities.ourSigningKeys
import com.r3.corda.lib.tokens.workflows.utilities.participants
import com.r3.corda.lib.tokens.workflows.utilities.requireSessionsForParticipants
import com.r3.corda.lib.tokens.workflows.utilities.toWellKnownParties
import net.corda.systemflows.FinalityFlow
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.crypto.KeyManagementService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.services.TransactionMappingService
import net.corda.v5.ledger.transactions.LedgerTransaction
import net.corda.v5.ledger.transactions.SignedTransaction
import net.corda.v5.ledger.transactions.TransactionBuilder

/**
 * This flow is a wrapper around [FinalityFlow] and properly handles broadcasting transactions to observers (those which
 * are not transaction participants) by amending the [StatesToRecord] level based upon the role. Those which are not
 * participants in any of the states must invoke [FinalityFlow] with [StatesToRecord] set to ALL_VISIBLE, otherwise they
 * will not store any of the states. Those which are participants record the transaction as usual. This does mean that
 * there is an "all or nothing" approach to storing outputs for observers, so if there are privacy concerns, then it is
 * best to split state issuance up for different token holders in separate flow invocations.
 * If transaction is a redeem tokens transaction, the issuer is treated as a participant - it records transaction and
 * states with [StatesToRecord.ONLY_RELEVANT] set.
 *
 * @property transactionBuilder the transaction builder to finalise
 * @property signedTransaction if [CollectSignaturesFlow] was called before you can use this flow to finalise signed
 *  transaction with observers, notice that this flow can be called either with [transactionBuilder] or
 *  [signedTransaction]
 * @property allSessions a set of sessions for, at least, all the transaction participants and maybe observers
 */
class ObserverAwareFinalityFlow private constructor(
    val allSessions: List<FlowSession>,
    val signedTransaction: SignedTransaction? = null,
    val transactionBuilder: TransactionBuilder? = null
) : Flow<SignedTransaction> {

    @CordaInject
    lateinit var transactionMappingService: TransactionMappingService

    @CordaInject
    lateinit var identityService: IdentityService

    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @CordaInject
    lateinit var keyManagementService: KeyManagementService

    @CordaInject
    lateinit var flowEngine: FlowEngine

    constructor(transactionBuilder: TransactionBuilder, allSessions: List<FlowSession>)
            : this(allSessions, null, transactionBuilder)

    constructor(signedTransaction: SignedTransaction, allSessions: List<FlowSession>)
            : this(allSessions, signedTransaction)

    @Suspendable
    override fun call(): SignedTransaction {
        // Check there is a session for each participant, apart from the node itself.
        val ledgerTransaction: LedgerTransaction =
            transactionBuilder?.let { transactionMappingService.toLedgerTransaction(it.toWireTransaction()) }
                ?: transactionMappingService.toLedgerTransaction(signedTransaction!!, false)
        val participants: List<AbstractParty> = ledgerTransaction.participants
        val issuers: Set<Party> = ledgerTransaction.commands
            .map(Command<*>::value)
            .filterIsInstance<RedeemTokenCommand>()
            .map { it.token.issuer }
            .toSet()
        val wellKnownParticipantsAndIssuers: Set<Party> = participants.toWellKnownParties(identityService).toSet() + issuers
        val wellKnownParticipantsApartFromUs: Set<Party> = wellKnownParticipantsAndIssuers - flowIdentity.ourIdentity
        // We need participantSessions for all participants apart from us.
        requireSessionsForParticipants(wellKnownParticipantsApartFromUs, allSessions)
        val finalSessions = allSessions.filter { it.counterparty != flowIdentity.ourIdentity }
        // Notify all session counterparties of their role. Observers store the transaction using
        // StatesToRecord.ALL_VISIBLE, participants store the transaction using StatesToRecord.ONLY_RELEVANT.
        finalSessions.forEach { session ->
            if (session.counterparty in wellKnownParticipantsAndIssuers) session.send(TransactionRole.PARTICIPANT)
            else session.send(TransactionRole.OBSERVER)
        }
        // Sign and finalise the transaction, obtaining the signing keys required from the LedgerTransaction.
        val ourSigningKeys = ledgerTransaction.ourSigningKeys(keyManagementService)
        val stx = transactionBuilder?.let {
            it.sign(signingPubKeys = ourSigningKeys)
        } ?: signedTransaction
        ?: throw IllegalArgumentException("Didn't provide transactionBuilder nor signedTransaction to the flow.")
        return flowEngine.subFlow(FinalityFlow(transaction = stx, sessions = finalSessions))
    }
}