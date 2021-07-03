package com.r3.corda.lib.tokens.sample.flows.fixedFungible

import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.move.MoveFungibleTokensFlow
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import net.corda.v5.application.flows.*
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.base.annotations.Suspendable

@StartableByRPC
@InitiatingFlow
class MoveEuroFlow @JsonConstructor constructor(
	val inputJson: RpcStartFlowRequestParameters
) : Flow<Unit> {

	@CordaInject
	lateinit var jsonMarshallingService: JsonMarshallingService

	@CordaInject
	lateinit var identityService: IdentityService

	@CordaInject
	lateinit var flowEngine: FlowEngine

	@CordaInject
	lateinit var flowMessaging: FlowMessaging

	@Suspendable
	override fun call() {
		val params: Map<String, String> = jsonMarshallingService.parseJson(inputJson.parametersInJson)
		val amount = params["amount"]!!.toDouble()
		val recipient = CordaX500Name.parse(params["recipient"]!!)
		val recipientParty = identityService.partyFromName(recipient)!!
		val recipientSession = flowMessaging.initiateFlow(recipientParty)

		flowEngine.subFlow(
			MoveFungibleTokensFlow(
				PartyAndAmount(
					recipientParty,
					EUR(amount)
				),
				listOf(recipientSession)
			)
		)
	}
}