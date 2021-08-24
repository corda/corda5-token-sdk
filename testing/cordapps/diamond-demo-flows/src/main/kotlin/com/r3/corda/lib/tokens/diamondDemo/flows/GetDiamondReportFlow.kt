package com.r3.corda.lib.tokens.diamondDemo.flows

import com.r3.corda.lib.tokens.testing.states.DiamondGradingReport
import com.r3.corda.lib.tokens.testing.states.DiamondGradingReportDigest
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.JsonConstructor
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.flows.StartableByRPC
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

@StartableByRPC
class GetDiamondReportFlow
@JsonConstructor constructor(
    val params: RpcStartFlowRequestParameters
) : Flow<DiamondGradingReportDigest> {

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @Suspendable
    override fun call(): DiamondGradingReportDigest {
        val parameters: Map<String, String> = jsonMarshallingService.parseJson(params.parametersInJson)
        val tokenLinearId = UniqueIdentifier.fromString(parameters["linearId"]!!)
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

        val report = results.single().state.data

        return DiamondGradingReportDigest(report)
    }
}