package com.r3.corda.lib.tokens.contracts.internal.schemas

import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.ledger.schemas.PersistentState
import net.corda.v5.persistence.MappedSchema
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.Index
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.Table

object FungibleTokenSchema

object FungibleTokenSchemaV1 : MappedSchema(
    schemaFamily = FungibleTokenSchema.javaClass,
    version = 1,
    mappedTypes = listOf(PersistentFungibleToken::class.java)
) {
    @Entity
    @CordaSerializable
    @Table(
        name = "fungible_token", indexes = [
            Index(name = "amount_idx", columnList = "amount"),
            Index(name = "held_token_amount_idx", columnList = "token_class, token_identifier"),
            Index(name = "holding_key_idx", columnList = "holding_key")
        ]
    )
    @NamedQueries(
        NamedQuery(
            name = "FungibleTokenSchemaV1.PersistentFungibleToken.findAllUnconsumed",
            query = "SELECT token" +
                " FROM com.r3.corda.lib.tokens.contracts.internal.schemas.FungibleTokenSchemaV1\$PersistentFungibleToken token," +
                " net.corda.v5.ledger.schemas.vault.VaultSchemaV1\$VaultState state" +
                " WHERE state.stateStatus = 0" +
                " AND state.stateRef.txId = token.stateRef.txId" +
                " AND state.stateRef.index = token.stateRef.index" +
                " ORDER BY token.stateRef.txId ASC, token.stateRef.index ASC"
        ),
        NamedQuery(
            name = "FungibleTokenSchemaV1.PersistentFungibleToken.findAllUnconsumedTokensByClassAndIdentifier",
            query = "SELECT token" +
                " FROM com.r3.corda.lib.tokens.contracts.internal.schemas.FungibleTokenSchemaV1\$PersistentFungibleToken token," +
                " net.corda.v5.ledger.schemas.vault.VaultSchemaV1\$VaultState state" +
                " WHERE state.stateStatus = 0" +
                " AND state.stateRef.txId = token.stateRef.txId" +
                " AND state.stateRef.index = token.stateRef.index" +
                " AND token.tokenClass = :tokenClass" +
                " AND token.tokenIdentifier = :tokenIdentifier"
        ),
        NamedQuery(
            name = "FungibleTokenSchemaV1.PersistentFungibleToken.findAllUnconsumedTokensByClassIdentifierAndHolder",
            query = "SELECT token" +
                    " FROM com.r3.corda.lib.tokens.contracts.internal.schemas.FungibleTokenSchemaV1\$PersistentFungibleToken token," +
                    " net.corda.v5.ledger.schemas.vault.VaultSchemaV1\$VaultState state" +
                    " WHERE state.stateStatus = 0" +
                    " AND state.stateRef.txId = token.stateRef.txId" +
                    " AND state.stateRef.index = token.stateRef.index" +
                    " AND token.tokenClass = :tokenClass" +
                    " AND token.tokenIdentifier = :tokenIdentifier" +
                    " AND token.holder = :holder"
        ),
        NamedQuery(
            name = "FungibleTokenSchemaV1.PersistentFungibleToken.findAllUnconsumedTokensByClassIdentifierAndOwningKey",
            query = "SELECT token" +
                    " FROM com.r3.corda.lib.tokens.contracts.internal.schemas.FungibleTokenSchemaV1\$PersistentFungibleToken token," +
                    " net.corda.v5.ledger.schemas.vault.VaultSchemaV1\$VaultState state" +
                    " WHERE state.stateStatus = 0" +
                    " AND state.stateRef.txId = token.stateRef.txId" +
                    " AND state.stateRef.index = token.stateRef.index" +
                    " AND token.tokenClass = :tokenClass" +
                    " AND token.tokenIdentifier = :tokenIdentifier" +
                    " AND token.owningKeyHash = :owningKeyHash"
        ),
        NamedQuery(
            name = "FungibleTokenSchemaV1.PersistentFungibleToken.findAllUnconsumedTokensByClassIdentifierAndIssuer",
            query = "SELECT token" +
                    " FROM com.r3.corda.lib.tokens.contracts.internal.schemas.FungibleTokenSchemaV1\$PersistentFungibleToken token," +
                    " net.corda.v5.ledger.schemas.vault.VaultSchemaV1\$VaultState state" +
                    " WHERE state.stateStatus = 0" +
                    " AND state.stateRef.txId = token.stateRef.txId" +
                    " AND state.stateRef.index = token.stateRef.index" +
                    " AND token.tokenClass = :tokenClass" +
                    " AND token.tokenIdentifier = :tokenIdentifier" +
                    " AND token.issuer = :issuer"
        ),
        NamedQuery(
            name = "FungibleTokenSchemaV1.PersistentFungibleToken.findAllUnconsumedTokensByClassIdentifierHolderAndIssuer",
            query = "SELECT token" +
                    " FROM com.r3.corda.lib.tokens.contracts.internal.schemas.FungibleTokenSchemaV1\$PersistentFungibleToken token," +
                    " net.corda.v5.ledger.schemas.vault.VaultSchemaV1\$VaultState state" +
                    " WHERE state.stateStatus = 0" +
                    " AND state.stateRef.txId = token.stateRef.txId" +
                    " AND state.stateRef.index = token.stateRef.index" +
                    " AND token.tokenClass = :tokenClass" +
                    " AND token.tokenIdentifier = :tokenIdentifier" +
                    " AND token.issuer = :issuer" +
                    " AND token.holder = :holder"
        ),
        NamedQuery(
            name = "FungibleTokenSchemaV1.PersistentFungibleToken.sumAllUnconsumedTokensByClassIdentifierAndIssuer",
            query = "SELECT sum(token.amount)" +
                    " FROM com.r3.corda.lib.tokens.contracts.internal.schemas.FungibleTokenSchemaV1\$PersistentFungibleToken token," +
                    " net.corda.v5.ledger.schemas.vault.VaultSchemaV1\$VaultState state" +
                    " WHERE state.stateStatus = 0" +
                    " AND state.stateRef.txId = token.stateRef.txId" +
                    " AND state.stateRef.index = token.stateRef.index" +
                    " AND token.tokenClass = :tokenClass" +
                    " AND token.tokenIdentifier = :tokenIdentifier" +
                    " AND token.issuer = :issuer"
        ),
        NamedQuery(
            name = "FungibleTokenSchemaV1.PersistentFungibleToken.sumAllUnconsumedTokensByClassAndIdentifier",
            query = "SELECT sum(token.amount)" +
                    " FROM com.r3.corda.lib.tokens.contracts.internal.schemas.FungibleTokenSchemaV1\$PersistentFungibleToken token," +
                    " net.corda.v5.ledger.schemas.vault.VaultSchemaV1\$VaultState state" +
                    " WHERE state.stateStatus = 0" +
                    " AND state.stateRef.txId = token.stateRef.txId" +
                    " AND state.stateRef.index = token.stateRef.index" +
                    " AND token.tokenClass = :tokenClass" +
                    " AND token.tokenIdentifier = :tokenIdentifier"
        ),
        NamedQuery(
            name = "FungibleTokenSchemaV1.PersistentFungibleToken.findAllUnconsumedTokensByClassIdentifierAndExternalId",
            query = "SELECT token" +
                    " FROM com.r3.corda.lib.tokens.contracts.internal.schemas.FungibleTokenSchemaV1\$PersistentFungibleToken token," +
                    " net.corda.v5.ledger.schemas.vault.VaultSchemaV1\$VaultState state," +
                    " net.corda.v5.ledger.schemas.vault.VaultSchemaV1\$StateToExternalId stateToExternalId" +
                    " WHERE state.stateStatus = 0" +
                    " AND state.stateRef.txId = token.stateRef.txId" +
                    " AND state.stateRef.index = token.stateRef.index" +
                    " AND token.tokenClass = :tokenClass" +
                    " AND token.tokenIdentifier = :tokenIdentifier" +
                    " AND stateToExternalId.compositeKey.stateRef.txId = token.stateRef.txId" +
                    " AND stateToExternalId.compositeKey.stateRef.index = token.stateRef.index" +
                    " AND stateToExternalId.externalId = :externalId"
        ),
        NamedQuery(
            name = "FungibleTokenSchemaV1.PersistentFungibleToken.findAllUnconsumedTokensByClassIdentifierIssuerAndExternalId",
            query = "SELECT token" +
                    " FROM com.r3.corda.lib.tokens.contracts.internal.schemas.FungibleTokenSchemaV1\$PersistentFungibleToken token," +
                    " net.corda.v5.ledger.schemas.vault.VaultSchemaV1\$VaultState state," +
                    " net.corda.v5.ledger.schemas.vault.VaultSchemaV1\$StateToExternalId stateToExternalId" +
                    " WHERE state.stateStatus = 0" +
                    " AND state.stateRef.txId = token.stateRef.txId" +
                    " AND state.stateRef.index = token.stateRef.index" +
                    " AND token.tokenClass = :tokenClass" +
                    " AND token.tokenIdentifier = :tokenIdentifier" +
                    " AND token.issuer = :issuer" +
                    " AND stateToExternalId.compositeKey.stateRef.txId = token.stateRef.txId" +
                    " AND stateToExternalId.compositeKey.stateRef.index = token.stateRef.index" +
                    " AND stateToExternalId.externalId = :externalId"
        )
    )
    class PersistentFungibleToken(
        @Column(name = "issuer", nullable = false)
        var issuer: Party,

        @Column(name = "holder")
        var holder: AbstractParty?,

        @Column(name = "amount", nullable = false)
        var amount: Long,

        // The fully qualified class name of the class which implements the token tokenType.
        // This is either a fixed token or a evolvable token.
        @Column(name = "token_class", nullable = false)
        @Convert(converter = TokenClassConverter::class)
        var tokenClass: Class<*>,

        // This can either be a symbol or a linearID depending on whether the token is evolvable or fixed.
        // Not all tokens will have identifiers if there is only one instance for a token class, for example.
        // It is expected that the combination of token_class and token_symbol will be enough to identity a unique
        // token.
        @Column(name = "token_identifier", nullable = true)
        var tokenIdentifier: String,

        @Column(name = "holding_key", nullable = true)
        val owningKeyHash: String?
    ) : PersistentState()
}
