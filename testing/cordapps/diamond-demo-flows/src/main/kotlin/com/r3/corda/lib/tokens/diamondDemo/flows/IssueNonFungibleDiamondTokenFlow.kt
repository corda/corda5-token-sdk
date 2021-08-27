package com.r3.corda.lib.tokens.diamondDemo.flows

import com.r3.corda.lib.tokens.builder.heldBy
import com.r3.corda.lib.tokens.builder.withHashingService
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.test.utils.getMandatoryParameter
import com.r3.corda.lib.tokens.test.utils.getUnconsumedLinearStates
import com.r3.corda.lib.tokens.testing.states.DiamondGradingReport
import com.r3.corda.lib.tokens.workflows.flows.rpc.ConfidentialIssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
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
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.transactions.SignedTransactionDigest

@StartableByRPC
class IssueNonFungibleDiamondTokenFlow
@JsonConstructor constructor(
    val params: RpcStartFlowRequestParameters
) : Flow<SignedTransactionDigest> {

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var hashingService: HashingService

    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @CordaInject
    lateinit var identityService: IdentityService

    @Suspendable
    override fun call(): SignedTransactionDigest {
        val parameters: Map<String, String> = jsonMarshallingService.parseJson(params.parametersInJson)

        val tokenLinearId = UniqueIdentifier.fromString(parameters.getMandatoryParameter("tokenLinearId"))
        val issueTo = CordaX500Name.parse(parameters.getMandatoryParameter("issueTo"))
        val issueToParty = identityService.partyFromName(issueTo)
            ?: throw BadRpcStartFlowRequestException("Could not find party for CordaX500Name: $issueTo")
        val anonymous = parameters.getMandatoryParameter("anonymous").toBoolean()

        val results: List<StateAndRef<DiamondGradingReport>> =
            persistenceService.getUnconsumedLinearStates(tokenLinearId.id, expectedSize = 1)

        val token = results.single().state.data

        val nft =
            token.toPointer<DiamondGradingReport>() issuedBy flowIdentity.ourIdentity heldBy issueToParty withHashingService hashingService

        val stx = if (anonymous) {
            flowEngine.subFlow(ConfidentialIssueTokens(listOf(nft)))
        } else {
            flowEngine.subFlow(IssueTokens(listOf(nft)))
        }

        return SignedTransactionDigest(
            stx.id,
            listOf(nft.linearId.toString()),
            stx.sigs
        )
    }
}