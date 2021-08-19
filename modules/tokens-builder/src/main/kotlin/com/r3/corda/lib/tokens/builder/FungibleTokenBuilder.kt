package com.r3.corda.lib.tokens.builder

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.ledger.contracts.Amount
import java.math.BigDecimal

/**
 * A utility class designed for Java developers to more easily access Kotlin DSL
 * functions to build Fungible tokens and their component classes.
 *
 * This function vaguely follows a builder pattern, design choices were made
 * to emulate Kotlin syntax as closely as possible for an easily transferable
 * developer experience in Java.
 */
class FungibleTokenBuilder {
    private var amount: BigDecimal? = null
    private lateinit var tokenType: TokenType
    private lateinit var issuer: Party
    private lateinit var holder: AbstractParty
    private var issuedTokenTypeAmount: Amount<IssuedTokenType>? = null

    /**
     * Set the [amount] member property of the builder using a provided [Long].
     *
     * @param longAmount The [Long] that will be converted to set the [amount] member property
     */
    fun withAmount(longAmount: Long) = this.apply { amount = BigDecimal.valueOf(longAmount) }

    /**
     * Set the [amount] member property of the builder using a provided [Int].
     *
     * @param intAmount The [Int] that will be converted to set the [amount] member property
     */
    fun withAmount(intAmount: Int) = this.apply { amount = BigDecimal(intAmount) }

    /**
     * Set the [amount] member property of the builder using a provided [Double].
     *
     * @param doubleAmount The [Double] that will be converted to set the [amount] member property
     */
    fun withAmount(doubleAmount: Double) = this.apply { amount = BigDecimal.valueOf(doubleAmount) }

    /**
     * Set the [amount] member property of the builder using a provided [BigDecimal].
     *
     * @param bigDecimal The [BigDecimal] that will be used to set the [amount] member property
     */
    fun withAmount(bigDecimalAmount: BigDecimal) = this.apply { amount = bigDecimalAmount }

    /**
     * Replicates the Kotlin DSL [ofTokenType] infix function. Supplies a [TokenType] to the builder
     * which will be used to build an [Amount] of a [TokenType].
     *
     * @param t The token type that will be used to build an [Amount] of a [TokenType]
     */
    fun <T : TokenType> ofTokenType(t: T): FungibleTokenBuilder = this.apply { this.tokenType = t }

    /**
     * Replicates the Kotlin DSL [issuedBy] infix function. Supplies a [Party] to the builder
     * representing the identity of the issuer of an [Amount] of an [IssuedTokenType].
     *
     * @param party The issuing identity that will be used to build an [Amount] of an [IssuedTokenType]
     */
    fun issuedBy(party: Party): FungibleTokenBuilder = this.apply {
        this.issuer = party
    }

    /**
     * Can be used to add a prebuilt [Amount] of type [IssuedTokenType] to a builder. Cannot be used in combination with [withAmount], [ofTokenType],
     * and [issuedBy].
     */
    fun withIssuedTokenTypeAmount(issuedTokenTypeAmount: Amount<IssuedTokenType>) = this.apply {
        this.issuedTokenTypeAmount = issuedTokenTypeAmount
    }

    /**
     * Replicates the Kotlin DSL [heldBy] infix function. Supplies an [AbstractParty] to the builder
     * representing the identity of the holder of a new fungible token.
     *
     * @param party The identity of the holder that will be used to build an [Amount] of an [IssuedTokenType].
     */
    fun heldBy(party: AbstractParty): FungibleTokenBuilder = this.apply {
        this.holder = party
    }

    /**
     * Builds an [Amount] of a [TokenType]. This function will throw a [TokenBuilderException] exception if the appropriate builder methods have not been
     * called: [withAmount], [ofTokenType], or if an issued amount token type has already been added to the builder using [withIssuedTokenTypeAmount].
     */
    @Throws(TokenBuilderException::class)
    fun buildAmountTokenType(): Amount<TokenType> = when {
        issuedTokenTypeAmount != null -> {
            throw TokenBuilderException("Cannot build amount token type when amount issued token type has already been set. " +
                    "Do not try to set amount and/or token type in combination with setting the issued token type.")
        }
        !::tokenType.isInitialized -> {
            throw TokenBuilderException("A Token Type has not been provided to the builder.")
        }
        amount == null -> {
            throw TokenBuilderException("An amount value has not been provided to the builder.")
        }
        else -> {
            amount!! of tokenType
        }
    }

    /**
     * Builds an [Amount] of an [IssuedTokenType]. This function will throw a [TokenBuilderException] if the appropriate builder methods have not been
     * called: [withAmount], [ofTokenType], [issuedBy], or if an issued amount token type has already been added to the builder using
     * [withIssuedTokenTypeAmount].
     */
    @Throws(TokenBuilderException::class)
    fun buildAmountIssuedTokenType(): Amount<IssuedTokenType> = when {
        issuedTokenTypeAmount != null -> {
            throw TokenBuilderException("Cannot build amount issued token type when it has already been set. " +
                    "Setting amount, token type and/or issuer cannot be used in combination with setting a pre-built amount of type issued token type.")
        }
        !::issuer.isInitialized -> {
            throw TokenBuilderException("A token issuer has not been provided to the builder.")
        }
        else -> {
            buildAmountTokenType() issuedBy issuer
        }
    }

    /**
     * Builds a [FungibleToken] state. This function will throw a [TokenBuilderException] if the appropriate builder methods have not been called.
     * i.e. a combination of [withAmount], [ofTokenType], [issuedBy], [heldBy], or a combination of [withIssuedTokenTypeAmount], [heldBy].
     * The build process uses the input [HashingService] in the [FungibleToken] construction.
     */
    @Throws(TokenBuilderException::class)
    fun buildFungibleToken(hashingService: HashingService) = when {
        !::holder.isInitialized -> {
            throw TokenBuilderException("A token holder has not been provided to the builder.")
        }
        else -> {
            FungibleToken(issuedTokenTypeAmount ?: buildAmountIssuedTokenType(), holder, hashingService)
        }
    }
}
