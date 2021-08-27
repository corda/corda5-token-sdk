package com.r3.corda.lib.tokens.demo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.r3.corda.lib.tokens.diamondDemo.flows.CreateEvolvableDiamondTokenFlow
import com.r3.corda.lib.tokens.diamondDemo.flows.GetDiamondReportFlow
import com.r3.corda.lib.tokens.diamondDemo.flows.HasTransactionFlow
import com.r3.corda.lib.tokens.diamondDemo.flows.HasUnconsumedNonFungibleTokenFlow
import com.r3.corda.lib.tokens.diamondDemo.flows.IssueNonFungibleDiamondTokenFlow
import com.r3.corda.lib.tokens.diamondDemo.flows.MoveNonFungibleDiamondTokenFlow
import com.r3.corda.lib.tokens.diamondDemo.flows.RedeemEvolvableDiamondTokenFlow
import com.r3.corda.lib.tokens.diamondDemo.flows.UpdateEvolvableDiamondTokenFlow
import com.r3.corda.lib.tokens.test.utils.getFlowOutcome
import com.r3.corda.lib.tokens.test.utils.runFlow
import com.r3.corda.lib.tokens.testing.states.DiamondGradingReport.ClarityScale
import com.r3.corda.lib.tokens.testing.states.DiamondGradingReport.ColorScale
import com.r3.corda.lib.tokens.testing.states.DiamondGradingReport.CutScale
import com.r3.corda.lib.tokens.testing.states.DiamondGradingReportDigest
import net.corda.client.rpc.flow.FlowStarterRPCOps
import net.corda.test.dev.network.Node
import net.corda.test.dev.network.httpRpcClient
import net.corda.test.dev.network.withFlow
import net.corda.test.dev.network.x500Name
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.ledger.transactions.SignedTransactionDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * This test suite is intended to test and demonstrate common scenarios for working with evolvable token types and
 * non-fungible (discrete) holdable tokensToIssue.
 */
class DiamondWithTokenScenarioTests {
    companion object {
        private val jsonMapper: ObjectMapper = ObjectMapper().apply { registerModule(KotlinModule()) }

        @JvmStatic
        @BeforeAll
        @Suppress("UNUSED")
        fun verifySetup() {
            diamondDemoNetwork.verify {
                listOf("alice", "bob", "caroline", "denise", "gic")
                    .map { hasNode(it) }
                    .forEach {
                        it.withFlow<CreateEvolvableDiamondTokenFlow>()
                            .withFlow<GetDiamondReportFlow>()
                            .withFlow<HasTransactionFlow>()
                            .withFlow<HasUnconsumedNonFungibleTokenFlow>()
                            .withFlow<IssueNonFungibleDiamondTokenFlow>()
                            .withFlow<MoveNonFungibleDiamondTokenFlow>()
                            .withFlow<RedeemEvolvableDiamondTokenFlow>()
                            .withFlow<UpdateEvolvableDiamondTokenFlow>()
                    }
            }
        }
    }

