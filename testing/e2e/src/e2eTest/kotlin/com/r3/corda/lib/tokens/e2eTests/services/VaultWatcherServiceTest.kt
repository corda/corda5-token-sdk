package com.r3.corda.lib.tokens.e2eTests.services

import com.google.gson.JsonParser
import com.r3.corda.lib.tokens.e2eTests.alice
import com.r3.corda.lib.tokens.e2eTests.e2eTestNetwork
import com.r3.corda.lib.tokens.e2eTests.getFlowOutcome
import com.r3.corda.lib.tokens.e2eTests.runFlow
import com.r3.corda.lib.tokens.testflows.VaultWatcherServiceIsInjectableFlow
import net.corda.client.rpc.flow.FlowStarterRPCOps
import net.corda.test.dev.network.httpRpcClient
import net.corda.test.dev.network.withFlow
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class VaultWatcherServiceTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun verifySetup() {
            e2eTestNetwork.verify {
                listOf("alice", "bob", "caroline")
                    .map { hasNode(it) }
                    .forEach {
                        it.withFlow<VaultWatcherServiceIsInjectableFlow>()
                    }
            }
        }
    }

    @Test
    fun `VaultWatcherService is injectable in to a flow and corda service`() {
        e2eTestNetwork.use {
            alice().httpRpcClient<FlowStarterRPCOps, Unit> {
                val flowResult = getFlowOutcome(
                    runFlow(
                        VaultWatcherServiceIsInjectableFlow::class,
                        emptyMap()
                    )
                )

                Assertions.assertThat(
                    JsonParser.parseString(flowResult.resultJson).asBoolean
                ).isTrue
            }
        }
    }
}