package com.r3.corda.lib.tokens.diamondDemo.flows

import net.corda.v5.application.flows.BadRpcStartFlowRequestException
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.stream.Cursor
import net.corda.v5.base.util.seconds
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.ContractState
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.services.vault.IdentityStateAndRefPostProcessor
import net.corda.v5.ledger.services.vault.StateStatus
import java.util.UUID

@Suspendable
private fun <T> Cursor<T>.getResults(): List<T> {
    val result = mutableListOf<T>()
    do {
        val pollResult = poll(5, 5.seconds)
        result.addAll(pollResult.values)
    } while (!pollResult.isLastResult)
    return result
}

private fun <T : ContractState> PersistenceService.findByUuidAndStateStatus(
    uuid: UUID,
    stateStatus: StateStatus = StateStatus.UNCONSUMED
): Cursor<StateAndRef<T>> = query(
    "LinearState.findByUuidAndStateStatus",
    mapOf(
        "uuid" to uuid,
        "stateStatus" to stateStatus,
    ),
    IdentityStateAndRefPostProcessor.POST_PROCESSOR_NAME,
)

@Suspendable
fun <T : ContractState> PersistenceService.getUnconsumedLinearStates(
    uuid: UUID,
    expectedSize: Int? = null
): List<StateAndRef<T>> {
    val results = findByUuidAndStateStatus<T>(uuid).getResults()
    expectedSize?.let {
        require(results.size == it) {
            "Expected to find exactly $it unconsumed states for ID but found ${results.size}."
        }
    }
    return results
}

@Suspendable
fun <T : ContractState> PersistenceService.getUnconsumedLinearState(uuid: UUID): StateAndRef<T> =
    getUnconsumedLinearStates<T>(uuid, 1).single()

@Suspendable
fun <T : ContractState> PersistenceService.getUnconsumedLinearStateData(uuid: UUID): T =
    getUnconsumedLinearState<T>(uuid).state.data

fun JsonMarshallingService.parseParameters(params: RpcStartFlowRequestParameters): Map<String, String> =
    parseJson(params.parametersInJson)

fun Map<String, String>.getMandatoryParameter(param: String): String =
    this[param] ?: throw BadRpcStartFlowRequestException(
        "Mandatory parameter required to start flow was not found. Missing parameter: [$param]"
    )

fun Map<String, String>.getMandatoryBoolean(param: String): Boolean =
    this[param]?.toBoolean() ?: throw BadRpcStartFlowRequestException(
        "Mandatory parameter required to start flow was not found. Missing parameter: [$param]"
    )

fun Map<String, String>.getMandatoryUUID(param: String): UUID =
    UniqueIdentifier.fromString(getMandatoryParameter(param)).id

fun Map<String, String>.getMandatoryPartyFromName(identityService: IdentityService, param: String): Party {
    val name = CordaX500Name.parse(getMandatoryParameter(param))
    return identityService.partyFromName(name)
        ?: throw BadRpcStartFlowRequestException("Could not find requesting party from CordaX500Name: $name")
}