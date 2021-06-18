package com.r3.corda.lib.tokens.e2etests

import com.google.gson.GsonBuilder
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.testing.states.House
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import com.r3.corda.lib.tokens.sample.flows.CreateHouseToken
import com.r3.corda.lib.tokens.sample.flows.GetHouseInfoFlow
import com.r3.corda.lib.tokens.sample.flows.UpdateHouseValuation
import net.corda.test.dev.network.Credentials
import net.corda.test.dev.network.Nodes
import net.corda.test.dev.network.withFlow
import net.corda.v5.ledger.transactions.SignedTransaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*

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
    fun runSampleFlowsForEvolvableToken() = e2eTestNetwork.use {
        /**
         * Issue [House] state
         */
        val initialHouseValue = 400000L
        val currencyCode = "EUR"
        val address = "1 Fake Street"

        // Alice issues a state to be exchanged
        //var (stx, aliceId) =
        alice().httpRpc(Credentials.DEFAULT_CREDENTIALS) {
            val clientId = "client-${UUID.randomUUID()}"
            val parametersInJson = GsonBuilder()
                .create()
                .toJson(
                    mapOf(
                        "address" to address,
                        "currencyCode" to currencyCode,
                        "initialHouseValue" to initialHouseValue.toString(),
                    )
                )
            val body = mapOf(
                "rpcStartFlowRequest" to
                        mapOf(
                            "flowName" to "net.corda.sample.flows.CreateHouseToken",
                            "clientId" to clientId,
                            "parameters" to mapOf(
                                "parametersInJson" to parametersInJson
                            )
                        )
            )

            val request = post("flowstarter/startflow")
                .header("Content-Type", "application/json")
                .body(body)

            val response = request.asJson()

            assertThat(response.status).isEqualTo(200)
            assertThat(response.body.`object`.get("clientId")).isEqualTo(clientId)
            assertThat(response.body.`object`.get("stateMachineRunId")).isNotNull

            /*startFlowDynamic(CreateHouseToken::class.java, address, currencyCode, initialHouseValue)
                .returnValue.getOrThrow() to nodeInfo().legalIdentities.first()*/
        }

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
