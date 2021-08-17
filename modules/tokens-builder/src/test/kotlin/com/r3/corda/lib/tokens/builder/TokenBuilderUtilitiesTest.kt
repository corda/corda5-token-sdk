package com.r3.corda.lib.tokens.builder

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import io.mockk.every
import io.mockk.mockk
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.ledger.contracts.Amount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TokenBuilderUtilitiesTest {

    private val mockTokenType: TokenType = mockk {
        every { tokenClass } returns TokenType::class.java
    }
    private val mockIssuedTokenType: IssuedTokenType = mockk {
        every { tokenType } returns mockTokenType
    }
    private val mockIssuedTokenTypeAmount: Amount<IssuedTokenType> = mockk {
        every { token } returns mockIssuedTokenType
    }
    private val mockParty: AbstractParty = mockk()
    private val mockHashingService: HashingService = mockk {
        every { digestLength(any()) } returns 64
    }

    @Test
    fun `Create fungible token from amount using infix heldBy and withHashingService`() {
        val token: FungibleToken = mockIssuedTokenTypeAmount heldBy mockParty withHashingService mockHashingService

        assertEquals(mockIssuedTokenTypeAmount, token.amount)
        assertEquals(mockParty, token.holder)
    }

    @Test
    fun `Create non fungible token from amount using infix heldBy and withHashingService`() {
        val token: NonFungibleToken = mockIssuedTokenType heldBy mockParty withHashingService mockHashingService

        assertEquals(mockIssuedTokenType, token.token)
        assertEquals(mockParty, token.holder)
    }
}