package com.r3.corda.lib.tokens.e2eTests

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.r3.corda.lib.tokens.testing.states.House
import com.r3.corda.lib.tokens.sample.flows.CreateHouseToken
import com.r3.corda.lib.tokens.sample.flows.GetHouseInfoFlow
import com.r3.corda.lib.tokens.sample.flows.MoveHouseTokenFlow
import com.r3.corda.lib.tokens.sample.flows.UpdateHouseValuation
import net.corda.client.rpc.flow.FlowStarterRPCOps
import net.corda.client.rpc.flow.RpcFlowOutcomeResponse
import net.corda.client.rpc.flow.RpcFlowStatus
import net.corda.client.rpc.flow.RpcStartFlowRequest
import net.corda.client.rpc.flow.RpcStartFlowResponse
import net.corda.client.rpc.identity.NodeIdentityRPCOps
import net.corda.test.dev.network.Node
import net.corda.test.dev.network.httpRpcClient
import net.corda.test.dev.network.withFlow
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.identity.CordaX500Name
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.reflect.KClass

/**
 * This test class is used to verify the confidential identities flows can run successfully by
 * calling them via sample flows.
 */
@Disabled("Disabled so that it is not run as part of the build since it depends on a running e2e test network")
class SampleFlowTests {

    companion object {
        @JvmStatic
        @BeforeAll
        fun verifySetup() {
            e2eTestNetwork.verify {
                listOf("alice", "bob", "caroline")
                    .map { hasNode(it) }
                    .forEach {
                        it.withFlow<CreateHouseToken>()
                    }
            }
        }
    }

    @Test
    fun runSampleFlowsForEvolvableToken() {
        e2eTestNetwork.use {
            /**
             * Issue [House] state
             */
            val initialHouseValue = 400000.0
            val currencyCode = "EUR"
            val address = "1 Fake Street"

            // Alice issues a state to be exchanged
            val alice = alice()
            val (nftLinearId, houseLinearId) = alice.createHouseToken(address, currencyCode, initialHouseValue)
            alice.assertHouseStateProperties(houseLinearId, address, initialHouseValue, currencyCode)

            val houseValueIncrement = 50000.0
            val newHouseValuation = initialHouseValue + houseValueIncrement

            alice.updateHouseValuation(houseLinearId, newHouseValuation, currencyCode)
            alice.assertHouseStateProperties(houseLinearId, address, newHouseValuation, currencyCode)

            assertThat(nftLinearId).isNotBlank
            val bob = bob()
            val bobX500Name = bob.getX500Name()

            alice.moveHouseToken(nftLinearId, bobX500Name)
            alice.assertHouseStateProperties(houseLinearId, address, newHouseValuation, currencyCode)
            bob.assertHouseStateProperties(houseLinearId, address, newHouseValuation, currencyCode)
        }
    }

    private fun Node.createHouseToken(
        address: String,
        currencyCode: String,
        initialHouseValue: Double
    ): Pair<String, String> {
        return httpRpcClient<FlowStarterRPCOps, Pair<String, String>> {
            val result = getFlowOutcome(
                runFlow(
                    CreateHouseToken::class,
                    mapOf(
                        "address" to address,
                        "currencyCode" to currencyCode,
                        "value" to initialHouseValue.toString(),
                    )
                )
            )

            val resultJson = JsonParser.parseString(result.resultJson).asJsonObject
            val nftLinearId = resultJson["linearId"].asString
            assertThat(nftLinearId).isNotBlank

            val houseTokenLinearId = resultJson["token"].asJsonObject["linearId"].asString
            assertThat(houseTokenLinearId).isNotBlank

            nftLinearId to houseTokenLinearId
        }
    }

    private fun Node.updateHouseValuation(linearId: String, newValuation: Double, currencyCode: String) {
        httpRpcClient<FlowStarterRPCOps, Unit> {
            getFlowOutcome(
                runFlow(
                    UpdateHouseValuation::class,
                    mapOf(
                        "linearId" to linearId,
                        "newValuation" to newValuation.toString(),
                        "currencyCode" to currencyCode,
                    )
                )
            )
        }
    }

    private fun Node.assertHouseStateProperties(
        linearId: String,
        address: String,
        value: Double,
        valueCurrency: String
    ) {
        httpRpcClient<FlowStarterRPCOps, Unit> {
            val result = getFlowOutcome(
                runFlow(
                    GetHouseInfoFlow::class,
                    mapOf("linearId" to linearId)
                )
            )

            val resultJson = JsonParser.parseString(result.resultJson).asJsonObject
            assertThat(resultJson["linearId"].asString).isEqualTo(linearId)
            assertThat(resultJson["address"].asString).isEqualTo(address)
            assertThat(resultJson["valuation"].asJsonObject["amount"].asDouble).isEqualTo(value)
            assertThat(resultJson["valuation"].asJsonObject["type"].asString).isEqualTo(valueCurrency)
        }
    }

    private fun Node.getX500Name(): CordaX500Name =
        httpRpcClient<NodeIdentityRPCOps, CordaX500Name> { getMyMemberInfo().x500Name }

    private fun Node.moveHouseToken(linearId: String, recipient: CordaX500Name) {
        httpRpcClient<FlowStarterRPCOps, Unit> {
            getFlowOutcome(
                runFlow(
                    MoveHouseTokenFlow::class,
                    mapOf(
                        "linearId" to linearId,
                        "recipient" to recipient.toString()
                    )
                )
            )

        }
    }

    private fun FlowStarterRPCOps.runFlow(flowClass: KClass<*>, parameters: Map<Any, Any>): RpcStartFlowResponse {
        val clientId = "client-${UUID.randomUUID()}"
        val parametersInJson = GsonBuilder().create().toJson(parameters)

        val response = startFlow(
            RpcStartFlowRequest(
                flowClass.java.name,
                clientId,
                RpcStartFlowRequestParameters(parametersInJson)
            )
        )

        assertThat(response.clientId).isEqualTo(clientId)
        assertThat(response.stateMachineRunId).isNotNull

        return response
    }

    private fun FlowStarterRPCOps.getFlowOutcome(response: RpcStartFlowResponse): RpcFlowOutcomeResponse {
        var result: RpcFlowOutcomeResponse
        do {
            result = getFlowOutcome(response.stateMachineRunId.uuid.toString())
        } while (result.status == RpcFlowStatus.RUNNING)

        assertThat(RpcFlowStatus.COMPLETED).isEqualTo(result.status)

        return result
    }
}
