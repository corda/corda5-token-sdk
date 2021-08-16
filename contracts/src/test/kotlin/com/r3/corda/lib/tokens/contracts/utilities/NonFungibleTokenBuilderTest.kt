package com.r3.corda.lib.tokens.contracts.utilities

import com.r3.corda.lib.tokens.contracts.TokenBuilderException
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import io.mockk.every
import io.mockk.mockk
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.crypto.HashingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals

class NonFungibleTokenBuilderTest {

    private lateinit var mockTokenType: TokenType
    private lateinit var mockIssuer: Party
    private lateinit var mockHolder: AbstractParty
    private lateinit var mockIssuedTokenType: IssuedTokenType
    private lateinit var mockHashingService: HashingService

    private lateinit var builder: NonFungibleTokenBuilder

    @BeforeEach
    fun setUp() {
        mockTokenType = mockk {
            every { displayTokenSize } returns BigDecimal.ONE
            every { tokenIdentifier } returns "TEST_IDENTIFIER"
            every { fractionDigits } returns 1
            every { tokenClass } returns TokenType::class.java
        }
        mockIssuer = mockk()
        mockHolder = mockk()
        mockIssuedTokenType = mockk {
            every { tokenIdentifier } returns "MOCK_ITT_IDENTIFIER"
            every { tokenType } returns mockTokenType
            every { issuer } returns mockIssuer
        }
        mockHashingService = mockk {
            every { digestLength(any()) } returns 64
        }
        builder = NonFungibleTokenBuilder()
    }

    @Test
    fun `buildIssuedTokenType has expected output when builder is set up correctly`() {
        builder.ofTokenType(mockTokenType)
            .issuedBy(mockIssuer)

        val result =  builder.buildIssuedTokenType()
        assertEquals(mockTokenType, result.tokenType)
        assertEquals(mockIssuer, result.issuer)
    }

    @Test
    fun `buildIssuedTokenType fails if issued token type is already set`() {
        builder.ofTokenType(mockTokenType)
            .issuedBy(mockIssuer)

        builder.withIssuedTokenType(mockIssuedTokenType)

        assertThrows<TokenBuilderException> { builder.buildIssuedTokenType() }
    }

    @Test
    fun `buildIssuedTokenType fails if token type is not set`() {
        builder.issuedBy(mockIssuer)

        assertThrows<TokenBuilderException> { builder.buildIssuedTokenType() }
    }

    @Test
    fun `buildIssuedTokenType fails if issuer is not set`() {
        builder.ofTokenType(mockTokenType)

        assertThrows<TokenBuilderException> { builder.buildIssuedTokenType() }
    }

    @Test
    fun `buildNonFungibleToken has expected output when builder is set up correctly`() {
        builder.ofTokenType(mockTokenType)
            .issuedBy(mockIssuer)
            .heldBy(mockHolder)

        val result =  builder.buildNonFungibleToken(mockHashingService)
        assertEquals(mockHolder, result.holder)
        assertEquals(mockIssuer, result.issuer)
    }

    @Test
    fun `buildNonFungibleToken fails when holder is not set`() {
        builder.ofTokenType(mockTokenType)
            .issuedBy(mockIssuer)

        assertThrows<TokenBuilderException> { builder.buildNonFungibleToken(mockHashingService) }
    }

    @Test
    fun `buildNonFungibleToken creates token using set issued token type`() {
        builder.withIssuedTokenType(mockIssuedTokenType)
            .heldBy(mockHolder)

        val result =  builder.buildNonFungibleToken(mockHashingService)
        assertEquals(mockHolder, result.holder)
        assertEquals(mockIssuer, result.issuer)
        assertEquals(mockIssuedTokenType, result.token)
    }
}