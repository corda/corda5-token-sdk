package com.r3.corda.lib.tokens.builder

import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import io.mockk.every
import io.mockk.mockk
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.crypto.SecureHash
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.BelongsToContract
import net.corda.v5.ledger.contracts.Contract
import net.corda.v5.ledger.contracts.TransactionState
import net.corda.v5.ledger.transactions.LedgerTransaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TokenBuilderUtilitiesTest {

    private val mockTokenType: TokenType = mockk {
        every { tokenClass } returns TokenType::class.java
    }
    private val mockIssuedTokenType: IssuedTokenType = mockk {
        every { tokenType } returns mockTokenType
    }
    private val mockIssuedTokenTypeAmount: Amount<IssuedTokenType> = mockk {
        every { token } returns mockIssuedTokenType
    }
    private val mockParty: AbstractParty = mockk()
    private val mockHashingService: HashingService = mockk {
        every { digestLength(any()) } returns 64
    }
    private val mockNotary: Party = mockk()

    @Test
    fun `Create fungible token from amount using infix heldBy and withHashingService`() {
        val token: FungibleToken = mockIssuedTokenTypeAmount heldBy mockParty withHashingService mockHashingService

        assertEquals(mockIssuedTokenTypeAmount, token.amount)
        assertEquals(mockParty, token.holder)
    }

    @Test
    fun `Create non fungible token from amount using infix heldBy and withHashingService`() {
        val token: NonFungibleToken = mockIssuedTokenType heldBy mockParty withHashingService mockHashingService

        assertEquals(mockIssuedTokenType, token.token)
        assertEquals(mockParty, token.holder)
    }

    @Test
    fun `Create transaction state for abstract token with a given notary`() {
        val mockToken = mockk<DummyToken>()
        val transactionState: TransactionState<AbstractToken> = mockToken withNotary mockNotary

        assertEquals(mockToken, transactionState.data)
        assertEquals(mockNotary, transactionState.notary)
    }

    @Test
    fun `Create transaction state for evolvable token type with a given notary`() {
        val mockEvolvableTokenType: DummyEvolvableToken = mockk()
        val transactionState: TransactionState<EvolvableTokenType> = mockEvolvableTokenType withNotary mockNotary

        assertEquals(mockEvolvableTokenType, transactionState.data)
        assertEquals(mockNotary, transactionState.notary)
    }
}


class TestContract : Contract {
    override fun verify(tx: LedgerTransaction) {}
}

@BelongsToContract(TestContract::class)
class DummyToken(
    override val holder: AbstractParty,
    override val issuedTokenType: IssuedTokenType,
    override val tokenTypeJarHash: SecureHash?
) : AbstractToken {
    override fun withNewHolder(newHolder: AbstractParty): AbstractToken {
        return DummyToken(newHolder, issuedTokenType, tokenTypeJarHash)
    }
}


@BelongsToContract(TestContract::class)
class DummyEvolvableToken(
    override val linearId: UniqueIdentifier,
    override val maintainers: List<Party>,
    override val fractionDigits: Int
) : EvolvableTokenType()