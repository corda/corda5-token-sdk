package com.r3.corda.lib.tokens.sample.flows.fixedFungible

import com.r3.corda.lib.tokens.builder.FungibleTokenBuilder
import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.rpc.ConfidentialIssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.JsonConstructor
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.base.annotations.Suspendable

@StartableByRPC
class IssueEuroFlow @JsonConstructor constructor(
    val inputJson: RpcStartFlowRequestParameters
) : Flow<Unit> {

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var hashingService: HashingService

    @CordaInject
    lateinit var identityService: IdentityService

    @Suspendable
    override fun call() {
        val params: Map<String, String> = jsonMarshallingService.parseJson(inputJson.parametersInJson)
        val amount = params["amount"]!!.toDouble()
        val recipient = params["recipient"]
        val confidential = params["confidential"]!!.toBoolean()

        val party = recipient?.let {
            identityService.partyFromName(CordaX500Name.parse(recipient))
        } ?: flowIdentity.ourIdentity

        val fungibleToken = FungibleTokenBuilder()
            .ofTokenType(EUR)
            .issuedBy(flowIdentity.ourIdentity)
            .heldBy(party)
            .withAmount(amount)
            .buildFungibleToken(hashingService)

        if (confidential && recipient != null) {
            flowEngine.subFlow(
                ConfidentialIssueTokens(listOf(fungibleToken), listOf(party))
            )
        } else if (confidential) {
            flowEngine.subFlow(ConfidentialIssueTokens(listOf(fungibleToken)))
        } else if (recipient != null) {
            flowEngine.subFlow(IssueTokens(listOf(fungibleToken), listOf(party)))
        } else {
            flowEngine.subFlow(IssueTokens(listOf(fungibleToken)))
        }
    }
}