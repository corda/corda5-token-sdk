package com.r3.corda.lib.tokens.selection.api

import com.r3.corda.lib.tokens.selection.database.config.DatabaseSelectionConfig
import com.r3.corda.lib.tokens.selection.memory.config.InMemorySelectionConfig
import com.r3.corda.lib.tokens.selection.memory.services.VaultWatcherService
import net.corda.v5.application.cordapp.CordappConfig
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.node.services.IdentityService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.services.VaultService
import org.slf4j.LoggerFactory

interface StateSelectionConfig {
    @Suspendable
    fun toSelector(
        vaultService: VaultService,
        identityService: IdentityService,
        flowEngine: FlowEngine,
    ): Selector
}

/**
 * CorDapp config format:
 *
 * stateSelection {
 *      database {
 *          maxRetries: Int
 *          retrySleep: Int
 *          retryCap: Int
 *          pageSize: Int
 *      }
 *      or
 *      in_memory {
 *          indexingStrategy: ["external_id"|"public_key"|"token_only"]
 *          cacheSize: Int
 *      }
 * }
 *
 * Use ConfigSelection.getPreferredSelection to choose based on you cordapp config between database token selection and in memory one.
 * By default Move and Redeem methods use config to switch between them. If no config option is provided it will default to database
 * token selection.
 */
object ConfigSelection {
    val logger = LoggerFactory.getLogger("configSelectionLogger")

    @Suspendable
    fun getPreferredSelection(
        vaultService: VaultService,
        identityService: IdentityService,
        flowEngine: FlowEngine,
        vaultWatcherService: VaultWatcherService,
        config: CordappConfig
    ): Selector {
        return if (!config.exists("stateSelection")) {
            logger.warn("No configuration for state selection, defaulting to database selection.")
            DatabaseSelectionConfig() // Return default database selection
        } else {
            when {
                config.exists("stateSelection.database") -> DatabaseSelectionConfig.parse(config)
                config.exists("stateSelection.inMemory") -> InMemorySelectionConfig.parse(config, vaultWatcherService)
                else -> throw IllegalArgumentException("Provide correct state-selection type string in the config, see kdocs for ConfigSelection.")
            }
        }.toSelector(vaultService, identityService, flowEngine)
    }
}