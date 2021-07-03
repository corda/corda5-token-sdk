package com.r3.corda.lib.tokens.sample.flows.fixedFungible

import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.JsonConstructor
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.injection.CordaInject
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

	@Suspendable
	override fun call() {
		val params: Map<String, String> = jsonMarshallingService.parseJson(inputJson.parametersInJson)
		val amount = params["amount"]!!.toDouble()

		val fungibleToken = FungibleTokenBuilder()
			.ofTokenType(EUR)
			.issuedBy(flowIdentity.ourIdentity)
			.heldBy(flowIdentity.ourIdentity)
			.withAmount(amount)
			.buildFungibleToken()

		flowEngine.subFlow(IssueTokensFlow(fungibleToken))
	}
}