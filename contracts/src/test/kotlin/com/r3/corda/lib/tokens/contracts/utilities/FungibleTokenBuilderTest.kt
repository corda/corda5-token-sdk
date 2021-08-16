package com.r3.corda.lib.tokens.contracts.utilities

import com.r3.corda.lib.tokens.contracts.TokenBuilderException
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import io.mockk.every
import io.mockk.mockk
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.ledger.contracts.Amount
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals

class FungibleTokenBuilderTest {

    private lateinit var mockTokenType: TokenType
    private lateinit var mockIssuer: Party
    private lateinit var mockTokenTypeIssuedByMockIssuer: IssuedTokenType
    private lateinit var mockHolder: AbstractParty
    private lateinit var mockHashingService: HashingService
    private lateinit var issuedTokenTypeAmountMock: Amount<IssuedTokenType>

    private lateinit var builder: FungibleTokenBuilder

    @BeforeEach
    fun setUp() {
        mockTokenType = mockk {
            every { displayTokenSize } returns BigDecimal.ONE
            every { tokenIdentifier } returns "TEST_IDENTIFIER"
            every { fractionDigits } returns 1
            every { tokenClass } returns TokenType::class.java
        }

        mockIssuer = mockk {
            every { name } returns mockk {
                every { organisation } returns "MOCK_ORG"
            }
        }

        mockTokenTypeIssuedByMockIssuer = mockk {
            every { tokenType } returns mockTokenType
            every { issuer } returns mockIssuer
        }
        mockHolder = mockk()
        issuedTokenTypeAmountMock = mockk()
        mockHashingService = mockk {
            every { digestLength(any()) } returns 64
        }

        builder = FungibleTokenBuilder()
    }

    @Test
    fun `buildAmountTokenType has expected output when builder is set up correctly`() {
        // Set up FungibleToken properties necessary to build amount token type
        builder.withAmount(1)
            .ofTokenType(mockTokenType)

        // Assert Amount of type TokenType is correctly constructed
        val result =  builder.buildAmountTokenType()
        assertEquals(1, result.quantity)
        assertEquals(mockTokenType, result.token)
    }

    @Test
    fun `buildAmountTokenType fails if issued token type amount is already set`() {
        // Set up FungibleToken properties necessary to build amount token type
        builder.withAmount(1)
            .ofTokenType(mockTokenType)

        // Add issued token type which isn't allowed combined with the above builder functions
        builder.withIssuedTokenTypeAmount(issuedTokenTypeAmountMock)

        // Assert exception thrown when building amount token type
        assertThrows<TokenBuilderException> { builder.buildAmountTokenType() }
    }

    @Test
    fun `buildAmountTokenType fails if token type is not set`() {
        // Set up FungibleToken properties without mandatory token type
        builder.withAmount(1)

        // Assert exception thrown when building amount token type
        assertThrows<TokenBuilderException> { builder.buildAmountTokenType() }
    }

    @Test
    fun `buildAmountTokenType fails if amount is not set`() {
        // Set up FungibleToken properties without mandatory token type
        builder.ofTokenType(mockTokenType)

        // Assert exception thrown when building amount token type
        assertThrows<TokenBuilderException> { builder.buildAmountTokenType() }
    }

    @Test
    fun `buildAmountIssuedTokenType has expected output when builder is set up correctly`() {
        // Set up FungibleToken properties necessary to build amount token type
        builder.withAmount(1)
            .ofTokenType(mockTokenType)
            .issuedBy(mockIssuer)

        // Assert Amount of type TokenType is correctly constructed
        val result =  builder.buildAmountIssuedTokenType()
        assertEquals(1, result.quantity)
        assertEquals(mockTokenTypeIssuedByMockIssuer, result.token)
        assertEquals(mockIssuer, result.token.issuer)
    }

    @Test
    fun `buildAmountIssuedTokenType fails if issued token type amount is already set`() {
        // Set up FungibleToken properties necessary to build amount token type
        builder.withAmount(1)
            .ofTokenType(mockTokenType)
            .issuedBy(mockIssuer)

        // Add issued token type which isn't allowed combined with the above builder functions
        builder.withIssuedTokenTypeAmount(issuedTokenTypeAmountMock)

        // Assert exception thrown when building amount token type
        assertThrows<TokenBuilderException> { builder.buildAmountIssuedTokenType() }
    }

    @Test
    fun `buildAmountIssuedTokenType fails if issuer is not set`() {
        // Set up FungibleToken properties without mandatory token type
        builder.withAmount(1)
            .ofTokenType(mockTokenType)

        // Assert exception thrown when building amount token type
        assertThrows<TokenBuilderException> { builder.buildAmountIssuedTokenType() }
    }

    @Test
    fun `buildFungibleToken has expected output when builder is set up correctly`() {
        // Set up FungibleToken properties necessary to build amount token type
        builder.withAmount(1)
            .ofTokenType(mockTokenType)
            .issuedBy(mockIssuer)
            .heldBy(mockHolder)

        // Assert Amount of type TokenType is correctly constructed
        val result =  builder.buildFungibleToken(mockHashingService)
        assertEquals(mockHolder, result.holder)
        assertEquals(mockIssuer, result.issuer)
        assertEquals(1, result.amount.quantity)
        assertEquals(mockTokenTypeIssuedByMockIssuer, result.amount.token)
        assertEquals(mockIssuer, result.amount.token.issuer)
    }
}