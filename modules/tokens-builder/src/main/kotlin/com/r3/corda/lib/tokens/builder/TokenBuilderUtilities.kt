package com.r3.corda.lib.tokens.builder

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.ledger.contracts.Amount

class TokenBuilderUtilities

// ------------------------------------------------------
// Creates a tokens from (amounts of) issued token types.
// ------------------------------------------------------

/**
 * Creates a [FungibleTokenBuilder] from an amount of [IssuedTokenType].
 */
infix fun Amount<IssuedTokenType>.heldBy(owner: AbstractParty): FungibleTokenBuilder = _heldBy(owner)

internal infix fun Amount<IssuedTokenType>._heldBy(owner: AbstractParty): FungibleTokenBuilder {
    return FungibleTokenBuilder().withIssuedTokenTypeAmount(this).heldBy(owner)
}

/**
 * Creates a [FungibleToken] from a [FungibleTokenBuilder].
 */
infix fun FungibleTokenBuilder.withHashingService(hashingService: HashingService): FungibleToken = buildFungibleToken(hashingService)

/**
 * Creates a [NonFungibleToken] from an [IssuedTokenType].
 * E.g. IssuedTokenType<TokenType> -> NonFungibleToken.
 * This function must exist outside of the contracts module as creating a unique identifier is non-deterministic.
 */
infix fun IssuedTokenType.heldBy(owner: AbstractParty): NonFungibleTokenBuilder = _heldBy(owner)

private infix fun IssuedTokenType._heldBy(owner: AbstractParty): NonFungibleTokenBuilder {
    return NonFungibleTokenBuilder().withIssuedTokenType(this).heldBy(owner)
}

/**
 * Creates a [NonFungibleToken] from a [NonFungibleTokenBuilder] using a hashing service to create the hash for the jar containing the token type.
 */
infix fun NonFungibleTokenBuilder.withHashingService(hashingService: HashingService): NonFungibleToken = buildNonFungibleToken(hashingService)
