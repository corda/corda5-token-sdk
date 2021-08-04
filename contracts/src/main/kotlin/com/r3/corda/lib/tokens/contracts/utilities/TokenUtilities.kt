package com.r3.corda.lib.tokens.contracts.utilities

import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.crypto.toStringShort
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.TransactionState

class TokenUtilities

// ------------------------------------------------------
// Creates a tokens from (amounts of) issued token types.
// ------------------------------------------------------

/**
 * Creates a [FungibleToken] from an an amount of [IssuedTokenType].
 * E.g. Amount<IssuedTokenType<TokenType>> -> FungibleToken<TokenType>.
 */
fun Amount<IssuedTokenType>.heldBy(owner: AbstractParty, hashingService: HashingService): FungibleToken = _heldBy(owner, hashingService)

internal fun Amount<IssuedTokenType>._heldBy(owner: AbstractParty, hashingService: HashingService): FungibleToken {
    return FungibleToken(this, owner, hashingService)
}

// --------------------------
// Add a a notary to a token.
// --------------------------

/** Adds a notary [Party] to an [AbstractToken], by wrapping it in a [TransactionState]. */
infix fun <T : AbstractToken> T.withNotary(notary: Party): TransactionState<T> = _withNotary(notary)

internal infix fun <T : AbstractToken> T._withNotary(notary: Party): TransactionState<T> {
    return TransactionState(data = this, notary = notary)
}

/** Adds a notary [Party] to an [EvolvableTokenType], by wrapping it in a [TransactionState]. */
infix fun <T : EvolvableTokenType> T.withNotary(notary: Party): TransactionState<T> = _withNotary(notary)

internal infix fun <T : EvolvableTokenType> T._withNotary(notary: Party): TransactionState<T> {
    return TransactionState(data = this, notary = notary)
}

/**
 * Converts [AbstractToken.holder] into a more friendly string. It uses only the x500 organisation for [Party] objects
 * and shortens the public key for [AnonymousParty]s to the first 16 characters.
 */
val AbstractToken.holderString: String
    get() =
        (holder as? Party)?.name?.organisation ?: holder.owningKey.toStringShort().substring(0, 16)

infix fun <T : AbstractToken> T.withNewHolder(newHolder: AbstractParty) = withNewHolder(newHolder)