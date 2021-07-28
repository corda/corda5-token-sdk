package com.r3.corda.lib.tokens.workflows.flows.redeem

import com.r3.corda.lib.ci.workflows.SyncKeyMappingFlowHandler
import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.workflows.internal.checkOwner
import com.r3.corda.lib.tokens.workflows.internal.checkSameIssuer
import com.r3.corda.lib.tokens.workflows.internal.checkSameNotary
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.TransactionRole
import net.corda.systemflows.SignTransactionFlow
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.flows.receive
import net.corda.v5.application.flows.unwrap
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.MemberLookupService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.transactions.SignedTransaction
import net.corda.v5.ledger.transactions.inRefsOfType

/**
 * Inlined responder flow called on the issuer side, should be used with: [RedeemFungibleTokensFlow],
 * [RedeemNonFungibleTokensFlow], [RedeemTokensFlow].
 */
// Called on Issuer side.
class RedeemTokensFlowHandler(val otherSession: FlowSession) : Flow<SignedTransaction?> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var memberLookupService: MemberLookupService

    @Suspendable
    override fun call(): SignedTransaction? {
        val role = otherSession.receive<TransactionRole>().unwrap { it }
        if (role == TransactionRole.PARTICIPANT) {
            // Synchronise all confidential identities, issuer isn't involved in move transactions, so states holders may
            // not be known to this node.
            flowEngine.subFlow(SyncKeyMappingFlowHandler(otherSession))
            // There is edge case where issuer redeems with themselves, then we need to be careful not to call handler for
            // collect signatures for already fully signed transaction - it causes session messages mismatch.
            if (otherSession.counterparty.owningKey !in memberLookupService.myInfo().identityKeys) {
                // Perform all the checks to sign the transaction.
                flowEngine.subFlow(object : SignTransactionFlow(otherSession) {
                    @CordaInject
                    lateinit var flowIdentity: FlowIdentity

                    // TODO if it is with itself, then we won't perform that check...
                    override fun checkTransaction(stx: SignedTransaction) {
                        val stateAndRefsToRedeem =
                            transactionMappingService.toLedgerTransaction(stx, false)
                                .inRefsOfType<AbstractToken>()
                        checkSameIssuer(stateAndRefsToRedeem, flowIdentity.ourIdentity)
                        checkSameNotary(stateAndRefsToRedeem)
                        checkOwner(identityService, stateAndRefsToRedeem, otherSession.counterparty)
                    }
                })
            }
        }
        return if (otherSession.counterparty.owningKey !in memberLookupService.myInfo().identityKeys) {
            // Call observer aware finality flow handler.
            flowEngine.subFlow(ObserverAwareFinalityFlowHandler(otherSession))
        } else null
    }
}
