package com.r3.corda.lib.tokens.selection.database.config

import com.r3.corda.lib.tokens.selection.api.ConfigSelection
import com.r3.corda.lib.tokens.selection.api.StateSelectionConfig
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.selection.memory.config.getIntOrNull
import net.corda.v5.application.cordapp.CordappConfig
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable

const val MAX_RETRIES_DEFAULT = 8
const val RETRY_SLEEP_DEFAULT = 100
const val RETRY_CAP_DEFAULT = 2000
const val PAGE_SIZE_DEFAULT = 200

data class DatabaseSelectionConfig (
    val maxRetries: Int,
    val retrySleep: Int,
    val retryCap: Int,
    val pageSize: Int
) : StateSelectionConfig {

    constructor() : this(MAX_RETRIES_DEFAULT, RETRY_SLEEP_DEFAULT, RETRY_CAP_DEFAULT, PAGE_SIZE_DEFAULT)

    companion object {
        @JvmStatic
        fun parse(config: CordappConfig): DatabaseSelectionConfig {
            val maxRetries = config.getIntOrNull("stateSelection.database.maxRetries") ?: MAX_RETRIES_DEFAULT
            val retrySleep = config.getIntOrNull("stateSelection.database.retrySleep") ?: RETRY_SLEEP_DEFAULT
            val retryCap = config.getIntOrNull("stateSelection.database.retryCap") ?: RETRY_CAP_DEFAULT
            val pageSize = config.getIntOrNull("stateSelection.database.pageSize") ?: PAGE_SIZE_DEFAULT
            ConfigSelection.logger.info("Found database token selection configuration with values maxRetries: $maxRetries, retrySleep: $retrySleep, retryCap: $retryCap, pageSize: $pageSize")
            return DatabaseSelectionConfig(maxRetries, retrySleep, retryCap, pageSize)
        }
    }

    @Suspendable
    override fun toSelector(
        persistenceService: PersistenceService,
        identityService: IdentityService,
        flowEngine: FlowEngine,
    ): DatabaseTokenSelection {
        return DatabaseTokenSelection(persistenceService, identityService, flowEngine, maxRetries, retrySleep, retryCap, pageSize)
    }
}
