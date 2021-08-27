package com.r3.corda.lib.tokens.diamondDemo.flows

import com.r3.corda.lib.tokens.test.utils.getMandatoryParameter
import com.r3.corda.lib.tokens.test.utils.getUnconsumedLinearStates
import com.r3.corda.lib.tokens.testing.states.DiamondGradingReport
import com.r3.corda.lib.tokens.testing.states.DiamondGradingReport.ClarityScale
import com.r3.corda.lib.tokens.testing.states.DiamondGradingReport.ColorScale
import com.r3.corda.lib.tokens.testing.states.DiamondGradingReport.CutScale
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
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.StateAndRef
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

        val tokenLinearId = UniqueIdentifier.fromString(parameters.getMandatoryParameter("tokenLinearId"))
        val caratWeight = parameters["caratWeight"]?.let { BigDecimal(it) }
        val colorScale = parameters["colorScale"]?.let { ColorScale.valueOf(it) }
        val clarityScale = parameters["clarityScale"]?.let { ClarityScale.valueOf(it) }
        val cutScale = parameters["cutScale"]?.let { CutScale.valueOf(it) }

        val results: List<StateAndRef<DiamondGradingReport>> =
            persistenceService.getUnconsumedLinearStates(tokenLinearId.id, expectedSize = 1)
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