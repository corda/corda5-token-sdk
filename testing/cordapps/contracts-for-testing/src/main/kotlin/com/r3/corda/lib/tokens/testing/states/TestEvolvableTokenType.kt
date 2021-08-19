package com.r3.corda.lib.tokens.testing.states

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.testing.contracts.TestEvolvableTokenContract
import net.corda.v5.application.identity.Party
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.BelongsToContract

@BelongsToContract(TestEvolvableTokenContract::class)
data class TestEvolvableTokenType(
    override val maintainers: List<Party>,
    val observers: List<Party> = emptyList(),
    override val participants: List<Party> = (maintainers + observers),
    override val linearId: UniqueIdentifier = UniqueIdentifier(),
    override val fractionDigits: Int = 0
) : EvolvableTokenType()