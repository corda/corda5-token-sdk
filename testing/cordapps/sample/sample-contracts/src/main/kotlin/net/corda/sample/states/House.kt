package net.corda.sample.states

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.sample.contracts.HouseContract
import net.corda.v5.application.identity.Party
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.BelongsToContract

// A token representing a house on ledger.
@BelongsToContract(HouseContract::class)
data class House(
    val address: String,
    val valuation: Amount<TokenType>,
    override val maintainers: List<Party>,
    override val fractionDigits: Int = 5,
    override val linearId: UniqueIdentifier
) : EvolvableTokenType()
