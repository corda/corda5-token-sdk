package com.r3.corda.lib.tokens.workflows.utilities

import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.ledger.contracts.TransactionState

class TokenUtilities

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

infix fun <T : AbstractToken> T.withNewHolder(newHolder: AbstractParty) = withNewHolder(newHolder)