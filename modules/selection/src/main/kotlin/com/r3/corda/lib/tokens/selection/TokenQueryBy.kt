package com.r3.corda.lib.tokens.selection

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.v5.application.identity.Party
import net.corda.v5.ledger.contracts.StateAndRef

//TODO: After 2.0 we should get rid of queryCriteria, because it was a mistake to expose it in the
data class TokenQueryBy (
    val issuer: Party?,
    val predicate: (StateAndRef<FungibleToken>) -> Boolean,
) {
    constructor() : this(null, { true })
    constructor(issuer: Party?) : this(issuer, { true })
    constructor(predicate: (StateAndRef<FungibleToken>) -> Boolean) : this(null, predicate)
}

internal fun TokenQueryBy.issuerAndPredicate(): (StateAndRef<FungibleToken>) -> Boolean {
    return if (issuer != null) {
        { stateAndRef -> stateAndRef.state.data.amount.token.issuer == issuer && predicate(stateAndRef) }
    } else predicate
}