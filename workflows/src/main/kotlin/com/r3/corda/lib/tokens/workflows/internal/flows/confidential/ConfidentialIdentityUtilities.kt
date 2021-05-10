package com.r3.corda.lib.tokens.workflows.internal.flows.confidential

import net.corda.v5.base.annotations.CordaSerializable

@CordaSerializable
enum class ActionRequest { DO_NOTHING, CREATE_NEW_KEY }

