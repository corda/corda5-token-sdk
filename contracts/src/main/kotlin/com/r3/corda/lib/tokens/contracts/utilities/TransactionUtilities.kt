package com.r3.corda.lib.tokens.contracts.utilities

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.crypto.DigestAlgorithmName
import net.corda.v5.crypto.SecureHash
import net.corda.v5.crypto.getZeroHash
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.ContractState
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.transactions.LedgerTransaction
import net.corda.v5.ledger.transactions.inputsOfType
import net.corda.v5.ledger.transactions.outputsOfType

// Transaction helpers.

/** Get single input/output from ledger transaction. */
inline fun <reified T : ContractState> LedgerTransaction.singleInput() = inputsOfType<T>().single()

inline fun <reified T : ContractState> LedgerTransaction.singleOutput() = outputsOfType<T>().single()

// State summing utilities.

/**
 * Sums the [IssuedTokenType] amounts in the list of [FungibleToken]s. Note that all tokens must have the same issuer
 * otherwise this function will throw an [IllegalArgumentException]. If issuers differ then filter the list before using
 * this function.
 */
fun Iterable<FungibleToken>.sumTokenStatesOrThrow(): Amount<IssuedTokenType> {
    return map { it.amount }.sumTokensOrThrow()
}

/** Sums the held token amounts states in the list, returning null if there are none. */
fun Iterable<FungibleToken>.sumTokenStatesOrNull(): Amount<IssuedTokenType>? {
    return map { it.amount }.sumIssuedTokensOrNull()
}

/** Sums the cash states in the list, returning zero of the given currency+issuer if there are none. */
fun Iterable<FungibleToken>.sumTokenStatesOrZero(
    token: IssuedTokenType
): Amount<IssuedTokenType> {
    return map { it.amount }.sumIssuedTokensOrZero(token)
}

/** Sums the token amounts in the list of state and refs. */
fun Iterable<StateAndRef<FungibleToken>>.sumTokenStateAndRefs(): Amount<IssuedTokenType> {
    return map { it.state.data.amount }.sumTokensOrThrow()
}

/** Sums the held token amount state and refs in the list, returning null if there are none. */
fun Iterable<StateAndRef<FungibleToken>>.sumTokenStateAndRefsOrNull(): Amount<IssuedTokenType>? {
    return map { it.state.data.amount }.sumIssuedTokensOrNull()
}

/**
 * Sums the held token amounts state and refs in the list, returning zero of the given currency+issuer if there are
 * none.
 */
fun Iterable<StateAndRef<FungibleToken>>.sumTokenStateAndRefsOrZero(
    token: IssuedTokenType
): Amount<IssuedTokenType> {
    return map { it.state.data.amount }.sumIssuedTokensOrZero(token)
}

/** Filters a list of tokens of the same type by issuer. */
fun Iterable<FungibleToken>.filterTokensByIssuer(issuer: Party): List<FungibleToken> {
    return filter { it.amount.token.issuer == issuer }
}

/** Filters a list of token state and refs with the same token type by issuer. */
fun Iterable<StateAndRef<FungibleToken>>.filterTokenStateAndRefsByIssuer(
    issuer: Party
): List<StateAndRef<FungibleToken>> {
    return filter { it.state.data.amount.token.issuer == issuer }
}

// Utilities for ensuring that the JAR which implements the specified TokenType is added to the transaction.

internal val attachmentCache = HashMap<Class<*>, SecureHash>()

/**
 * If the [TokenType] is not a [TokenPointer] this function discovers the JAR which implements the receiving [TokenType].
 */
fun TokenType.getAttachmentIdForGenericParam(
    hashingService: HashingService
): SecureHash? {
    val computedValue = synchronized(attachmentCache) {
        val startingPoint = if (this is IssuedTokenType) {
            this.tokenType.javaClass
        } else {
            this.javaClass
        }
        attachmentCache.computeIfAbsent(startingPoint) { clazz ->
            var classToSearch: Class<*> = clazz
            while (classToSearch != this.tokenClass && classToSearch != TokenPointer::class.java) {
                classToSearch = this.tokenClass
            }
            if (classToSearch.protectionDomain.codeSource.location
                    == TokenType::class.java.protectionDomain.codeSource.location) {
                hashingService.getZeroHash(DigestAlgorithmName.SHA2_256)
            } else {
                val hash = hashingService.hash(
                    classToSearch.protectionDomain.codeSource.location.readBytes(),
                    DigestAlgorithmName.SHA2_256
                )
                hash
            }
        }
    }
    return if (computedValue == hashingService.getZeroHash(DigestAlgorithmName.SHA2_256)) {
        null
    } else {
        computedValue
    }
}
