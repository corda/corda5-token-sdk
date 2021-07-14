@file:JvmName("Rubles")

package com.r3.corda.lib.tokens.testing.states

import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.BelongsToContract
import net.corda.v5.ledger.contracts.Contract
import net.corda.v5.ledger.transactions.LedgerTransaction

class Ruble : TokenType("рубль", 0) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        return true
    }
}

class PhoBowl : TokenType("PTK", 0) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        return true
    }
}

@JvmField
val RUB = Ruble()

@JvmField
val PTK = PhoBowl()

data class Appartment(val id: String = "Foo") : TokenType(id, 0)

/**
 * Test class only used to test that are grouped by Contract as well as TokenType
 */
@BelongsToContract(DodgeTokenContract::class)
open class DodgeToken(
    amount: Amount<IssuedTokenType>,
    holder: AbstractParty,
    hashingService: HashingService
) : FungibleToken(amount, holder, hashingService)

open class DodgeTokenContract : Contract {
    override fun verify(tx: LedgerTransaction) {
    }
}

/**
 * Test class only used to test that tokens cannot change class during a move
 */
@BelongsToContract(FungibleTokenContract::class)
open class RubleToken(
    amount: Amount<IssuedTokenType>,
    holder: AbstractParty,
    hashingService: HashingService
) : FungibleToken(amount, holder, hashingService)