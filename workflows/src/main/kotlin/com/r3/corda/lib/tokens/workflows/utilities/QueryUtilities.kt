@file:JvmName("QueryUtilities")

package com.r3.corda.lib.tokens.workflows.utilities

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.datatypes.NamedQueryAndParameters
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.stream.Cursor
import net.corda.v5.base.util.seconds
import net.corda.v5.crypto.toStringShort
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.LinearState
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.services.vault.StateStatus

// TODO Revisit this API and add documentation.
/** Miscellaneous helpers. */

// Grabs the latest version of a linear state for a specified linear ID.
@Suspendable
inline fun <reified T : LinearState> PersistenceService.getLinearStateById(linearId: UniqueIdentifier): StateAndRef<T>? {
    return query<StateAndRef<T>>(
        "LinearState.findByUuidAndStateStatus",
        mapOf(
            "uuid" to linearId,
            "stateStatus" to StateStatus.UNCONSUMED,
        )
    ).poll(1, 10.seconds).values.firstOrNull()
}

/** Utilities for getting tokens from the vault and performing miscellaneous queries. */

// TODO: Add queries for getting the balance of all tokens, not just relevant ones.
// TODO: Allow discrimination by issuer or a set of issuers.

// Returns all held token amounts of a specified token with given issuer.
// We need to discriminate on the token type as well as the symbol as different tokens might use the same symbols.
fun tokenAmountWithIssuerCriteria(token: TokenType, issuer: Party): NamedQueryAndParameters {
    return namedQueryForFungibleTokenClassIdentifierAndIssuer(token, issuer)
}

fun sumTokenAmountWithIssuerCriteria(token: TokenType, issuer: Party): NamedQueryAndParameters {
    return namedQueryForSumFungibleTokenAmountClassIdentifierAndIssuer(token, issuer)
}

fun heldTokenAmountCriteria(token: TokenType, holder: AbstractParty): NamedQueryAndParameters {
    return namedQueryForFungibleTokenClassIdentifierAndOwningKey(token, holder)
}

// Returns all held token amounts of a specified token.
// We need to discriminate on the token type as well as the symbol as different tokens might use the same symbols.
// TODO should be called token amount criteria (there is no owner selection)
fun namedQueryForFungibleTokenClassAndIdentifier(token: TokenType): NamedQueryAndParameters {
    return NamedQueryAndParameters(
        "FungibleTokenSchemaV1.PersistentFungibleToken.findAllUnconsumedTokensByClassAndIdentifier",
        mapOf(
            "tokenClass" to token.tokenClass,
            "tokenIdentifier" to token.tokenIdentifier,
        )
    )
}

fun namedQueryForSumFungibleTokenAmountClassAndIdentifier(token: TokenType): NamedQueryAndParameters {
    return NamedQueryAndParameters(
        "FungibleTokenSchemaV1.PersistentFungibleToken.sumAllUnconsumedTokensByClassAndIdentifier",
        mapOf(
            "tokenClass" to token.tokenClass,
            "tokenIdentifier" to token.tokenIdentifier,
        )
    )
}

fun namedQueryForFungibleTokenClassIdentifierAndIssuer(token: TokenType, issuer: Party): NamedQueryAndParameters {
    return NamedQueryAndParameters(
        "FungibleTokenSchemaV1.PersistentFungibleToken.findAllUnconsumedTokensByClassIdentifierAndIssuer",
        mapOf(
            "tokenClass" to token.tokenClass,
            "tokenIdentifier" to token.tokenIdentifier,
            "issuer" to issuer,
        )
    )
}

fun namedQueryForSumFungibleTokenAmountClassIdentifierAndIssuer(token: TokenType, issuer: Party): NamedQueryAndParameters {
    return NamedQueryAndParameters(
        "FungibleTokenSchemaV1.PersistentFungibleToken.sumAllUnconsumedTokensByClassIdentifierAndIssuer",
        mapOf(
            "tokenClass" to token.tokenClass,
            "tokenIdentifier" to token.tokenIdentifier,
            "issuer" to issuer,
        )
    )
}

