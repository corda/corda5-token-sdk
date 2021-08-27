package com.r3.corda.lib.tokens.diamondDemo.flows

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemNonFungibleTokens
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
class RedeemEvolvableDiamondTokenFlow
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
        val redeemFrom = CordaX500Name.parse(parameters.getMandatoryParameter("redeemFrom"))
        val redeemFromParty = identityService.partyFromName(redeemFrom)
            ?: throw BadRpcStartFlowRequestException("Could not find party for CordaX500Name: $redeemFrom")

        val results: List<StateAndRef<NonFungibleToken>> =
            persistenceService.getUnconsumedLinearStates(nftLinearId.id, 1)
        val nft = results.single().state.data
        val stx = flowEngine.subFlow(RedeemNonFungibleTokens(nft.token.tokenType, redeemFromParty))

        return SignedTransactionDigest(
            stx.id,
            emptyList(),
            stx.sigs
        )
    }
}