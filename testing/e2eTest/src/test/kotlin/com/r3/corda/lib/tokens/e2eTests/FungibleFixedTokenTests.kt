package com.r3.corda.lib.tokens.e2eTests

import com.r3.corda.lib.tokens.sample.flows.fixedFungible.IssueEuroFlow
import com.r3.corda.lib.tokens.sample.flows.fixedFungible.MoveEuroFlow
import com.r3.corda.lib.tokens.testing.states.House
import net.corda.client.rpc.flow.FlowStarterRPCOps
import net.corda.test.dev.network.Node
import net.corda.test.dev.network.httpRpcClient
import net.corda.test.dev.network.withFlow
import net.corda.v5.application.identity.CordaX500Name
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * This test class is used to verify the confidential identities flows can run successfully by
 * calling them via sample flows.
 */
@Disabled("Disabled so that it is not run as part of the build since it depends on a running e2e test network")
class FungibleFixedTokenTests {

	companion object {
		@JvmStatic
		@BeforeAll
		fun verifySetup() {
			e2eTestNetwork.verify {
				listOf("alice", "bob", "caroline")
					.map { hasNode(it) }
					.forEach {
						it.withFlow<IssueEuroFlow>()
						it.withFlow<MoveEuroFlow>()
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
			var aliceEuro = 400000.0
			// Alice issues the euro token for testing
			alice().issueEuroToken(aliceEuro)

			var bobEuro = aliceEuro / 2
			aliceEuro /= 2

			// Transfer half of alice's euro to bob
			alice().moveEuroToken(
				bobEuro,
				bob().getX500Name()
			)

			val carolineEuro = bobEuro / 2
			bobEuro /= 2
			// Transfer half of bob's euro to caroline
			bob().moveEuroToken(
				carolineEuro,
				caroline().getX500Name()
			)
		}
	}

	private fun Node.issueEuroToken(
		amount: Double
	) {
		httpRpcClient<FlowStarterRPCOps, Unit> {
			getFlowOutcome(
				runFlow(
					IssueEuroFlow::class,
					mapOf(
						"amount" to amount.toString(),
					)
				)
			)
		}
	}

	private fun Node.moveEuroToken(
		amount: Double,
		recipient: CordaX500Name,
	) {
		httpRpcClient<FlowStarterRPCOps, Unit> {
			getFlowOutcome(
				runFlow(
					MoveEuroFlow::class,
					mapOf(
						"amount" to amount.toString(),
						"recipient" to recipient.toString()
					)
				)
			)
		}
	}
}
