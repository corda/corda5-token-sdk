package com.r3.corda.lib.tokens.diamondDemo.flows

import com.r3.corda.lib.tokens.test.utils.getMandatoryParameter
import com.r3.corda.lib.tokens.testing.states.DiamondGradingReport
import com.r3.corda.lib.tokens.testing.states.DiamondGradingReportDigest
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import net.corda.v5.application.flows.BadRpcStartFlowRequestException
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.JsonConstructor
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.TransactionState
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.transactions.SignedTransactionDigest

@StartableByRPC
class CreateEvolvableDiamondTokenFlow
@JsonConstructor constructor(
    val params: RpcStartFlowRequestParameters
) : Flow<SignedTransactionDigest> {

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var identityService: IdentityService

    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var notaryLookupService: NotaryLookupService

    @Suspendable
    override fun call(): SignedTransactionDigest {
        val parameters: Map<String, String> = jsonMarshallingService.parseJson(params.parametersInJson)

        val caratWeight = parameters.getMandatoryParameter("caratWeight")
        val colorScale = parameters.getMandatoryParameter("colorScale")
        val clarityScale = parameters.getMandatoryParameter("clarityScale")
        val cutScale = parameters.getMandatoryParameter("cutScale")
        val requestor = CordaX500Name.parse(parameters.getMandatoryParameter("requestor"))
        val requestorParty = identityService.partyFromName(requestor)
            ?: throw BadRpcStartFlowRequestException("Could not find requesting party from CordaX500Name: $requestor")

        val notary = notaryLookupService.notaryIdentities.first()

        val diamond = DiamondGradingReport(
            caratWeight,
            DiamondGradingReport.ColorScale.valueOf(colorScale),
            DiamondGradingReport.ClarityScale.valueOf(clarityScale),
            DiamondGradingReport.CutScale.valueOf(cutScale),
            flowIdentity.ourIdentity,
            requestorParty
        )

        val stx = flowEngine.subFlow(
            CreateEvolvableTokens(
                TransactionState(diamond, notary)
            )
        )

        val reportDigest = DiamondGradingReportDigest(diamond)
        return SignedTransactionDigest(
            stx.id,
            listOf(reportDigest.toJsonString()),
            stx.sigs
        )
    }
}