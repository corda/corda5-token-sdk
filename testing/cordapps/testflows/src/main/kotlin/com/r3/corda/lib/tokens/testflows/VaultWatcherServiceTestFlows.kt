package com.r3.corda.lib.tokens.testflows

import com.r3.corda.lib.tokens.selection.memory.services.VaultWatcherService
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.JsonConstructor
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.injection.CordaFlowInjectable
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.CordaService

/**
 * Very basic flow that verifies that [VaultWatcherService] can be injected in to a flow and a corda service.
 */
@StartableByRPC
class VaultWatcherServiceIsInjectableFlow @JsonConstructor constructor(
    @Suppress("UNUSED_PARAMETER") params: RpcStartFlowRequestParameters
) : Flow<Boolean> {

    @CordaInject
    lateinit var vaultWatcherService: VaultWatcherService

    @CordaInject
    lateinit var vaultWatcherServiceIsInjectableService: VaultWatcherServiceIsInjectableService

    override fun call()= ::vaultWatcherService.isInitialized
            && vaultWatcherServiceIsInjectableService.vaultWatcherServiceIsInjected()
}

/**
 * Very basic service that verifies that [VaultWatcherService] can be injected in to a corda service.
 */
interface VaultWatcherServiceIsInjectableService : CordaService, CordaFlowInjectable {
    fun vaultWatcherServiceIsInjected(): Boolean
}

class VaultWatcherServiceIsInjectableServiceImpl : VaultWatcherServiceIsInjectableService {
    @CordaInject
    private lateinit var vaultWatcherService: VaultWatcherService

    override fun vaultWatcherServiceIsInjected() = ::vaultWatcherService.isInitialized
}