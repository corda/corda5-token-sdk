package com.r3.corda.lib.tokens.selection

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.v5.application.identity.Party
import net.corda.v5.ledger.contracts.StateAndRef

data class TokenQueryBy (
    val issuer: Party?,
    val predicate: (StateAndRef<FungibleToken>) -> Boolean,
    val customPostProcessorName: String?
) {
    constructor() : this(null, { true }, null)
    constructor(issuer: Party?) : this(issuer, { true }, null)
    constructor(predicate: (StateAndRef<FungibleToken>) -> Boolean) : this(null, predicate, null)
    constructor(customPostProcessorName: String?) : this(null, { true }, customPostProcessorName)
    constructor(issuer: Party?, predicate: (StateAndRef<FungibleToken>) -> Boolean) : this(issuer, predicate, null)
    constructor(issuer: Party?, customPostProcessorName: String?) : this(issuer, { true }, customPostProcessorName)
    constructor(predicate: (StateAndRef<FungibleToken>) -> Boolean, customPostProcessorName: String?) : this(null, predicate, customPostProcessorName)
}

internal fun TokenQueryBy.issuerAndPredicate(): (StateAndRef<FungibleToken>) -> Boolean {
    return if (issuer != null) {
        { stateAndRef -> stateAndRef.state.data.amount.token.issuer == issuer && predicate(stateAndRef) }
    } else predicate
}