    /**
     * This scenario creates a new evolvable token type and issues holdable tokens. It is intended to demonstrate a
     * fairly typical use case for creating evolvable token types and for issuing discrete (non-fungible) holdable tokens.
     *
     * 1. GIC creates (publishes) the diamond grading report
     * 2. Denise (the diamond dealer) issues a holdable, discrete (non-fungible) token to Alice
     * 3. Alice transfers the discrete token to Bob
     * 4. Bob transfers the discrete token to Caroline
     * 5. GIC amends (updates) the grading report
     * 6. Caroline redeems the holdable token with Denise (perhaps Denise buys back the diamond and plans to issue a new
     *    holdable token as replacement)
     */
    @Test
    fun `lifecycle example`() {
        diamondDemoNetwork.use {
            val alice = alice()
            val bob = bob()
            val caroline = caroline()
            val denise = denise()
            val gic = gic()

            // STEP 01: GIC publishes the diamond certificate
            // GIC publishes and shares with Denise
            val publishedDiamondProperties = DiamondProperties(
                caratWeight = BigDecimal("1.0"),
                colorScale = ColorScale.A,
                clarityScale = ClarityScale.A,
                cutScale = CutScale.A,
            )

            val publishDiamondTx = gic.createEvolvableDiamondToken(
                publishedDiamondProperties,
                denise.x500Name
            )
            assertHasTransaction(publishDiamondTx, gic)
            assertHasTransaction(publishDiamondTx, denise)

            val publishedDiamond = publishDiamondTx.getSingleDiamondOutput()
            assertDiamondProperties(publishedDiamond, publishedDiamondProperties)

            // STEP 02: Denise creates ownership token
            // Denise issues the token to Alice
            val issueTokenTx = denise.issueNonFungibleTokens(
                tokenLinearId = publishedDiamond.linearId,
                issueTo = alice.x500Name,
                anonymous = true
            )
            // GIC should *not* receive a copy of this issuance
            assertHasTransaction(issueTokenTx, alice)
            assertNotHasTransaction(issueTokenTx, gic)

            // STEP 03: Alice transfers ownership to Bob
            // Continuing the chain of sale
            val moveTokenToBobTx = alice.moveNonFungibleTokens(
                nftLinearId = issueTokenTx.outputStates.single(),
                moveTo = bob.x500Name,
                anonymous = true
            )
            assertHasTransaction(moveTokenToBobTx, alice, bob)
            assertNotHasTransaction(moveTokenToBobTx, gic, denise)

            // STEP 04: Bob transfers ownership to Caroline
            // Continuing the chain of sale
            val moveTokenToCarolineTx = bob.moveNonFungibleTokens(
                nftLinearId = moveTokenToBobTx.outputStates.single(),
                moveTo = caroline.x500Name,
                anonymous = true
            )
            assertHasTransaction(moveTokenToCarolineTx, bob, caroline)
            assertNotHasTransaction(moveTokenToCarolineTx, gic, denise, alice)

            // STEP 05: GIC amends (updates) the grading report
            // This should be reflected to the report participants
            val updatedDiamondProperties = publishedDiamondProperties.copy(colorScale = ColorScale.B)
            val updateDiamondTx = gic.updateEvolvableToken(
                tokenLinearId = publishedDiamond.linearId,
                diamondProperties = updatedDiamondProperties
            )
            assertHasTransaction(updateDiamondTx, gic, denise, bob, caroline)
            assertNotHasTransaction(updateDiamondTx, alice)

            val updatedDiamond = updateDiamondTx.getSingleDiamondOutput()
            assertDiamondProperties(updatedDiamond, updatedDiamondProperties)

            // STEP 06: Caroline redeems the token with Denise
            // This should exit the holdable token
            val redeemDiamondTx = caroline.redeemTokens(
                nftLinearId = moveTokenToCarolineTx.outputStates.single(),
                redeemFrom = denise.x500Name,
            )
            assertHasTransaction(redeemDiamondTx, caroline, denise)
            assertNotHasTransaction(redeemDiamondTx, gic, alice, bob)

            // FINAL POSITIONS

            // GIC, Denise, Bob and Caroline have the latest evolvable token; Alice does not
            assertHasDiamondReport(updatedDiamondProperties, updatedDiamond.linearId, gic, denise, bob, caroline)
            assertNotHasDiamondReport(updatedDiamondProperties, updatedDiamond.linearId, alice)

            // Alice has an outdated (and unconsumed) evolvable token; GIC, Denise, Bob and Caroline do not
            assertHasDiamondReport(publishedDiamondProperties, publishedDiamond.linearId, alice)
            assertNotHasDiamondReport(publishedDiamondProperties, publishedDiamond.linearId, gic, denise, bob, caroline)

            // No one has nonfungible (discrete) tokens
            assertNotHasUnconsumedNonFungibleToken(issueTokenTx.outputStates.single(), gic, denise, alice, bob, caroline)
            // assert the linear ID was unchanged between transactions so we son't need to check for unconsumed tokens again
            assertEquals(issueTokenTx.outputStates.single(), moveTokenToBobTx.outputStates.single())
            assertEquals(issueTokenTx.outputStates.single(), moveTokenToCarolineTx.outputStates.single())
        }
    }

