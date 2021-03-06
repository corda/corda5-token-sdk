package com.r3.corda.lib.tokens.diamondDemo.flows

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.workflows.flows.rpc.ConfidentialMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import net.corda.v5.application.flows.BadRpcStartFlowRequestException
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.JsonConstructor
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.transactions.SignedTransactionDigest

@StartableByRPC
class MoveNonFungibleDiamondTokenFlow
@JsonConstructor constructor(
    val params: RpcStartFlowRequestParameters
) : Flow<SignedTransactionDigest> {

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @CordaInject
    lateinit var identityService: IdentityService

    @Suspendable
    override fun call(): SignedTransactionDigest {
        val parameters = jsonMarshallingService.parseParameters(params)

        val nftLinearId = parameters.getMandatoryUUID("nftLinearId")
        val moveTo = parameters.getMandatoryPartyFromName(identityService, "moveTo")
        val anonymous = parameters.getMandatoryBoolean("anonymous")

        val nft = persistenceService.getUnconsumedLinearStateData<NonFungibleToken>(nftLinearId)
        val partyAndToken = PartyAndToken(moveTo, nft.token.tokenType)
        val flow = if (anonymous) {
            ConfidentialMoveNonFungibleTokens(partyAndToken, emptyList())
        } else {
            MoveNonFungibleTokens(partyAndToken)
        }
        val stx = flowEngine.subFlow(flow)

        return SignedTransactionDigest(
            stx.id,
            listOf(nft.linearId.toString()),
            stx.sigs
        )
    }
}