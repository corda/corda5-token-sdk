package com.r3.corda.lib.tokens.workflows.internal.schemas

import net.corda.v5.application.identity.Party
import net.corda.v5.application.node.services.persistence.MappedSchema
import net.corda.v5.base.annotations.CordaSerializable
import org.hibernate.annotations.Type
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.Table
import javax.persistence.Index
import javax.persistence.Id
import javax.persistence.GeneratedValue
import javax.persistence.Column


object DistributionRecordSchema

object DistributionRecordSchemaV1 : MappedSchema(
    schemaFamily = DistributionRecordSchema.javaClass,
    version = 1,
    mappedTypes = listOf(DistributionRecord::class.java)
) {

    @CordaSerializable
    @Entity
    @Table(name = "distribution_record", indexes = [Index(name = "dist_record_idx", columnList = "linear_id")])
    @NamedQueries(
        NamedQuery(
            name = "DistributionRecord.findByLinearId",
            query = "FROM com.r3.corda.lib.tokens.workflows.internal.schemas.DistributionRecordSchemaV1\$DistributionRecord " +
                    "WHERE linearId = :linearId"
        ),
        NamedQuery(
            name = "DistributionRecord.findByLinearIdAndParty",
            query = "FROM com.r3.corda.lib.tokens.workflows.internal.schemas.DistributionRecordSchemaV1\$DistributionRecord " +
                    "WHERE linearId = :linearId AND party = :party"
        )
    )
    class DistributionRecord(

        @Id
        @GeneratedValue
        var id: Long,

        @Column(name = "linear_id", nullable = false)
        @Type(type = "uuid-char")
        var linearId: UUID,

        @Column(name = "party", nullable = false)
        var party: Party

    ) {
        constructor(linearId: UUID, party: Party) : this(0, linearId, party)
    }
}