package com.r3.corda.lib.tokens.workflows.flows.redeem

import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.transactions.TransactionBuilder

/**
 * General inlined flow used to redeem any type of tokens with the issuer. Should be called on tokens' owner side.
 * Notice that token selection and change output generation should be done beforehand. This flow builds a transaction
 * containing those states, but all checks should have be done before calling this flow as a subflow.
 * It can only be called for one [TokenType] at a time. If you need to do multiple token types in one transaction then create a new
 * flow, calling [addTokensToRedeem] for each token type.
 *
 * @param inputs list of token inputs to redeem
 * @param changeOutput possible change output to be paid back to the tokens owner
 * @param issuerSession session with the issuer of the tokens
 * @param observerSessions session with optional observers of the redeem transaction
 */
// Called on owner side.
class RedeemTokensFlow (
    val inputs: List<StateAndRef<AbstractToken>>,
    val changeOutput: AbstractToken?,
    override val issuerSession: FlowSession,
    override val observerSessions: List<FlowSession>
) : AbstractRedeemTokensFlow() {

    constructor(
        inputs: List<StateAndRef<AbstractToken>>,
        changeOutput: AbstractToken?,
        issuerSession: FlowSession,
    ) : this(inputs, changeOutput, issuerSession, emptyList())

    @Suspendable
    override fun generateExit(transactionBuilder: TransactionBuilder) {
        addTokensToRedeem(transactionBuilder, inputs, changeOutput)
    }
}
