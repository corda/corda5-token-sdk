package com.r3.corda.lib.tokens.diamondDemo.flows

import com.r3.corda.lib.tokens.testing.states.DiamondGradingReport
import com.r3.corda.lib.tokens.testing.states.DiamondGradingReportDigest
import com.r3.corda.lib.tokens.workflows.flows.rpc.UpdateEvolvableToken
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.JsonConstructor
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.seconds
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.services.vault.IdentityStateAndRefPostProcessor
import net.corda.v5.ledger.services.vault.StateStatus
import net.corda.v5.ledger.transactions.SignedTransactionDigest
import java.math.BigDecimal

@StartableByRPC
class UpdateEvolvableDiamondTokenFlow
@JsonConstructor constructor(
    val params: RpcStartFlowRequestParameters
) : Flow<SignedTransactionDigest> {

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @Suspendable
    override fun call(): SignedTransactionDigest {
        val parameters: Map<String, String> = jsonMarshallingService.parseJson(params.parametersInJson)

        val tokenLinearId = UniqueIdentifier.fromString(parameters["tokenLinearId"]!!)
        val caratWeight = parameters["caratWeight"]?.let { BigDecimal(it) }
        val colorScale = parameters["colorScale"]?.let { DiamondGradingReport.ColorScale.valueOf(it) }
        val clarityScale = parameters["clarityScale"]?.let { DiamondGradingReport.ClarityScale.valueOf(it) }
        val cutScale = parameters["cutScale"]?.let { DiamondGradingReport.CutScale.valueOf(it) }

        val cursor = persistenceService.query<StateAndRef<DiamondGradingReport>>(
            "LinearState.findByUuidAndStateStatus",
            mapOf(
                "uuid" to tokenLinearId.id,
                "stateStatus" to StateStatus.UNCONSUMED,
            ),
            IdentityStateAndRefPostProcessor.POST_PROCESSOR_NAME,
        )

        val results = mutableListOf<StateAndRef<DiamondGradingReport>>()
        do {
            val pollResult = cursor.poll(1, 5.seconds)
            results.addAll(pollResult.values)
        } while (!pollResult.isLastResult)

        require(results.size == 1)

        val token = results.single()

        val newGradingReport = with(token.state.data) {
            DiamondGradingReport(
                caratWeight = caratWeight ?: this.caratWeight,
                color = colorScale ?: this.color,
                clarity = clarityScale ?: this.clarity,
                cut = cutScale ?: this.cut,
                assessor = this.assessor,
                requester = this.requester,
                linearId = this.linearId
            )
        }

        val stx = flowEngine.subFlow(UpdateEvolvableToken(token, newGradingReport))

        val reportDigest = DiamondGradingReportDigest(newGradingReport)
        return SignedTransactionDigest(
            stx.id,
            listOf(reportDigest.toJsonString()),
            stx.sigs
        )
    }
}