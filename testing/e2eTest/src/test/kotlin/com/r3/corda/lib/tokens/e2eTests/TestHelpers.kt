package com.r3.corda.lib.tokens.e2eTests

import com.google.gson.GsonBuilder
import net.corda.client.rpc.flow.*
import net.corda.client.rpc.proxy.network.MembershipGroupRPCOps
import net.corda.test.dev.network.Node
import net.corda.test.dev.network.httpRpcClient
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.identity.CordaX500Name
import org.assertj.core.api.Assertions.assertThat
import java.util.*
import kotlin.reflect.KClass


internal fun Node.getX500Name(): CordaX500Name =
	httpRpcClient<MembershipGroupRPCOps, CordaX500Name> { getMyMemberInfo().x500Name }

internal fun FlowStarterRPCOps.runFlow(flowClass: KClass<*>, parameters: Map<Any, Any>): RpcStartFlowResponse {
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
	assertThat(response.stateMachineRunId).isNotNull

	return response
}

internal fun FlowStarterRPCOps.getFlowOutcome(response: RpcStartFlowResponse): RpcFlowOutcomeResponse {
	var result: RpcFlowOutcomeResponse
	do {
		result = getFlowOutcome(response.stateMachineRunId.uuid.toString())
	} while (result.status == RpcFlowStatus.RUNNING)

	assertThat(RpcFlowStatus.COMPLETED).isEqualTo(result.status)

	return result
}
