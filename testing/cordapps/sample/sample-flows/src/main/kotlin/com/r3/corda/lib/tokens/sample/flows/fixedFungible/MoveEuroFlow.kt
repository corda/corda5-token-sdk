package com.r3.corda.lib.tokens.sample.flows.fixedFungible

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.money.EUR
import com.r3.corda.lib.tokens.workflows.flows.rpc.ConfidentialMoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
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
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.contracts.ContractState
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.services.vault.StateAndRefPostProcessor
import java.util.stream.Stream

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

        val postProcessor = params["filterOnlyIssuedByBob"]?.let {
            if (it.toBoolean()) FilterTokensByIssuerBobPostProcessor.POST_PROCESSOR_NAME else null
        }

        if (confidential) {
            flowEngine.subFlow(
                ConfidentialMoveFungibleTokens(
                    PartyAndAmount(
                        recipientParty,
                        EUR(amount)
                    ),
                    emptyList(),
                    postProcessor
                )
            )
        } else {
            flowEngine.subFlow(
                MoveFungibleTokens(
                    PartyAndAmount(
                        recipientParty,
                        EUR(amount)
                    ),
                    postProcessor
                )
            )
        }
    }
}

class FilterTokensByIssuerBobPostProcessor : StateAndRefPostProcessor<StateAndRef<ContractState>> {
    companion object {
        const val POST_PROCESSOR_NAME = "MoveEuroFlow.FilterByIssuerBob"
        val logger = contextLogger()
    }

    override val name = POST_PROCESSOR_NAME

    override fun postProcess(inputs: Stream<StateAndRef<ContractState>>): Stream<StateAndRef<ContractState>> {
        return inputs.filter {
            it.state.data is FungibleToken
        }.filter {
            (it.state.data as FungibleToken).issuer.name.toString().contains("bob")
        }
    }
}