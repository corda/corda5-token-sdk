package com.r3.corda.lib.tokens.e2etests

import com.r3.corda.lib.ci.e2etests.alice
import com.r3.corda.lib.ci.e2etests.bob
import com.r3.corda.lib.ci.e2etests.e2eTestNetwork
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.testing.states.House
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import net.corda.sample.flows.CreateHouseToken
import net.corda.sample.flows.GetHouseInfoFlow
import net.corda.sample.flows.UpdateHouseValuation
import net.corda.test.dev.network.Nodes
import net.corda.test.dev.network.withFlow
import net.corda.v5.base.internal.uncheckedCast
import net.corda.v5.ledger.contracts.LinearPointer
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
                listOf("alice")//, "bob", "caroline")
                    .map { hasNode(it) }
                    .forEach {
                        it.withFlow<CreateHouseToken>()
                    }
            }
        }
    }

    @Test
    fun runSampleFlowsForEvolvableToken() = e2eTestNetwork.use {
//        /**
//         * Issue [House] state
//         */
//        val initialHouseValue = 400000L
//        val currencyCode = "EUR"
//        val address = "1 Fake Street"
//
//        // Alice issues a state to be exchanged
//        var (stx, aliceId) = alice().httpRpc {
//            startFlowDynamic(CreateHouseToken::class.java, address, currencyCode, initialHouseValue)
//                .returnValue.getOrThrow() to nodeInfo().legalIdentities.first()
//        }
//
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
//
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
