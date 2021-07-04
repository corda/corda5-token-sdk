package com.r3.corda.lib.tokens.workflows.internal.selection

import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.RedeemTokenCommand
import com.r3.corda.lib.tokens.contracts.datatypes.InputOutputStates
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import com.r3.corda.lib.tokens.workflows.utilities.addNotaryWithCheck
import com.r3.corda.lib.tokens.workflows.utilities.addTokenTypeJar
import com.r3.corda.lib.tokens.workflows.utilities.namedQueryForNonfungibleTokenClassAndIdentifier
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.seconds
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.services.vault.IdentityStateAndRefPostProcessor
import net.corda.v5.ledger.transactions.TransactionBuilder

@Suspendable
fun generateMoveNonFungible(
    partyAndToken: PartyAndToken,
    persistenceService: PersistenceService,
): InputOutputStates<NonFungibleToken> {
    val (namedQuery, params) = namedQueryForNonfungibleTokenClassAndIdentifier(partyAndToken.token)
    val cursor = persistenceService.query<StateAndRef<NonFungibleToken>>(
        namedQuery,
        params,
        IdentityStateAndRefPostProcessor.POST_PROCESSOR_NAME,
    )
    val nonFungibleTokens = mutableListOf<StateAndRef<NonFungibleToken>>()
    do {
        val pollResult = cursor.poll(50, 5.seconds)
        nonFungibleTokens.addAll(pollResult.values)
    } while (!pollResult.isLastResult)
    // There can be multiple non-fungible tokens of the same TokenType. E.g. There can be multiple House tokens, each
    // with a different address. Whilst they have the same TokenType, they are still non-fungible. Therefore care must
    // be taken to ensure that only one token is returned for each query. As non-fungible tokens are also LinearStates,
    // the linearID can be used to ensure you only get one result.
    require(nonFungibleTokens.isNotEmpty()) { "Your query returned no non-fungible tokens." }
    require(nonFungibleTokens.size == 1) { "Your query wasn't specific enough and returned multiple non-fungible tokens." }
    val input = nonFungibleTokens.single()
    val nonFungibleState = input.state.data
    val output = nonFungibleState.withNewHolder(partyAndToken.party)
    return InputOutputStates(input, output)
}

@Suspendable
fun generateMoveNonFungible(
    transactionBuilder: TransactionBuilder,
    partyAndToken: PartyAndToken,
    persistenceService: PersistenceService,
): TransactionBuilder {
    val (inputs, outputs) = generateMoveNonFungible(partyAndToken, persistenceService)
    val input = inputs.single()
    val output = outputs.single()
    val notary = input.state.notary
    addTokenTypeJar(listOf(input.state.data, output), transactionBuilder)
    addNotaryWithCheck(transactionBuilder, notary)
    val signingKey = input.state.data.holder.owningKey

    return transactionBuilder.apply {
        val currentInputSize = inputStates.size
        val currentOutputSize = outputStates.size
        addInputState(input)
        addOutputState(state = output withNotary notary)
        addCommand(MoveTokenCommand(output.token, inputs = listOf(currentInputSize), outputs = listOf(currentOutputSize)), signingKey)
    }
}

// All check should be performed before.
@Suspendable
fun generateExitNonFungible(txBuilder: TransactionBuilder, moveStateAndRef: StateAndRef<NonFungibleToken>) {
    val nonFungibleToken = moveStateAndRef.state.data // TODO What if redeeming many non-fungible assets.
    addTokenTypeJar(nonFungibleToken, txBuilder)
    val issuerKey = nonFungibleToken.token.issuer.owningKey
    val moveKey = nonFungibleToken.holder.owningKey
    txBuilder.apply {
        val currentInputSize = inputStates.size
        addInputState(moveStateAndRef)
        addCommand(RedeemTokenCommand(nonFungibleToken.token, listOf(currentInputSize)), issuerKey, moveKey)
    }
}