    private fun assertDiamondProperties(
        diamondGradingReportDigest: DiamondGradingReportDigest,
        diamondProperties: DiamondProperties
    ) {
        val commonError = "Original diamond did not match the published diamond."
        assertEquals(
            diamondProperties.caratWeight,
            diamondGradingReportDigest.caratWeight,
            "$commonError Carat weight differs."
        )
        assertEquals(
            diamondProperties.colorScale,
            diamondGradingReportDigest.color,
            "$commonError  Color scale differs."
        )
        assertEquals(
            diamondProperties.clarityScale,
            diamondGradingReportDigest.clarity,
            "$commonError  Clarity scale differs."
        )
        assertEquals(diamondProperties.cutScale, diamondGradingReportDigest.cut, "$commonError Cut differs.")
    }

    private fun SignedTransactionDigest.getSingleDiamondOutput(): DiamondGradingReportDigest {
        return jsonMapper.readValue(outputStates.single(), DiamondGradingReportDigest::class.java)
    }

    private fun Node.createEvolvableDiamondToken(
        diamondProperties: DiamondProperties,
        requestor: CordaX500Name
    ): SignedTransactionDigest =
        httpRpcClient<FlowStarterRPCOps, SignedTransactionDigest> {
            val flowOutcomeResponse = getFlowOutcome(
                runFlow(
                    CreateEvolvableDiamondTokenFlow::class,
                    mapOf(
                        "caratWeight" to diamondProperties.caratWeight.toString(),
                        "colorScale" to diamondProperties.colorScale.toString(),
                        "clarityScale" to diamondProperties.clarityScale.toString(),
                        "cutScale" to diamondProperties.cutScale.toString(),
                        "requestor" to requestor.toString()
                    )
                )
            )
            jsonMapper.readValue(flowOutcomeResponse.resultJson, SignedTransactionDigest::class.java)
        }

    private fun Node.updateEvolvableToken(
        tokenLinearId: String,
        diamondProperties: DiamondProperties
    ): SignedTransactionDigest =
        httpRpcClient<FlowStarterRPCOps, SignedTransactionDigest> {
            val flowOutcomeResponse = getFlowOutcome(
                runFlow(
                    UpdateEvolvableDiamondTokenFlow::class,
                    mapOf(
                        "tokenLinearId" to tokenLinearId,
                        "caratWeight" to diamondProperties.caratWeight.toString(),
                        "colorScale" to diamondProperties.colorScale.toString(),
                        "clarityScale" to diamondProperties.clarityScale.toString(),
                        "cutScale" to diamondProperties.cutScale.toString()
                    )
                )
            )
            jsonMapper.readValue(flowOutcomeResponse.resultJson, SignedTransactionDigest::class.java)
        }

    private fun Node.redeemTokens(
        nftLinearId: String,
        redeemFrom: CordaX500Name,
    ): SignedTransactionDigest =
        httpRpcClient<FlowStarterRPCOps, SignedTransactionDigest> {
            val flowOutcomeResponse = getFlowOutcome(
                runFlow(
                    RedeemEvolvableDiamondTokenFlow::class,
                    mapOf(
                        "nftLinearId" to nftLinearId,
                        "redeemFrom" to redeemFrom.toString(),
                    )
                )
            )
            jsonMapper.readValue(flowOutcomeResponse.resultJson, SignedTransactionDigest::class.java)
        }

    private fun Node.issueNonFungibleTokens(
        tokenLinearId: String,
        issueTo: CordaX500Name,
        anonymous: Boolean,
    ): SignedTransactionDigest =
        httpRpcClient<FlowStarterRPCOps, SignedTransactionDigest> {
            val flowOutcomeResponse = getFlowOutcome(
                runFlow(
                    IssueNonFungibleDiamondTokenFlow::class,
                    mapOf(
                        "tokenLinearId" to tokenLinearId,
                        "issueTo" to issueTo.toString(),
                        "anonymous" to anonymous.toString(),
                    )
                )
            )
            jsonMapper.readValue(flowOutcomeResponse.resultJson, SignedTransactionDigest::class.java)
        }

