package com.r3.corda.lib.tokens.contracts.states

import com.r3.corda.lib.tokens.contracts.NonFungibleTokenContract
import com.r3.corda.lib.tokens.contracts.internal.schemas.NonFungibleTokenSchemaV1
import com.r3.corda.lib.tokens.contracts.internal.schemas.NonFungibleTokenSchemaV1.PersistentNonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.getAttachmentIdForGenericParam
import com.r3.corda.lib.tokens.contracts.utilities.holderString
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.crypto.SecureHash
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.BelongsToContract
import net.corda.v5.ledger.contracts.LinearState
import net.corda.v5.ledger.schemas.PersistentState
import net.corda.v5.ledger.schemas.QueryableState
import net.corda.v5.persistence.MappedSchema

/**
 * This class is for handling the issuer and holder relationship for non-fungible token types. Non-fungible tokens
 * cannot be split and merged, as they are considered unique at the ledger level. If the [TokenType] is a
 * [TokenPointer], then the token can evolve independently of who holds it. Otherwise, the [TokenType] is in-lined into
 * the [NonFungibleToken] and it cannot change. There is no [Amount] property in this class, as the assumption is there
 * is only ever ONE of the [IssuedTokenType] provided. It is up to issuers to ensure that only ONE of a non-fungible
 * token ever issued. All [TokenType]s are wrapped with an [IssuedTokenType] class to add the issuer [Party]. This is
 * necessary so that the [NonFungibleToken] represents an agreement between the issuer and holder. In effect, the
 * [NonFungibleToken] conveys a right for the holder to make a claim on the issuer for whatever the [IssuedTokenType]
 * represents. [NonFungibleToken] is open, so it can be extended to allow for additional functionality, if necessary.
 *
 * @property token the [IssuedTokenType] which this [NonFungibleToken] is in respect of.
 * @property holder the [AbstractParty] which holds the [IssuedTokenType].
 * @property linearId the [UniqueIdentifier] which will uniquely identify this Token.
 * @property tokenTypeJarHash the [SecureHash] which will pin the jar that provides the [TokenType].
 * @param TokenType the [TokenType].
 */
@BelongsToContract(NonFungibleTokenContract::class)
open class NonFungibleToken (
    val token: IssuedTokenType,
    override val holder: AbstractParty,
    override val linearId: UniqueIdentifier,
    override val tokenTypeJarHash: SecureHash?
) : AbstractToken, QueryableState, LinearState {

    constructor(
        token: IssuedTokenType,
        holder: AbstractParty,
        linearId: UniqueIdentifier,
        hashingService: HashingService
    ) : this(token, holder, linearId, token.tokenType.getAttachmentIdForGenericParam(hashingService))

    override val issuedTokenType: IssuedTokenType get() = token

    final override val tokenType: TokenType get() = token.tokenType

    override val issuer: Party get() = token.issuer

    override fun toString(): String = "$token held by $holderString"

    override fun withNewHolder(newHolder: AbstractParty): NonFungibleToken {
        return NonFungibleToken(token = token, holder = newHolder, linearId = linearId, tokenTypeJarHash = tokenTypeJarHash)
    }

    override fun generateMappedObject(schema: MappedSchema): PersistentState = when (schema) {
        is NonFungibleTokenSchemaV1 -> PersistentNonFungibleToken(
            issuer = token.issuer,
            holder = holder,
            tokenClass = token.tokenType.tokenClass,
            tokenIdentifier = token.tokenType.tokenIdentifier
        )
        else -> throw IllegalArgumentException("Unrecognised schema $schema")
    }

    override fun supportedSchemas() = listOf(NonFungibleTokenSchemaV1)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NonFungibleToken

        if (token != other.token) return false
        if (holder != other.holder) return false
        if (linearId != other.linearId) return false
        if (tokenTypeJarHash != other.tokenTypeJarHash) return false

        return true
    }

    override fun hashCode(): Int {
        var result = token.hashCode()
        result = 31 * result + holder.hashCode()
        result = 31 * result + linearId.hashCode()
        result = 31 * result + (tokenTypeJarHash?.hashCode() ?: 0)
        return result
    }
}