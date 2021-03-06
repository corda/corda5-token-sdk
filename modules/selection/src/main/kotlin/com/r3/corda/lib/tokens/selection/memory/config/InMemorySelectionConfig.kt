package com.r3.corda.lib.tokens.selection.memory.config

import com.r3.corda.lib.tokens.selection.api.StateSelectionConfig
import com.r3.corda.lib.tokens.selection.memory.selector.LocalTokenSelector
import com.r3.corda.lib.tokens.selection.memory.services.TokenSelectionService
import com.r3.corda.lib.tokens.selection.memory.services.TokenSelectionService.IndexingType
import net.corda.v5.application.cordapp.CordappConfig
import net.corda.v5.application.cordapp.CordappConfigException
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import org.slf4j.LoggerFactory

const val CACHE_SIZE_DEFAULT = 1024 // TODO Return good default, for now it's not wired, it will be done in separate PR.

data class InMemorySelectionConfig (
    val tokenSelectionService: TokenSelectionService,
    val enabled: Boolean,
    val indexingStrategies: List<IndexingType>,
    val cacheSize: Int
) : StateSelectionConfig {

    constructor(
        tokenSelectionService: TokenSelectionService,
        enabled: Boolean,
        indexingStrategies: List<IndexingType>
    ) : this(tokenSelectionService, enabled, indexingStrategies, CACHE_SIZE_DEFAULT)

    companion object {
        private val logger = LoggerFactory.getLogger("inMemoryConfigSelectionLogger")

        @JvmStatic
        fun parse(config: CordappConfig, tokenSelectionService: TokenSelectionService): InMemorySelectionConfig {
            val enabled = if (!config.exists("stateSelection.inMemory.enabled")) {
                logger.warn("Did not detect a configuration for InMemory selection - enabling memory usage for token indexing. Please set stateSelection.inMemory.enabled to \"false\" to disable this")
                true
            } else {
                config.getBoolean("stateSelection.inMemory.enabled")
            }
            val cacheSize = config.getIntOrNull("stateSelection.inMemory.cacheSize")
                ?: CACHE_SIZE_DEFAULT
            val indexingType = try {
                (config.get("stateSelection.inMemory.indexingStrategies") as List<*>).map { IndexingType.valueOf(it.toString()) }
            } catch (e: CordappConfigException) {
                logger.warn("No indexing method specified. Indexes will be created at run-time for each invocation of selectTokens")
                emptyList()
            } catch (e: ClassCastException) {
                logger.warn("No indexing method specified. Indexes will be created at run-time for each invocation of selectTokens")
                emptyList()
            }
            logger.info("Found in memory token selection configuration with values indexing strategy: $indexingType, cacheSize: $cacheSize")
            return InMemorySelectionConfig(tokenSelectionService, enabled, indexingType, cacheSize)
        }

        fun defaultConfig(tokenSelectionService: TokenSelectionService): InMemorySelectionConfig {
            return InMemorySelectionConfig(tokenSelectionService, true, emptyList())
        }
    }

    @Suspendable
    override fun toSelector(
        persistenceService: PersistenceService,
        identityService: IdentityService,
        flowEngine: FlowEngine,
    ): LocalTokenSelector {
        return try {
            LocalTokenSelector(tokenSelectionService, state = null)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Couldn't find TokenSelectionService in CordaServices, please make sure that it was installed in node.")
        }
    }
}

// Helpers for configuration parsing.

fun CordappConfig.getIntOrNull(path: String): Int? {
    return try {
        getInt(path)
    } catch (e: CordappConfigException) {
        if (exists(path)) {
            throw IllegalArgumentException("Provide correct database selection configuration for config path: $path")
        } else {
            null
        }
    }
}
