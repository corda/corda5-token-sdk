package com.r3.corda.lib.tokens.diamondDemo.flows

import net.corda.v5.application.flows.BadRpcStartFlowRequestException
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.stream.Cursor
import net.corda.v5.base.util.seconds
import net.corda.v5.ledger.services.vault.IdentityStateAndRefPostProcessor
import net.corda.v5.ledger.services.vault.StateStatus
import java.util.*

private fun <T> Cursor<T>.getResults(): List<T> {
    val result = mutableListOf<T>()
    do {
        val pollResult = poll(5, 5.seconds)
        result.addAll(pollResult.values)
    } while (!pollResult.isLastResult)
    return result
}

private fun <T> PersistenceService.findByUuidAndStateStatus(
    uuid: UUID,
    stateStatus: StateStatus = StateStatus.UNCONSUMED
): Cursor<T> {
    return query(
        "LinearState.findByUuidAndStateStatus",
        mapOf(
            "uuid" to uuid,
            "stateStatus" to stateStatus,
        ),
        IdentityStateAndRefPostProcessor.POST_PROCESSOR_NAME,
    )
}

fun <T> PersistenceService.getUnconsumedLinearStates(uuid: UUID, expectedSize: Int? = null): List<T> {

    val results = findByUuidAndStateStatus<T>(uuid).getResults()
    expectedSize?.let {
        require(results.size == it) {
            "Expected to find exactly $it unconsumed states for ID but found ${results.size}."
        }
    }
    return results
}


fun Map<String, String>.getMandatoryParameter(param: String): String {
    return this[param] ?: throw BadRpcStartFlowRequestException(
        "Mandatory parameter required to start flow was not found. Missing parameter: [$param]"
    )
}