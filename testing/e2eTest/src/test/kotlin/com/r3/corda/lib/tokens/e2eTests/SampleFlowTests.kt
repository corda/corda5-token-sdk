package com.r3.corda.lib.tokens.e2eTests

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.r3.corda.lib.tokens.testing.states.House
import com.r3.corda.lib.tokens.sample.flows.CreateHouseToken
import net.corda.client.rpc.flow.FlowStarterRPCOps
import net.corda.client.rpc.flow.RpcFlowOutcomeResponse
import net.corda.client.rpc.flow.RpcFlowStatus
import net.corda.client.rpc.flow.RpcStartFlowRequest
import net.corda.client.rpc.flow.RpcStartFlowResponse
import net.corda.test.dev.network.httpRpcClient
import net.corda.test.dev.network.withFlow
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*

/**
 * This test class is used to verify the confidential identities flows can run successfully by
 * calling them via sample flows.
 */
//@Disabled("Disabled so that it is not run as part of the build since it depends on a running e2e test network")
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
            val initialHouseValue = 400000L
            val currencyCode = "EUR"
            val address = "1 Fake Street"

            // Alice issues a state to be exchanged
            //var (stx, aliceId) =
            val houseId = alice().httpRpcClient<FlowStarterRPCOps, String> {
                val clientId = "client-${UUID.randomUUID()}"
                val parametersInJson = GsonBuilder()
                        .create()
                        .toJson(
                                mapOf(
                                        "address" to address,
                                        "currencyCode" to currencyCode,
                                        "value" to initialHouseValue.toString(),
                                )
                        )


                val response: RpcStartFlowResponse = startFlow(
                        RpcStartFlowRequest(
                                CreateHouseToken::class.java.name,
                                clientId,
                                RpcStartFlowRequestParameters(parametersInJson)
                        )
                )

                assertThat(response.clientId).isEqualTo(clientId)
                assertThat(response.stateMachineRunId).isNotNull


                var result: RpcFlowOutcomeResponse
                do {
                    result = getFlowOutcome(response.stateMachineRunId.uuid.toString())
                } while (result.status == RpcFlowStatus.RUNNING)

                assertThat(RpcFlowStatus.COMPLETED).isEqualTo(result.status)

                JsonParser.parseString(result.resultJson).asJsonObject["linearId"].asString
            }

            assertThat(houseId).isNotBlank
//        val linearId = stx.getNFT().linearId.toString()
//        assertHouseProperties(linearId, address, initialHouseValue, currencyCode)
//
//        val valuationIncrement = 50000L
//        stx = alice().httpRpc {
//            startFlowDynamic(
//                UpdateHouseValuation::class.java,
//                currencyCode,
//                initialHouseValue + valuationIncrement,
//                stx.getNFT().linearId
//            ).returnValue.getOrThrow()
//        }
//
//        assertHouseProperties(linearId, address, initialHouseValue + valuationIncrement, currencyCode)
//
//        val bobId = bob().httpRpc { nodeInfo().legalIdentities }.single()
//        assertThat(stx.getNFT().issuer).isEqualTo(aliceId)
//        assertThat(stx.getNFT().holder).isEqualTo(aliceId)
//
//        stx = alice().httpRpc {
//            startFlowDynamic(
//                MoveNonFungibleTokens::class.java,
//                PartyAndToken(bobId, stx.getNFT().tokenType)
//            ).returnValue.getOrThrow()
//        }
//
//        assertHouseProperties(linearId, address, initialHouseValue + valuationIncrement, currencyCode)
//
//        assertThat(stx.getNFT().issuer).isEqualTo(aliceId)
//        assertThat(stx.getNFT().holder).isEqualTo(bobId)
        }
    }

//    private fun SignedTransaction.getNFT() = tx.outputStates.single() as NonFungibleToken

//    private fun Nodes<*>.assertHouseProperties(linearId: String, address: String, value: Long, valueCurrency: String) {
//        val house = alice().httpRpc {
//            startFlowDynamic(GetHouseInfoFlow::class.java, linearId)
//                .returnValue.getOrThrow()
//        }
//        assertThat(house.address).isEqualTo(address)
//        assertThat(house.valuation.quantity).isEqualTo(value)
//        assertThat(house.valuation.token.tokenIdentifier).isEqualTo(valueCurrency)
//    }
}
