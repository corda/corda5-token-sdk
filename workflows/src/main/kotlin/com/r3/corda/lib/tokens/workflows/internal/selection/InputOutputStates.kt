package com.r3.corda.lib.tokens.workflows.internal.selection

import net.corda.v5.ledger.contracts.ContractState
import net.corda.v5.ledger.contracts.StateAndRef

/**
 * Simple data structure to hold an input to output state pair.
 */
data class InputOutputStates<T : ContractState>(
    val input: StateAndRef<T>,
    val output: T
)