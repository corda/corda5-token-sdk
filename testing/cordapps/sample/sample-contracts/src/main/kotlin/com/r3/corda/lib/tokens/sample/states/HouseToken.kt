package com.r3.corda.lib.tokens.sample.states

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.sample.contracts.HouseContract
import net.corda.v5.application.identity.Party
import net.corda.v5.application.utilities.JsonRepresentable
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.BelongsToContract

// A token representing a house on ledger.
@BelongsToContract(HouseContract::class)
data class HouseToken(
    val address: String,
    val valuation: Amount<TokenType>,
    override val maintainers: List<Party>,
    override val fractionDigits: Int = 5,
    override val linearId: UniqueIdentifier
) : EvolvableTokenType(), JsonRepresentable {
    override fun toJsonString(): String {
        return """
            |{ 
            | "address" : "$address", 
            | "valuation" : { 
            | "amount" : "${valuation.quantity * valuation.displayTokenSize.toDouble()}", 
            | "type" : "${valuation.token.tokenIdentifier}" 
            | } ,
            | "linearId" : "$linearId"
            | }
            | """.trimMargin()
    }

}
