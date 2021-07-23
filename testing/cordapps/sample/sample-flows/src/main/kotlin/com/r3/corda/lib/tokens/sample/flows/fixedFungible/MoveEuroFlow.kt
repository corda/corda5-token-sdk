package com.r3.corda.lib.tokens.sample.flows.fixedFungible

import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.rpc.ConfidentialMoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import net.corda.v5.application.flows.*
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.base.annotations.Suspendable

@StartableByRPC
class MoveEuroFlow @JsonConstructor constructor(
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
		val recipient = CordaX500Name.parse(params["recipient"]!!)
		val recipientParty = identityService.partyFromName(recipient)!!

		if(confidential) {
			flowEngine.subFlow(
				ConfidentialMoveFungibleTokens(
					PartyAndAmount(
						recipientParty,
						EUR(amount)
					),
					emptyList()
				)
			)
		} else {
			flowEngine.subFlow(
				MoveFungibleTokens(
					PartyAndAmount(
						recipientParty,
						EUR(amount)
					)
				)
			)
		}
	}
}