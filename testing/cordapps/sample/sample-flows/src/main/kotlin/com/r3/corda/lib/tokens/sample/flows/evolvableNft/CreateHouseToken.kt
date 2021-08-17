package com.r3.corda.lib.tokens.sample.flows.evolvableNft

import com.r3.corda.lib.tokens.builder.NonFungibleTokenBuilder
import com.r3.corda.lib.tokens.contracts.utilities.amount
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.sample.states.HouseToken
import com.r3.corda.lib.tokens.sample.states.JsonRepresentableHouseNFT
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.JsonConstructor
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.TransactionState
import net.corda.v5.ledger.services.NotaryLookupService

@StartableByRPC
class CreateHouseToken @JsonConstructor constructor(
    val inputParams: RpcStartFlowRequestParameters
) : Flow<JsonRepresentableHouseNFT> {

    @CordaInject
    lateinit var notaryLookupService: NotaryLookupService

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var hashingService: HashingService

    @Suspendable
    override fun call(): JsonRepresentableHouseNFT {
        val params: Map<String, String> = jsonMarshallingService.parseJson(inputParams.parametersInJson)
        val address: String = params["address"]!!
        val currencyCode: String = params["currencyCode"]!!
        val value: Double = params["value"]!!.toDouble()

        val notary = notaryLookupService.notaryIdentities.first()

        val house = HouseToken(
            address,
            amount(value, FiatCurrency.getInstance(currencyCode)),
            listOf(flowIdentity.ourIdentity),
            linearId = UniqueIdentifier()
        )
        val transactionState = TransactionState(house, notary = notary)
        flowEngine.subFlow(CreateEvolvableTokens(transactionState))

        val houseToken = NonFungibleTokenBuilder()
            .ofTokenType(house.toPointer<HouseToken>())
            .issuedBy(flowIdentity.ourIdentity)
            .heldBy(flowIdentity.ourIdentity)
            .buildNonFungibleToken(hashingService)

        flowEngine.subFlow(IssueTokens(listOf(houseToken)))

        return JsonRepresentableHouseNFT(houseToken)
    }
}
