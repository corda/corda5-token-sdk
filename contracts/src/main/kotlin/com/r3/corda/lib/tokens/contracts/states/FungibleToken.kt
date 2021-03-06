package com.r3.corda.lib.tokens.contracts.states

import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.internal.schemas.FungibleTokenSchemaV1
import com.r3.corda.lib.tokens.contracts.internal.schemas.FungibleTokenSchemaV1.PersistentFungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.getAttachmentIdForGenericParam
import com.r3.corda.lib.tokens.contracts.utilities.holderString
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.crypto.SecureHash
import net.corda.v5.crypto.toStringShort
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.BelongsToContract
import net.corda.v5.ledger.contracts.FungibleState
import net.corda.v5.ledger.schemas.PersistentState
import net.corda.v5.ledger.schemas.QueryableState
import net.corda.v5.persistence.MappedSchema

/**
 * This class is for handling the issuer and holder relationship for fungible token types. If the [TokenType] is a
 * [TokenPointer], then the token can evolve independently of who owns it. Otherwise, the token definition is inlined
 * into the [FungibleToken] state. This state object implements [FungibleState] as the expectation is that it contains
 * [Amount]s of an [IssuedTokenType] which can be split and merged. All [TokenType]s are wrapped with an [IssuedTokenType]
 * class to add the issuer party, this is necessary so that the [FungibleToken] represents an agreement between an issuer
 * of the [IssuedTokenType] and a holder of the [IssuedTokenType]. In effect, the [FungibleToken] conveys a right for the
 * holder to make a claim on the issuer for whatever the [IssuedTokenType] represents. The class is defined as open, so
 * it can be extended to add new functionality, like a whitelisted token, for example.
 *
 * @property amount the [Amount] of [IssuedTokenType] represented by this [FungibleToken].
 * @property holder the [AbstractParty] which has a claim on the issuer of the [IssuedTokenType].
 */
@BelongsToContract(FungibleTokenContract::class)
open class FungibleToken (
    override val amount: Amount<IssuedTokenType>,
    override val holder: AbstractParty,
    override val tokenTypeJarHash: SecureHash?
) : FungibleState<IssuedTokenType>, AbstractToken, QueryableState {

    constructor(
        amount: Amount<IssuedTokenType>,
        holder: AbstractParty,
        hashingService: HashingService
    ) : this(amount, holder, amount.token.tokenType.getAttachmentIdForGenericParam(hashingService))

    override val tokenType: TokenType get() = amount.token.tokenType

    override val issuedTokenType: IssuedTokenType get() = amount.token

    override val issuer: Party get() = amount.token.issuer

    override fun toString(): String = "$amount held by $holderString"

    override fun withNewHolder(newHolder: AbstractParty): FungibleToken {
        return FungibleToken(amount = amount, holder = newHolder, tokenTypeJarHash = tokenTypeJarHash)
    }

    override fun generateMappedObject(schema: MappedSchema): PersistentState = when (schema) {
        is FungibleTokenSchemaV1 -> PersistentFungibleToken(
            issuer = amount.token.issuer,
            holder = holder,
            amount = amount.quantity,
            tokenClass = amount.token.tokenType.tokenClass,
            tokenIdentifier = amount.token.tokenType.tokenIdentifier,
            owningKeyHash = holder.owningKey.toStringShort()
        )
        else -> throw IllegalArgumentException("Unrecognised schema $schema")
    }

    override fun supportedSchemas() = listOf(FungibleTokenSchemaV1)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FungibleToken
        if (amount != other.amount) return false
        if (holder != other.holder) return false
        if (tokenTypeJarHash != other.tokenTypeJarHash) return false

        return true
    }

    override fun hashCode(): Int {
        var result = amount.hashCode()
        result = 31 * result + holder.hashCode()
        result = 31 * result + (tokenTypeJarHash?.hashCode() ?: 0)
        return result
    }
}