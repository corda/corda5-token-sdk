package com.r3.corda.lib.tokens.contracts

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException

class NonFungibleTokenContractTest {
    @Test
    fun `verifyIssue throws an exception when there are inputs`() {
        val contract = NonFungibleTokenContract()

        val e = assertThrows<IllegalArgumentException> {
            contract.verifyIssue(
                mockk(),
                listOf(mockk()),
                listOf(mockk()),
                listOf(mockk()),
                listOf(mockk())
            )
        }

        assertThat(e).hasMessageContaining("When issuing non fungible tokens, there cannot be any input states.")
    }
}
