@file:JvmName("MoveTokensUtilities")

package com.r3.corda.lib.tokens.workflows.flows.move

import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.internal.selection.generateMoveNonFungible
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import com.r3.corda.lib.tokens.workflows.types.toPairs
import com.r3.corda.lib.tokens.workflows.utilities.addTokenTypeJar
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.node.MemberInfo
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.transactions.TransactionBuilder

/* For fungible tokens. */

/**
 * Adds a set of token moves to a transaction using specific inputs and outputs.
 */
@Suspendable
fun addMoveTokens(
    transactionBuilder: TransactionBuilder,
    inputs: List<StateAndRef<AbstractToken>>,
    outputs: List<AbstractToken>
): TransactionBuilder {
    val outputGroups: Map<IssuedTokenType, List<AbstractToken>> = outputs.groupBy { it.issuedTokenType }
    val inputGroups: Map<IssuedTokenType, List<StateAndRef<AbstractToken>>> = inputs.groupBy {
        it.state.data.issuedTokenType
    }

    check(outputGroups.keys == inputGroups.keys) {
        "Input and output token types must correspond to each other when moving tokensToIssue"
    }

    transactionBuilder.apply {
        // Add a notary to the transaction.
        // TODO: Deal with notary change.
        setNotary(inputs.map { it.state.notary }.toSet().single())
        outputGroups.forEach { (issuedTokenType: IssuedTokenType, outputStates: List<AbstractToken>) ->
            val inputGroup = inputGroups[issuedTokenType]
                ?: throw IllegalArgumentException("No corresponding inputs for the outputs issued token type: $issuedTokenType")
            val keys = inputGroup.map { it.state.data.holder.owningKey }

            var inputStartingIdx = this@apply.inputStates.size
            var outputStartingIdx = this@apply.outputStates.size

            val inputIdx = inputGroup.map {
                addInputState(it)
                inputStartingIdx++
            }

            val outputIdx = outputStates.map {
                addOutputState(it)
                outputStartingIdx++
            }

            addCommand(MoveTokenCommand(issuedTokenType, inputs = inputIdx, outputs = outputIdx), keys)
        }
    }

    addTokenTypeJar(inputs.map { it.state.data } + outputs, transactionBuilder)

    return transactionBuilder
}

/**
 * Adds a single token move to a transaction.
 */
@Suspendable
fun addMoveTokens(
    transactionBuilder: TransactionBuilder,
    input: StateAndRef<AbstractToken>,
    output: AbstractToken
): TransactionBuilder {
    return addMoveTokens(transactionBuilder = transactionBuilder, inputs = listOf(input), outputs = listOf(output))
}

/**
 * Adds multiple token moves to transaction. [partiesAndAmounts] parameter specify which parties should receive amounts of the token.
 * With possible change paid to [changeHolder]. This method will combine multiple token amounts from different issuers if needed.
 * Note: For now this method always uses database token selection, to use in memory one, use [addMoveTokens] with already selected
 * input and output states.
 */
@Suspendable
fun addMoveFungibleTokens(
    transactionBuilder: TransactionBuilder,
    persistenceService: PersistenceService,
    identityService: IdentityService,
    hashingService: HashingService,
    flowEngine: FlowEngine,
    memberInfo: MemberInfo,
    partiesAndAmounts: List<PartyAndAmount<TokenType>>,
    changeHolder: AbstractParty,
    queryBy: TokenQueryBy
): TransactionBuilder {
    // TODO For now default to database query, but switch this line on after we can change API in 2.0
//    val selector: Selector = ConfigSelection.getPreferredSelection(serviceHub)
    val selector = DatabaseTokenSelection(persistenceService, identityService, flowEngine)
    val (inputs, outputs) =
        selector.generateMove(
            identityService,
            hashingService,
            memberInfo,
            partiesAndAmounts.toPairs(),
            changeHolder,
            queryBy,
            transactionBuilder.lockId
        )
    return addMoveTokens(transactionBuilder = transactionBuilder, inputs = inputs, outputs = outputs)
}