    private fun Node.moveNonFungibleTokens(
        nftLinearId: String,
        moveTo: CordaX500Name,
        anonymous: Boolean,
    ): SignedTransactionDigest =
        httpRpcClient<FlowStarterRPCOps, SignedTransactionDigest> {
            val flowOutcomeResponse = getFlowOutcome(
                runFlow(
                    MoveNonFungibleDiamondTokenFlow::class,
                    mapOf(
                        "nftLinearId" to nftLinearId,
                        "moveTo" to moveTo.toString(),
                        "anonymous" to anonymous.toString(),
                    )
                )
            )
            jsonMapper.readValue(flowOutcomeResponse.resultJson, SignedTransactionDigest::class.java)
        }

    private fun assertHasTransaction(stx: SignedTransactionDigest, vararg nodes: Node) {
        nodes.forEach { node -> node.assertHasTransaction(stx) }
    }

    private fun assertNotHasTransaction(stx: SignedTransactionDigest, vararg nodes: Node) {
        nodes.forEach { node -> node.assertHasTransaction(stx, false) }
    }

    private fun Node.assertHasTransaction(
        stx: SignedTransactionDigest,
        hasTransaction: Boolean = true
    ) =
        httpRpcClient<FlowStarterRPCOps, Unit> {
            val resultJson = getFlowOutcome(
                runFlow(
                    HasTransactionFlow::class,
                    mapOf(
                        "transactionDigest" to stx.toJsonString()
                    )
                )
            ).resultJson

            assertEquals(hasTransaction, jsonMapper.readValue(resultJson, Boolean::class.java))
        }

    private fun assertHasDiamondReport(diamondProperties: DiamondProperties, linearId: String, vararg nodes: Node) {
        nodes.forEach { node -> node.assertHasDiamondReport(diamondProperties, linearId, true) }
    }

    private fun assertNotHasDiamondReport(diamondProperties: DiamondProperties, linearId: String, vararg nodes: Node) {
        nodes.forEach { node -> node.assertHasDiamondReport(diamondProperties, linearId, false) }
    }

    private fun Node.assertHasDiamondReport(diamondProperties: DiamondProperties, linearId: String, result: Boolean) {
        httpRpcClient<FlowStarterRPCOps, Unit> {
            val resultJson = getFlowOutcome(
                runFlow(
                    GetDiamondReportFlow::class,
                    mapOf(
                        "linearId" to linearId,
                    )
                )
            ).resultJson

            val diamondDigest = jsonMapper.readValue(resultJson, DiamondGradingReportDigest::class.java)
            if (result) {
                assertEquals(diamondProperties, diamondDigest.diamondProperties())
            } else {
                assertNotEquals(diamondProperties, diamondDigest.diamondProperties())
            }
        }
    }

    private fun assertNotHasUnconsumedNonFungibleToken(linearId: String, vararg nodes: Node) {
        nodes.forEach { node -> node.assertHasUnconsumedNonFungibleToken(linearId, false) }
    }

    private fun Node.assertHasUnconsumedNonFungibleToken(linearId: String, result: Boolean) {
        httpRpcClient<FlowStarterRPCOps, Unit> {
            val resultJson = getFlowOutcome(
                runFlow(
                    HasUnconsumedNonFungibleTokenFlow::class,
                    mapOf(
                        "linearId" to linearId,
                    )
                )
            ).resultJson

            val hasUnconsumedNonFungibleToken = jsonMapper.readValue(resultJson, Boolean::class.java)
            assertEquals(result, hasUnconsumedNonFungibleToken)
        }
    }

    private data class DiamondProperties(
        val caratWeight: BigDecimal,
        val colorScale: ColorScale,
        val clarityScale: ClarityScale,
        val cutScale: CutScale
    )

    private fun DiamondGradingReportDigest.diamondProperties(): DiamondProperties {
        return DiamondProperties(
            caratWeight,
            color,
            clarity,
            cut
        )
    }
}