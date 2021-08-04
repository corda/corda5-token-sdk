@file:JvmName("TokenUtilities")

package com.r3.corda.lib.tokens.workflows.utilities

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.ledger.UniqueIdentifier

/**
 * Creates a [NonFungibleToken] from an [IssuedTokenType].
 * E.g. IssuedTokenType<TokenType> -> NonFungibleToken.
 * This function must exist outside of the contracts module as creating a unique identifier is non-deterministic.
 */
fun IssuedTokenType.heldBy(owner: AbstractParty, hashingService: HashingService): NonFungibleToken = _heldBy(owner, hashingService)

private fun IssuedTokenType._heldBy(owner: AbstractParty, hashingService: HashingService): NonFungibleToken {
    return NonFungibleToken(this, owner, UniqueIdentifier(), hashingService)
}
