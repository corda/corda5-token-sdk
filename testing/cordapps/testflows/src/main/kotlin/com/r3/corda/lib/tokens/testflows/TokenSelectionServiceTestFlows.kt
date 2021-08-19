package com.r3.corda.lib.tokens.testflows

import com.r3.corda.lib.tokens.selection.memory.services.TokenSelectionService
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.JsonConstructor
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.injection.CordaFlowInjectable
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.CordaService

/**
 * Very basic flow that verifies that [TokenSelectionService] can be injected in to a flow and a corda service.
 */
@StartableByRPC
class TokenSelectionServiceIsInjectableFlow @JsonConstructor constructor(
    @Suppress("UNUSED_PARAMETER") params: RpcStartFlowRequestParameters
) : Flow<Boolean> {

    @CordaInject
    lateinit var tokenSelectionService: TokenSelectionService

    @CordaInject
    lateinit var tokenSelectionServiceIsInjectableService: TokenSelectionServiceIsInjectableService

    override fun call()= ::tokenSelectionService.isInitialized
            && tokenSelectionServiceIsInjectableService.tokenSelectionServiceIsInjected()
}

/**
 * Very basic service that verifies that [TokenSelectionService] can be injected in to a corda service.
 */
interface TokenSelectionServiceIsInjectableService : CordaService, CordaFlowInjectable {
    fun tokenSelectionServiceIsInjected(): Boolean
}

class TokenSelectionServiceIsInjectableServiceImpl : TokenSelectionServiceIsInjectableService {
    @CordaInject
    private lateinit var tokenSelectionService: TokenSelectionService

    override fun tokenSelectionServiceIsInjected() = ::tokenSelectionService.isInitialized
}