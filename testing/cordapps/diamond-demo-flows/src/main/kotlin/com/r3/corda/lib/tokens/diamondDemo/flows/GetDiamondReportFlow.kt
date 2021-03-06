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
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.StateAndRef

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
        val tokenLinearId = jsonMarshallingService.parseParameters(params).getMandatoryUUID("linearId")
        val report = persistenceService.getUnconsumedLinearStateData<DiamondGradingReport>(tokenLinearId)
        return DiamondGradingReportDigest(report)
    }
}