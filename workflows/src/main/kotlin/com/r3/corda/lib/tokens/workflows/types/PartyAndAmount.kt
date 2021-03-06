package com.r3.corda.lib.tokens.workflows.types

import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.ledger.contracts.Amount

/**
 * A simple holder for a (possibly anonymous) [AbstractParty] and a quantity of tokens.
 * Used in [generateMove] to define what [amount] of token [T] [party] should receive.
 */
@CordaSerializable
data class PartyAndAmount<T : TokenType>(val party: AbstractParty, val amount: Amount<T>)

/**
 * A simple holder for a (possibly anonymous) [AbstractParty] and a token.
 * Used in [generateMove] to define what token [T] [party] should receive.
 */
@CordaSerializable
data class PartyAndToken(val party: AbstractParty, val token: TokenType)

fun Iterable<PartyAndAmount<TokenType>>.toPairs() = map { Pair(it.party, it.amount) }

