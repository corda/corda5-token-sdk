package com.r3.corda.lib.tokens.diamondDemo.flows

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.workflows.flows.rpc.ConfidentialMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.JsonConstructor
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.MemberLookupService
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.seconds
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.services.vault.IdentityStateAndRefPostProcessor
import net.corda.v5.ledger.services.vault.StateStatus
import net.corda.v5.ledger.transactions.SignedTransactionDigest
import java.util.*

@StartableByRPC
class MoveNonFungibleDiamondToken
@JsonConstructor constructor(
    val params: RpcStartFlowRequestParameters
) : Flow<SignedTransactionDigest> {

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @CordaInject
    lateinit var memberLookupService: MemberLookupService

    @Suspendable
    override fun call(): SignedTransactionDigest {
        val parameters: Map<String, String> = jsonMarshallingService.parseJson(params.parametersInJson)

        val nftLinearId = UUID.fromString(parameters["nftLinearId"]!!)
        val moveTo = memberLookupService.lookup(CordaX500Name.parse(parameters["moveTo"]!!))!!.party
        val anonymous = parameters["anonymous"]!!.toBoolean()

        val cursor = persistenceService.query<StateAndRef<NonFungibleToken>>(
            "LinearState.findByUuidAndStateStatus",
            mapOf(
                "uuid" to nftLinearId,
                "stateStatus" to StateStatus.UNCONSUMED,
            ),
            IdentityStateAndRefPostProcessor.POST_PROCESSOR_NAME,
        )

        val results = mutableListOf<StateAndRef<NonFungibleToken>>()
        do {
            val pollResult = cursor.poll(1, 5.seconds)
            results.addAll(pollResult.values)
        } while (!pollResult.isLastResult)

        require(results.size == 1)

        val nft = results.single().state.data

        val partyAndToken = PartyAndToken(
            moveTo,
            nft.token.tokenType
        )

        val stx = if (anonymous) {
            flowEngine.subFlow(ConfidentialMoveNonFungibleTokens(partyAndToken, emptyList()))
        } else {
            flowEngine.subFlow(MoveNonFungibleTokens(partyAndToken))
        }


        return SignedTransactionDigest(
            stx.id,
            listOf(nft.linearId.toString()),
            stx.sigs
        )
    }
}