@file:JvmName("SelectionUtilities")

package com.r3.corda.lib.tokens.selection

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.datatypes.NamedQueryAndParameters
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.base.exceptions.CordaRuntimeException
import java.util.*

// Returns all held token amounts of a specified token with given issuer.
// We need to discriminate on the token type as well as the symbol as different tokens might use the same symbols.
fun namedQueryForTokenClassIdentifierAndIssuer(token: TokenType, issuer: Party): NamedQueryAndParameters = NamedQueryAndParameters(
    "FungibleTokenSchemaV1.PersistentFungibleToken.findAllUnconsumedTokensByClassIdentifierAndIssuer",
    mapOf(
        "tokenClass" to token.tokenClass,
        "tokenIdentifier" to token.tokenIdentifier,
        "issuer" to issuer,
    )
)

fun namedQueryForTokenClassIdentifierIssuerAndExternalId(token: TokenType, issuer: Party, externalId: UUID): NamedQueryAndParameters = NamedQueryAndParameters(
    "FungibleTokenSchemaV1.PersistentFungibleToken.findAllUnconsumedTokensByClassIdentifierIssuerAndExternalId",
    mapOf(
        "tokenClass" to token.tokenClass,
        "tokenIdentifier" to token.tokenIdentifier,
        "issuer" to issuer,
        "externalId" to externalId,
    )
)


// Returns all held token amounts of a specified token with given holder and issuer.
// We need to discriminate on the token type as well as the symbol as different tokens might use the same symbols.
fun namedQueryForTokenClassIdentifierHolderAndIssuer(token: TokenType, holder: AbstractParty, issuer: Party): NamedQueryAndParameters = NamedQueryAndParameters(
    "FungibleTokenSchemaV1.PersistentFungibleToken.findAllUnconsumedTokensByClassIdentifierHolderAndIssuer",
    mapOf(
        "tokenClass" to token.tokenClass,
        "tokenIdentifier" to token.tokenIdentifier,
        "holder" to holder,
        "issuer" to issuer,
    )
)

// Returns all held token amounts of a specified token.
// We need to discriminate on the token type as well as the symbol as different tokens might use the same symbols.
fun namedQueryForTokenClassAndIdentifier(token: TokenType): NamedQueryAndParameters = NamedQueryAndParameters(
        "FungibleTokenSchemaV1.PersistentFungibleToken.findAllUnconsumedTokensByClassAndIdentifier",
        mapOf<String, Any>(
            "tokenClass" to token.tokenClass,
            "tokenIdentifier" to token.tokenIdentifier,
        )
    )

fun namedQueryForTokenClassIdentifierAndExternalId(token: TokenType, externalId: UUID): NamedQueryAndParameters = NamedQueryAndParameters(
    "FungibleTokenSchemaV1.PersistentFungibleToken.findAllUnconsumedTokensByClassIdentifierAndExternalId",
    mapOf<String, Any>(
        "tokenClass" to token.tokenClass,
        "tokenIdentifier" to token.tokenIdentifier,
        "externalId" to externalId,
    )
)


fun namedQueryForTokenClassIdentifierAndHolder(token: TokenType, holder: AbstractParty): NamedQueryAndParameters = NamedQueryAndParameters(
    "FungibleTokenSchemaV1.PersistentFungibleToken.findAllUnconsumedTokensByClassIdentifierAndHolder",
    mapOf(
        "tokenClass" to token.tokenClass,
        "tokenIdentifier" to token.tokenIdentifier,
        "holder" to holder,
    )
)

/**
 * An exception that is thrown where the specified criteria returns an amount of tokens
 * that is not sufficient for the specified spend. If the amount of tokens *is* sufficient
 * but there is not enough of non-locked tokens available to satisfy the amount then
 * [InsufficientNotLockedBalanceException] will be thrown.
 *
 * @param message The exception message that should be thrown in this context
 */
open class InsufficientBalanceException(message: String) : CordaRuntimeException(message)

/**
 * An exception that is thrown where the specified criteria returns an amount of tokens
 * that is sufficient for the specified spend, however there is not enough of non-locked tokens
 * available to satisfy the amount.
 *
 * @param message The exception message that should be thrown in this context
 */
class InsufficientNotLockedBalanceException(message: String) : InsufficientBalanceException(message)