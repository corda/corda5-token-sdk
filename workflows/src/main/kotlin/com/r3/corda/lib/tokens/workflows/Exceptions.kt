package com.r3.corda.lib.tokens.workflows

import net.corda.v5.application.flows.FlowException

/**
 * A dedicated exception for the TokenBuilder class.
 *
 * @param exceptionMessage The message to be included in the exception thrown.
 */
class TokenBuilderException(exceptionMessage: String): FlowException(exceptionMessage)