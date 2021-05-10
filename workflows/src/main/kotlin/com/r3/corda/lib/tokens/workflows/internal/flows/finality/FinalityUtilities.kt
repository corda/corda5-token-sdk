package com.r3.corda.lib.tokens.workflows.internal.flows.finality

import net.corda.v5.base.annotations.CordaSerializable

@CordaSerializable
enum class TransactionRole { PARTICIPANT, OBSERVER }