fun namedQueryForFungibleTokenClassIdentifierAndOwningKey(token: TokenType, holder: AbstractParty): NamedQueryAndParameters {
    return NamedQueryAndParameters(
        "FungibleTokenSchemaV1.PersistentFungibleToken.findAllUnconsumedTokensByClassIdentifierAndOwningKey",
        mapOf(
            "tokenClass" to token.tokenClass,
            "tokenIdentifier" to token.tokenIdentifier,
            "owningKeyHash" to holder.owningKey.toStringShort(),
        )
    )
}

fun namedQueryForNonfungibleTokenClassAndIdentifier(token: TokenType): NamedQueryAndParameters {
    return NamedQueryAndParameters(
        "NonFungibleTokenSchemaV1.PersistentNonFungibleToken.findAllUnconsumedTokensByClassAndIdentifier",
        mapOf(
            "tokenClass" to token.tokenClass,
            "tokenIdentifier" to token.tokenIdentifier,
        )
    )
}

fun namedQueryForNonfungibleTokenClassIdentifierAndIssuer(token: TokenType, issuer: Party): NamedQueryAndParameters {
    return NamedQueryAndParameters(
        "NonFungibleTokenSchemaV1.PersistentNonFungibleToken.findAllUnconsumedTokensByClassIdentifierAndIssuer",
        mapOf(
            "tokenClass" to token.tokenClass,
            "tokenIdentifier" to token.tokenIdentifier,
            "issuer" to issuer,
        )
    )
}

@Suspendable
fun cursorToAmount(token: TokenType, cursor: Cursor<StateAndRef<FungibleToken>>): Amount<TokenType> {
    val results = mutableListOf<StateAndRef<FungibleToken>>()
    do {
        val pollResult = cursor.poll(10, 5.seconds)
        results.addAll(pollResult.values)
    } while (!pollResult.isLastResult)

    return if (results.isEmpty()) {
        Amount(0L, token)
    } else {
        Amount(results.sumOf { it.state.data.amount.quantity }, token)
    }
}

/** General queries. */

// Get all held token amounts for a specific token, ignoring the issuer.
fun PersistenceService.tokenAmountsByToken(token: TokenType): Cursor<StateAndRef<FungibleToken>> {
    val (namedQuery, params) = namedQueryForFungibleTokenClassAndIdentifier(token)
    return query(namedQuery, params)
}

// Get all held tokens for a specific token, ignoring the issuer.
fun PersistenceService.heldTokensByToken(token: TokenType): Cursor<NonFungibleToken> {
    val (namedQuery, params) = namedQueryForNonfungibleTokenClassAndIdentifier(token)
    return query(namedQuery, params)
}

/** TokenType balances. */

// We need to group the sum by the token class and token identifier.
fun PersistenceService.tokenBalance(token: TokenType): Amount<TokenType> {
    val (namedQuery, params) = namedQueryForSumFungibleTokenAmountClassAndIdentifier(token)
    val result = query<StateAndRef<FungibleToken>>(namedQuery, params)
    return cursorToAmount(token, result)
}

// We need to group the sum by the token class and token identifier takes issuer into consideration.
fun PersistenceService.tokenBalanceForIssuer(token: TokenType, issuer: Party): Amount<TokenType> {
    val (namedQuery, params) = sumTokenAmountWithIssuerCriteria(token, issuer)
    val result = query<StateAndRef<FungibleToken>>(namedQuery, params)
    return cursorToAmount(token, result)
}

// TODO Add function to return balances grouped by issuers?

/* Queries with criteria. Eg. with issuer etc. */

// Get NonFungibleToken with issuer.
fun PersistenceService.heldTokensByTokenIssuer(token: TokenType, issuer: Party): Cursor<StateAndRef<NonFungibleToken>> {
    val (namedQuery, params) = namedQueryForNonfungibleTokenClassIdentifierAndIssuer(token, issuer)
    return query(namedQuery, params)
}
