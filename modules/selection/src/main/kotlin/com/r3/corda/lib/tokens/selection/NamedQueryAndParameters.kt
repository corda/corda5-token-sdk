package com.r3.corda.lib.tokens.selection

/**
 * Simple data class to hold a named query string, and a map of parameters for that named query.
 */
data class NamedQueryAndParameters (
    val namedQuery: String,
    val queryParams: Map<String, Any>,
)