@Suspendable
fun addMoveFungibleTokens(
    transactionBuilder: TransactionBuilder,
    persistenceService: PersistenceService,
    identityService: IdentityService,
    hashingService: HashingService,
    flowEngine: FlowEngine,
    memberInfo: MemberInfo,
    partiesAndAmounts: List<PartyAndAmount<TokenType>>,
    changeHolder: AbstractParty,
): TransactionBuilder {
    return addMoveFungibleTokens(
        transactionBuilder,
        persistenceService,
        identityService,
        hashingService,
        flowEngine,
        memberInfo,
        partiesAndAmounts,
        changeHolder,
        TokenQueryBy()
    )
}

/**
 * Add single move of [amount] of token to the new [holder]. Possible change output will be paid to [changeHolder].
 * This method will combine multiple token amounts from different issuers if needed.
 * Note: For now this method always uses database token selection, to use in memory one, use [addMoveTokens] with already selected
 * input and output states.
 */
@Suspendable
fun addMoveFungibleTokens(
    transactionBuilder: TransactionBuilder,
    persistenceService: PersistenceService,
    identityService: IdentityService,
    hashingService: HashingService,
    flowEngine: FlowEngine,
    memberInfo: MemberInfo,
    amount: Amount<TokenType>,
    holder: AbstractParty,
    changeHolder: AbstractParty,
    queryBy: TokenQueryBy
): TransactionBuilder {
    return addMoveFungibleTokens(
        transactionBuilder = transactionBuilder,
        persistenceService = persistenceService,
        identityService = identityService,
        flowEngine = flowEngine,
        memberInfo = memberInfo,
        partiesAndAmounts = listOf(PartyAndAmount(holder, amount)),
        changeHolder = changeHolder,
        hashingService = hashingService,
        queryBy = queryBy
    )
}

@Suspendable
fun addMoveFungibleTokens(
    transactionBuilder: TransactionBuilder,
    persistenceService: PersistenceService,
    identityService: IdentityService,
    hashingService: HashingService,
    flowEngine: FlowEngine,
    memberInfo: MemberInfo,
    amount: Amount<TokenType>,
    holder: AbstractParty,
    changeHolder: AbstractParty,
): TransactionBuilder {
    return addMoveFungibleTokens(
        transactionBuilder,
        persistenceService,
        identityService,
        hashingService,
        flowEngine,
        memberInfo,
        amount,
        holder,
        changeHolder,
        TokenQueryBy()
    )
}

/* For non-fungible tokens. */

/**
 * Add single move of [token] to the new [holder].
 */
@Suspendable
fun addMoveNonFungibleTokens(
    transactionBuilder: TransactionBuilder,
    persistenceService: PersistenceService,
    token: TokenType,
    holder: AbstractParty,
    queryBy: TokenQueryBy
): TransactionBuilder {
    return generateMoveNonFungible(transactionBuilder, PartyAndToken(holder, token), persistenceService, queryBy)
}

@Suspendable
fun addMoveNonFungibleTokens(
    transactionBuilder: TransactionBuilder,
    persistenceService: PersistenceService,
    token: TokenType,
    holder: AbstractParty,
): TransactionBuilder {
    return addMoveNonFungibleTokens(transactionBuilder, persistenceService, token, holder, TokenQueryBy())
}

/**
 * Add single move of token to the new holder specified using [partyAndToken] parameter.
 */
@Suspendable
fun addMoveNonFungibleTokens(
    transactionBuilder: TransactionBuilder,
    persistenceService: PersistenceService,
    partyAndToken: PartyAndToken,
    queryBy: TokenQueryBy
): TransactionBuilder {
    return generateMoveNonFungible(transactionBuilder, partyAndToken, persistenceService, queryBy)
}

@Suspendable
fun addMoveNonFungibleTokens(
    transactionBuilder: TransactionBuilder,
    persistenceService: PersistenceService,
    partyAndToken: PartyAndToken,
): TransactionBuilder {
    return addMoveNonFungibleTokens(transactionBuilder, persistenceService, partyAndToken, TokenQueryBy())
}