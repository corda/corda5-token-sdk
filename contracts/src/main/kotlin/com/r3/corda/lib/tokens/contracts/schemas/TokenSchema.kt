package com.r3.corda.lib.tokens.contracts.schemas

import net.corda.v5.application.node.services.persistence.MappedSchema

/**
 * Here, schemas can be added for commonly used [EvolvableTokenType]s.
 */
object TokenSchema

object TokenSchemaV1 : MappedSchema(
        schemaFamily = TokenSchema.javaClass,
        version = 1,
        mappedTypes = listOf()
)
