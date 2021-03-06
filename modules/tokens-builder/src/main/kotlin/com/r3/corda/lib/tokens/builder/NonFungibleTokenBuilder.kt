package com.r3.corda.lib.tokens.builder

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.Amount

/**
 * A utility class designed for Java developers to more easily access Kotlin DSL
 * functions to build non-fungible tokens and their component classes.
 *
 * This function vaguely follows a builder pattern, design choices were made
 * to emulate Kotlin syntax as closely as possible for an easily transferable
 * developer experience in Java.
 */
class NonFungibleTokenBuilder {
    private lateinit var tokenType: TokenType
    private lateinit var issuer: Party
    private lateinit var holder: AbstractParty
    private var issuedTokenType: IssuedTokenType? = null

    /**
     * Replicates the Kotlin DSL [ofTokenType] infix function. Supplies a [TokenType] to the builder
     * which will be used to build an [IssuedTokenType].
     *
     * @param t The token type that will be used to build an [IssuedTokenType]
     */
    fun <T : TokenType> ofTokenType(t: T): NonFungibleTokenBuilder = this.apply { this.tokenType = t }

    /**
     * Replicates the Kotlin DSL [issuedBy] infix function. Supplies a [Party] to the builder
     * representing the identity of the issuer of a non-fungible [IssuedTokenType].
     *
     * @param party The issuing identity that will be used to build an [IssuedTokenType]
     */
    fun issuedBy(party: Party): NonFungibleTokenBuilder = this.apply { this.issuer = party }

    /**
     * Replicates the Kotlin DSL [heldBy] infix function. Supplies a [Party] to the builder
     * representing the identity of the holder of a new non-fungible token.
     *
     * @param party The identity of the holder that will be used to build an [Amount] of an [IssuedTokenType].
     */
    fun heldBy(party: AbstractParty): NonFungibleTokenBuilder = this.apply { this.holder = party }

    /**
     * Can be used to add a prebuilt [IssuedTokenType] to a builder. Cannot be used in combination with [ofTokenType], and [issuedBy].
     */
    fun withIssuedTokenType(issuedTokenType: IssuedTokenType): NonFungibleTokenBuilder =
        this.apply { this.issuedTokenType = issuedTokenType }

    /**
     * Builds an [IssuedTokenType]. This function will throw a [TokenBuilderException] if the appropriate
     * builder methods have not been called: [ofTokenType], [issuedBy], or if an issued token type has already been
     * added to the builder.
     */
    @Throws(TokenBuilderException::class)
    fun buildIssuedTokenType(): IssuedTokenType = when {
        issuedTokenType != null -> {
            throw TokenBuilderException("An issued token type has already been provided to the builder.")
        }
        !::tokenType.isInitialized -> {
            throw TokenBuilderException("A token type has not been provided to the builder.")
        }
        !::issuer.isInitialized -> {
            throw TokenBuilderException("A token issuer has not been provided to the builder.")
        }
        else -> {
            tokenType issuedBy issuer
        }
    }

    /**
     * Builds a [NonFungibleToken] state. This function will throw a [TokenBuilderException] if the appropriate
     * builder methods have not been called: [ofTokenType], [issuedBy], [heldBy].
     */
    @Throws(TokenBuilderException::class)
    fun buildNonFungibleToken(hashingService: HashingService): NonFungibleToken = when {
        !::holder.isInitialized -> {
            throw TokenBuilderException("A token holder has not been provided to the builder.")
        }
        else -> {
            NonFungibleToken(issuedTokenType ?: buildIssuedTokenType(), holder, UniqueIdentifier(), hashingService)
        }
    }
}