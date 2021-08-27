package com.r3.corda.lib.tokens.test.utils

import com.google.gson.GsonBuilder
import net.corda.client.rpc.flow.FlowStarterRPCOps
import net.corda.client.rpc.flow.RpcFlowOutcomeResponse
import net.corda.client.rpc.flow.RpcFlowStatus
import net.corda.client.rpc.flow.RpcStartFlowRequest
import net.corda.client.rpc.flow.RpcStartFlowResponse
import net.corda.client.rpc.proxy.network.MembershipGroupRPCOps
import net.corda.test.dev.network.Node
import net.corda.test.dev.network.httpRpcClient
import net.corda.v5.application.flows.BadRpcStartFlowRequestException
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.stream.Cursor
import net.corda.v5.base.util.seconds
import net.corda.v5.ledger.services.vault.IdentityStateAndRefPostProcessor
import net.corda.v5.ledger.services.vault.StateStatus
import org.assertj.core.api.Assertions.assertThat
import java.util.*
import kotlin.reflect.KClass

fun <T> PersistenceService.getUnconsumedLinearStates(uuid: UUID, expectedSize: Int? = null): List<T> {
    val results = query<T>(
        "LinearState.findByUuidAndStateStatus",
        mapOf(
            "uuid" to uuid,
            "stateStatus" to StateStatus.UNCONSUMED,
        ),
        IdentityStateAndRefPostProcessor.POST_PROCESSOR_NAME,
    ).getResults()
    expectedSize?.let {
        require(results.size == it) {
            "Expected to find exactly $it unconsumed states for ID but found ${results.size}."
        }
    }
    return results
}

fun <T> Cursor<T>.getResults(): List<T> {
    val result = mutableListOf<T>()
    do {
        val pollResult = poll(5, 5.seconds)
        result.addAll(pollResult.values)
    } while (!pollResult.isLastResult)
    return result
}

fun Map<String, String>.getMandatoryParameter(param: String): String {
    return this[param] ?: throw BadRpcStartFlowRequestException(
        "Mandatory parameter required to start flow was not found. Missing parameter: [$param]"
    )
}

fun Node.getX500Name(): CordaX500Name =
    httpRpcClient<MembershipGroupRPCOps, CordaX500Name> { getMyMemberInfo().x500Name }

fun FlowStarterRPCOps.runFlow(flowClass: KClass<*>, parameters: Map<String, String>): RpcStartFlowResponse {
    val clientId = "client-${UUID.randomUUID()}"
    val parametersInJson = GsonBuilder().create().toJson(parameters)

    val response = startFlow(
        RpcStartFlowRequest(
            flowClass.java.name,
            clientId,
            RpcStartFlowRequestParameters(parametersInJson)
        )
    )

    assertThat(response.clientId).isEqualTo(clientId)
    assertThat(response.flowId).isNotNull

    return response
}

fun FlowStarterRPCOps.getFlowOutcome(response: RpcStartFlowResponse): RpcFlowOutcomeResponse {
    var result: RpcFlowOutcomeResponse
    do {
        result = getFlowOutcome(response.flowId.uuid.toString())
    } while (result.status == RpcFlowStatus.RUNNING)

    assertThat(RpcFlowStatus.COMPLETED).isEqualTo(result.status)

    return result
}
