package com.r3.corda.lib.tokens.e2eTests

import com.google.gson.JsonParser
import com.r3.corda.lib.tokens.sample.flows.evolvableNft.*
import com.r3.corda.lib.tokens.testing.states.House
import net.corda.client.rpc.flow.FlowStarterRPCOps
import net.corda.test.dev.network.Node
import net.corda.test.dev.network.httpRpcClient
import net.corda.test.dev.network.withFlow
import net.corda.v5.application.identity.CordaX500Name
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * This test class is used to verify the confidential identities flows can run successfully by
 * calling them via sample flows.
 */
@Disabled("Disabled so that it is not run as part of the build since it depends on a running e2e test network")
class NonFungibleEvolvableTokenTests {

	companion object {
		@JvmStatic
		@BeforeAll
		fun verifySetup() {
			e2eTestNetwork.verify {
				listOf("alice", "bob", "caroline")
					.map { hasNode(it) }
					.forEach {
						it.withFlow<CreateHouseToken>()
						it.withFlow<GetHouseInfoFlow>()
						it.withFlow<MoveHouseTokenFlow>()
						it.withFlow<RedeemHouseTokenFlow>()
						it.withFlow<UpdateHouseValuation>()
					}
			}
		}
	}

	@Test
	fun runSampleFlowsForEvolvableNonFungibleToken() {
		e2eTestNetwork.use {
			/**
			 * Issue [House] state
			 */
			var houseValue = 400000.0
			val currencyCode = "EUR"
			val address = "1 Fake Street"

			// Alice issues a state to be exchanged
			val (nftLinearId, houseLinearId) = alice().createHouseToken(address, currencyCode, houseValue)
			alice().assertHouseStateProperties(houseLinearId, address, houseValue, currencyCode)

			houseValue += 50000.0

			alice().updateHouseValuation(houseLinearId, houseValue, currencyCode)
			alice().assertHouseStateProperties(houseLinearId, address, houseValue, currencyCode)

			alice().moveHouseToken(nftLinearId, bob().getX500Name())
			alice().assertHouseStateProperties(houseLinearId, address, houseValue, currencyCode)
			bob().assertHouseStateProperties(houseLinearId, address, houseValue, currencyCode)

			bob().redeemHouseToken(nftLinearId)
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

	private fun Node.redeemHouseToken(linearId: String) {
		httpRpcClient<FlowStarterRPCOps, Unit> {
			getFlowOutcome(
				runFlow(
					RedeemHouseTokenFlow::class,
					mapOf(
						"linearId" to linearId,
					)
				)
			)

		}
	}
}
