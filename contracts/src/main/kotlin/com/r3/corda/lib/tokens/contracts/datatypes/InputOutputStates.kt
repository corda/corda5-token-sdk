package com.r3.corda.lib.tokens.contracts.datatypes

import net.corda.v5.ledger.contracts.ContractState
import net.corda.v5.ledger.contracts.StateAndRef

/**
 * Simple data structure to hold an input to output state pair.
 */
data class InputOutputStates<T : ContractState>(
    val input: List<StateAndRef<T>>,
    val output: List<T>
) {
    constructor(input: StateAndRef<T>, output: T) : this(listOf(input), listOf(output))
}