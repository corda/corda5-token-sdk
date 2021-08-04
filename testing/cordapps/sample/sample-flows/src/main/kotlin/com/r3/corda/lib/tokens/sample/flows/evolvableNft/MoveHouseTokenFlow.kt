package com.r3.corda.lib.tokens.sample.flows.evolvableNft

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
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
import net.corda.v5.base.util.seconds
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.services.vault.IdentityStateAndRefPostProcessor
import net.corda.v5.ledger.services.vault.StateStatus
import java.util.*

@StartableByRPC
class MoveHouseTokenFlow @JsonConstructor constructor(
    val inputParams: RpcStartFlowRequestParameters
) : Flow<Unit> {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var identityService: IdentityService

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @Suspendable
    override fun call() {
        val inputJson = jsonMarshallingService.parseJson<Map<String, String>>(inputParams.parametersInJson)
        val linearId = inputJson["linearId"]!!
        val recipient = CordaX500Name.parse(inputJson["recipient"]!!)

        val cursor = persistenceService.query<StateAndRef<NonFungibleToken>>(
            "LinearState.findByUuidAndStateStatus",
            mapOf(
                "uuid" to UUID.fromString(linearId),
                "stateStatus" to StateStatus.UNCONSUMED,
            ),
            IdentityStateAndRefPostProcessor.POST_PROCESSOR_NAME,
        )

        val results = mutableListOf<StateAndRef<NonFungibleToken>>()
        do {
            val pollResult = cursor.poll(1, 5.seconds)
            results.addAll(pollResult.values)
        } while (!pollResult.isLastResult)

        require(results.size == 1)

        val nft = results.single()

        val partyAndToken = PartyAndToken(
            identityService.partyFromName(recipient)!!,
            nft.state.data.token.tokenType
        )
        flowEngine.subFlow(MoveNonFungibleTokens(partyAndToken))
    }
}