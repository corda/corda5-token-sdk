package com.r3.corda.lib.tokens.sample.flows.fixedFungible

import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.rpc.ConfidentialRedeemFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemFungibleTokens
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
import net.corda.v5.base.annotations.Suspendable

@StartableByRPC
class RedeemEuroFlow @JsonConstructor constructor(
    val inputJson: RpcStartFlowRequestParameters
) : Flow<Unit> {

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var identityService: IdentityService

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() {
        val params: Map<String, String> = jsonMarshallingService.parseJson(inputJson.parametersInJson)
        val amount = params["amount"]!!.toDouble()
        val confidential = params["confidential"]!!.toBoolean()
        val issuer = CordaX500Name.parse(params["issuer"]!!)
        val issuerParty = identityService.partyFromName(issuer)!!

        if (confidential) {
            flowEngine.subFlow(
                ConfidentialRedeemFungibleTokens(
                    EUR(amount),
                    issuerParty
                )
            )
        } else {
            flowEngine.subFlow(
                RedeemFungibleTokens(
                    EUR(amount),
                    issuerParty
                )
            )
        }
    }
}
