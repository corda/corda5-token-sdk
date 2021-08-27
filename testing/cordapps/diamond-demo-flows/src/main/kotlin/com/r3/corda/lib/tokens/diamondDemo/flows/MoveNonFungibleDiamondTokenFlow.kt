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
        val parameters: Map<String, String> = jsonMarshallingService.parseJson(params.parametersInJson)

        val nftLinearId = UniqueIdentifier.fromString(parameters.getMandatoryParameter("nftLinearId"))
        val moveTo = CordaX500Name.parse(parameters.getMandatoryParameter("moveTo"))
        val moveToParty = identityService.partyFromName(moveTo)
            ?: throw BadRpcStartFlowRequestException("Could not find party for CordaX500Name: $moveTo")
        val anonymous = parameters.getMandatoryParameter("anonymous").toBoolean()

        val results: List<StateAndRef<NonFungibleToken>> =
            persistenceService.getUnconsumedLinearStates(nftLinearId.id, expectedSize = 1)

        val nft = results.single().state.data

        val partyAndToken = PartyAndToken(
            moveToParty,
            nft.token.tokenType
        )

        val stx = if (anonymous) {
            flowEngine.subFlow(ConfidentialMoveNonFungibleTokens(partyAndToken, emptyList()))
        } else {
            flowEngine.subFlow(MoveNonFungibleTokens(partyAndToken))
        }


        return SignedTransactionDigest(
            stx.id,
            listOf(nft.linearId.toString()),
            stx.sigs
        )
    }
}