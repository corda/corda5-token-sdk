@file:JvmName("RedeemFlowUtilities")

package com.r3.corda.lib.tokens.workflows.flows.redeem

import com.r3.corda.lib.tokens.contracts.commands.RedeemTokenCommand
import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.sumTokenStateAndRefs
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.internal.checkSameIssuer
import com.r3.corda.lib.tokens.workflows.internal.checkSameNotary
import com.r3.corda.lib.tokens.workflows.internal.selection.generateExitNonFungible
import com.r3.corda.lib.tokens.workflows.utilities.addNotaryWithCheck
import com.r3.corda.lib.tokens.workflows.utilities.addTokenTypeJar
import com.r3.corda.lib.tokens.workflows.utilities.heldTokensByTokenIssuer
import com.r3.corda.lib.tokens.workflows.utilities.tokenAmountWithIssuerCriteria
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.seconds
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.transactions.TransactionBuilder

/**
 * Add redeeming of multiple [inputs] to the [transactionBuilder] with possible [changeOutput].
 */
@Suspendable
fun addTokensToRedeem(
    transactionBuilder: TransactionBuilder,
    inputs: List<StateAndRef<AbstractToken>>,
    changeOutput: AbstractToken?
): TransactionBuilder {
    checkSameIssuer(inputs, changeOutput?.issuer)
    checkSameNotary(inputs)
    if (changeOutput != null && changeOutput is FungibleToken) {
        check(inputs.filterIsInstance<StateAndRef<FungibleToken>>().sumTokenStateAndRefs() > changeOutput.amount) {
            "Change output should be less than sum of inputs."
        }
    }
    val firstState = inputs.first().state
    addNotaryWithCheck(transactionBuilder, firstState.notary)
    val issuerKey = firstState.data.issuer.owningKey
    val moveKeys = inputs.map { it.state.data.holder.owningKey }

    var inputIdx = transactionBuilder.inputStates.size
    val outputIdx = transactionBuilder.outputStates.size
    transactionBuilder.apply {
        val inputIndicies = inputs.map {
            addInputState(it)
            inputIdx++
        }
        val outputs = if (changeOutput != null) {
            addOutputState(changeOutput)
            listOf(outputIdx)
        } else {
            emptyList()
        }
        addCommand(RedeemTokenCommand(firstState.data.issuedTokenType, inputIndicies, outputs), moveKeys + issuerKey)
    }
    val states = inputs.map { it.state.data } + if (changeOutput == null) emptyList() else listOf(changeOutput)
    addTokenTypeJar(states, transactionBuilder)
    return transactionBuilder
}

@Suspendable
fun addTokensToRedeem(
    transactionBuilder: TransactionBuilder,
    inputs: List<StateAndRef<AbstractToken>>,
) = addTokensToRedeem(transactionBuilder, inputs, null)


/**
 * Redeem non-fungible [heldToken] issued by the [issuer] and add it to the [transactionBuilder].
 */
@Suspendable
fun addNonFungibleTokensToRedeem(
    transactionBuilder: TransactionBuilder,
    persistenceService: PersistenceService,
    heldToken: TokenType,
    issuer: Party
): TransactionBuilder {
    val cursor = persistenceService.heldTokensByTokenIssuer(heldToken, issuer)
    val heldTokenStateAndRef = mutableListOf<StateAndRef<NonFungibleToken>>()
    do {
        val pollResult = cursor.poll(10, 5.seconds)
        heldTokenStateAndRef.addAll(pollResult.values)
    } while (!pollResult.isLastResult)
    check(heldTokenStateAndRef.size == 1) {
        "Exactly one held token of a particular type $heldToken should be in the vault at any one time."
    }
    val nonFungibleState = heldTokenStateAndRef.first()
    addNotaryWithCheck(transactionBuilder, nonFungibleState.state.notary)
    generateExitNonFungible(transactionBuilder, nonFungibleState)
    return transactionBuilder
}

/**
 * Redeem amount of certain type of the token issued by [issuer]. Pay possible change to the [changeHolder] - it can be confidential identity.
 */
@Suspendable
fun addFungibleTokensToRedeem(
    transactionBuilder: TransactionBuilder,
    persistenceService: PersistenceService,
    identityService: IdentityService,
    flowEngine: FlowEngine,
    amount: Amount<TokenType>,
    issuer: Party,
    changeHolder: AbstractParty,
): TransactionBuilder {
    // TODO For now default to database query, but switch this line on after we can change API in 2.0
    val selector = DatabaseTokenSelection(persistenceService, identityService, flowEngine)
    val fungibleStates =
        selector.selectTokens(amount, transactionBuilder.lockId, TokenQueryBy(issuer = issuer))
    checkSameNotary(fungibleStates)
    check(fungibleStates.isNotEmpty()) {
        "Received empty list of states to redeem."
    }
    val notary = fungibleStates.first().state.notary
    addNotaryWithCheck(transactionBuilder, notary)
    val (exitStates, change) = selector.generateExit(
        exitStates = fungibleStates,
        amount = amount,
        changeHolder = changeHolder
    )

    addTokensToRedeem(transactionBuilder, exitStates, change)
    return transactionBuilder
}
