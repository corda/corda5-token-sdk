package com.r3.corda.lib.tokens.selection.api

import com.r3.corda.lib.tokens.contracts.datatypes.InputOutputStates
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.sumTokenStateAndRefs
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import com.r3.corda.lib.tokens.selection.memory.internal.Holder
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.node.MemberInfo
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.Amount.Companion.sumOrThrow
import net.corda.v5.ledger.contracts.StateAndRef
import java.security.PublicKey
import java.util.*

/**
 * Interface that provides fungible state selection methods in flow.
 */
abstract class Selector {

    /**
     * Select [FungibleToken]s that cover [requiredAmount]. Notice that this
     * function doesn't calculate change. If query criteria is not specified then only held token amounts are used.
     *
     * Set [TokenQueryBy.issuer] to specify issuer.
     * Calling selectTokens multiple time with the same lockId will return next unlocked states.
     *
     * @param lockId id used to lock the states for spend. Should be the state machine run ID. Can be provided by [FlowEngine]
     *      or [TransactionBuilder]
     * @param requiredAmount amount that should be spent
     * @param queryBy narrows down tokens to spend, see [TokenQueryBy]
     * @return List of [FungibleToken]s that satisfy the amount to spend, empty list if none found.
     */
    // Token Only
    @Suspendable
    fun selectTokens(
        requiredAmount: Amount<TokenType>,
        queryBy: TokenQueryBy,
        lockId: UUID,
    ): List<StateAndRef<FungibleToken>> {
        return selectTokens(Holder.TokenOnly(), lockId, requiredAmount, queryBy)
    }

    @Suspendable
    fun selectTokens(
        requiredAmount: Amount<TokenType>,
        lockId: UUID,
    ): List<StateAndRef<FungibleToken>> {
        return selectTokens(requiredAmount, TokenQueryBy(), lockId)
    }

    /**
     * Select [FungibleToken]s that cover [requiredAmount] from keys associated with [externalId]. Notice that this
     * function doesn't calculate change. If query criteria is not specified then only held token amounts are used.
     *
     * Set [TokenQueryBy.issuer] to specify issuer.
     * Calling selectTokens multiple time with the same lockId will return next unlocked states.
     *
     * @param externalId external id that states should be selected from
     * @param lockId id used to lock the states for spend
     * @param requiredAmount amount that should be spent
     * @param queryBy narrows down tokens to spend, see [TokenQueryBy]
     * @return List of [FungibleToken]s that satisfy the amount to spend, empty list if none found.
     */
    // From external id
    @Suspendable
    fun selectTokens(
        externalId: UUID,
        requiredAmount: Amount<TokenType>,
        queryBy: TokenQueryBy,
        lockId: UUID
    ): List<StateAndRef<FungibleToken>> {
        return selectTokens(Holder.fromUUID(externalId), lockId, requiredAmount, queryBy)
    }

    @Suspendable
    fun selectTokens(
        externalId: UUID,
        requiredAmount: Amount<TokenType>,
        lockId: UUID,
    ): List<StateAndRef<FungibleToken>> {
        return selectTokens(externalId, requiredAmount, TokenQueryBy(), lockId)
    }

    /**
     * Select [FungibleToken]s that cover [requiredAmount] that belong to [holdingKey]. Notice that this
     * function doesn't calculate change. If query criteria is not specified then only held token amounts are used.
     *
     * Set [TokenQueryBy.issuer] to specify issuer.
     * Calling selectTokens multiple time with the same lockId will return next unlocked states.
     *
     * @param holdingKey key that holds the states to select
     * @param lockId id used to lock the states for spend
     * @param requiredAmount amount that should be spent
     * @param queryBy narrows down tokens to spend, see [TokenQueryBy]
     * @return List of [FungibleToken]s that satisfy the amount to spend, empty list if none found.
     */
    // From a given key
    @Suspendable
    fun selectTokens(
        holdingKey: PublicKey,
        requiredAmount: Amount<TokenType>,
        queryBy: TokenQueryBy,
        lockId: UUID
    ): List<StateAndRef<FungibleToken>> {
        return selectTokens(Holder.KeyIdentity(holdingKey), lockId, requiredAmount, queryBy)
    }

    @Suspendable
    fun selectTokens(
        holdingKey: PublicKey,
        requiredAmount: Amount<TokenType>,
        lockId: UUID,
    ): List<StateAndRef<FungibleToken>> {
        return selectTokens(holdingKey, requiredAmount, TokenQueryBy(), lockId)
    }

    /**
     * Generate move of [FungibleToken] T to tokenHolders specified in [PartyAndAmount]. Each party will receive amount
     * defined by [partyAndAmounts]. If [queryBy] is not specified then only held token amounts are used. Set
     * [TokenQueryBy.issuer] to specify issuer.
     *
     * @return Pair of lists, one for [FungibleToken]s that satisfy the amount to spend, empty list if none found, second
     *  for output states with possible change.
     */
    // Token only
    @Suspendable
    fun generateMove(
        identityService: IdentityService,
        hashingService: HashingService,
        memberInfo: MemberInfo,
        partiesAndAmounts: List<Pair<AbstractParty, Amount<TokenType>>>,
        changeHolder: AbstractParty,
        queryBy: TokenQueryBy,
        lockId: UUID
    ): InputOutputStates<FungibleToken> {
        return generateMove(
            identityService,
            hashingService,
            memberInfo,
            Holder.TokenOnly(),
            lockId,
            partiesAndAmounts,
            changeHolder,
            queryBy
        )
    }

    @Suspendable
    fun generateMove(
        identityService: IdentityService,
        hashingService: HashingService,
        memberInfo: MemberInfo,
        partiesAndAmounts: List<Pair<AbstractParty, Amount<TokenType>>>,
        changeHolder: AbstractParty,
        lockId: UUID
    ): InputOutputStates<FungibleToken> {
        return generateMove(identityService, hashingService, memberInfo, partiesAndAmounts, changeHolder, TokenQueryBy(), lockId)
    }

    /**
     * Generate move of [FungibleToken] T to tokenHolders specified in [PartyAndAmount] from given [externalId]. Each party will receive amount
     * defined by [partyAndAmounts]. If [queryBy] is not specified then only held token amounts are used. Set
     * [TokenQueryBy.issuer] to specify issuer.
     *
     * @return Pair of lists, one for [FungibleToken]s that satisfy the amount to spend, empty list if none found, second
     *  for output states with possible change.
     */
    // External id
    @Suspendable
    fun generateMove(
        identityService: IdentityService,
        hashingService: HashingService,
        memberInfo: MemberInfo,
        externalId: UUID,
        partiesAndAmounts: List<Pair<AbstractParty, Amount<TokenType>>>,
        changeHolder: AbstractParty,
        queryBy: TokenQueryBy,
        lockId: UUID
    ): InputOutputStates<FungibleToken> {
        return generateMove(
            identityService,
            hashingService,
            memberInfo,
            Holder.fromUUID(externalId),
            lockId,
            partiesAndAmounts,
            changeHolder,
            queryBy
        )
    }

    @Suspendable
    fun generateMove(
        identityService: IdentityService,
        hashingService: HashingService,
        memberInfo: MemberInfo,
        externalId: UUID,
        partiesAndAmounts: List<Pair<AbstractParty, Amount<TokenType>>>,
        changeHolder: AbstractParty,
        lockId: UUID
    ): InputOutputStates<FungibleToken> {
        return generateMove(
            identityService,
            hashingService,
            memberInfo,
            externalId,
            partiesAndAmounts,
            changeHolder,
            TokenQueryBy(),
            lockId
        )
    }

    /**
     * Generate move of [FungibleToken] T to tokenHolders specified in [PartyAndAmount] from given [publicKey]. Each party will receive amount
     * defined by [partyAndAmounts]. If [queryBy] is not specified then only held token amounts are used. Set
     * [TokenQueryBy.issuer] to specify issuer.
     *
     * @return Pair of lists, one for [FungibleToken]s that satisfy the amount to spend, empty list if none found, second
     *  for output states with possible change.
     */
    // From public key
    @Suspendable
    fun generateMove(
        identityService: IdentityService,
        hashingService: HashingService,
        memberInfo: MemberInfo,
        holdingKey: PublicKey,
        partiesAndAmounts: List<Pair<AbstractParty, Amount<TokenType>>>,
        changeHolder: AbstractParty,
        queryBy: TokenQueryBy,
        lockId: UUID
    ): InputOutputStates<FungibleToken> {
        return generateMove(
            identityService,
            hashingService,
            memberInfo,
            Holder.KeyIdentity(holdingKey),
            lockId,
            partiesAndAmounts,
            changeHolder,
            queryBy
        )
    }

    @Suspendable
    fun generateMove(
        identityService: IdentityService,
        hashingService: HashingService,
        memberInfo: MemberInfo,
        holdingKey: PublicKey,
        partiesAndAmounts: List<Pair<AbstractParty, Amount<TokenType>>>,
        changeHolder: AbstractParty,
        lockId: UUID
    ): InputOutputStates<FungibleToken> {
        return generateMove(
            identityService,
            hashingService,
            memberInfo,
            holdingKey,
            partiesAndAmounts,
            changeHolder,
            TokenQueryBy(),
            lockId
        )
    }

    @Suspendable
    protected abstract fun selectTokens(
        holder: Holder,
        lockId: UUID,
        requiredAmount: Amount<TokenType>,
        queryBy: TokenQueryBy
    ): List<StateAndRef<FungibleToken>>

    @Suspendable
    private fun generateMove(
        identityService: IdentityService,
        hashingService: HashingService,
        memberInfo: MemberInfo,
        holder: Holder,
        lockId: UUID,
        partiesAndAmounts: List<Pair<AbstractParty, Amount<TokenType>>>,
        changeHolder: AbstractParty,
        queryBy: TokenQueryBy
    ): InputOutputStates<FungibleToken> {
        // Grab some tokens from the vault and soft-lock.
        // Only supports moves of the same token instance currently.
        // TODO Support spends for different token types, different instances of the same type.
        // The way to do this will be to perform a query for each token type. If there are multiple token types then
        // just do all the below however many times is necessary.
        val totalRequired = partiesAndAmounts.map { it.second }.sumOrThrow()
        val acceptableStates = selectTokens(holder, lockId, totalRequired, queryBy) // call private version
        require(acceptableStates.isNotEmpty()) {
            "No states matching given criteria to generate move."
        }

        // Check that the change identity belongs to the node that called generateMove.
        val ownerId = identityService.partyFromAnonymous(changeHolder)
        check(ownerId != null && ownerId.owningKey in memberInfo.identityKeys) {
            "Owner of the change: $changeHolder is not the identity that belongs to the node."
        }

        // Now calculate the output states. This is complicated by the fact that a single payment may require
        // multiple output states, due to the need to keep states separated by issuer. We start by figuring out
        // how much we've gathered for each issuer: this map will keep track of how much we've used from each
        // as we work our way through the payments.
        val tokensGroupedByIssuer = acceptableStates.groupBy { it.state.data.amount.token }
        val remainingTokensFromEachIssuer = tokensGroupedByIssuer.mapValues { (_, value) ->
            value.map { (state) -> state.data.amount }.sumOrThrow()
        }.toList().toMutableList()

        // TODO: This assumes there is only ever ONE notary. In the future we need to deal with notary change.
        check(acceptableStates.map { it.state.notary }.toSet().size == 1) {
            "States selected have different notaries. For now we don't support notary change, it should be performed beforehand."
        }

        // Calculate the list of output states making sure that the
        val outputStates = mutableListOf<FungibleToken>()
        for ((party, paymentAmount) in partiesAndAmounts) {
            var remainingToPay = paymentAmount.quantity
            while (remainingToPay > 0) {
                val (token, remainingFromCurrentIssuer) = remainingTokensFromEachIssuer.last()
                val delta = remainingFromCurrentIssuer.quantity - remainingToPay
                when {
                    delta > 0 -> {
                        // The states from the current issuer more than covers this payment.
                        outputStates += FungibleToken(Amount(remainingToPay, token), party, hashingService)
                        remainingTokensFromEachIssuer[remainingTokensFromEachIssuer.lastIndex] =
                            Pair(token, Amount(delta, token))
                        remainingToPay = 0
                    }
                    delta == 0L -> {
                        // The states from the current issuer exactly covers this payment.
                        outputStates += FungibleToken(Amount(remainingToPay, token), party, hashingService)
                        remainingTokensFromEachIssuer.removeAt(remainingTokensFromEachIssuer.lastIndex)
                        remainingToPay = 0
                    }
                    delta < 0 -> {
                        // The states from the current issuer don't cover this payment, so we'll have to use >1 output
                        // state to cover this payment.
                        outputStates += FungibleToken(remainingFromCurrentIssuer, party, hashingService)
                        remainingTokensFromEachIssuer.removeAt(remainingTokensFromEachIssuer.lastIndex)
                        remainingToPay -= remainingFromCurrentIssuer.quantity
                    }
                }
            }
        }

        // Generate the change states.
        remainingTokensFromEachIssuer.forEach { (_, amount) ->
            outputStates += FungibleToken(amount, changeHolder, hashingService)
        }

        return InputOutputStates(acceptableStates, outputStates)
    }

    @Suspendable
    private fun generateMove(
        identityService: IdentityService,
        hashingService: HashingService,
        memberInfo: MemberInfo,
        holder: Holder,
        lockId: UUID,
        partiesAndAmounts: List<Pair<AbstractParty, Amount<TokenType>>>,
        changeHolder: AbstractParty,
    ): InputOutputStates<FungibleToken> {
        return generateMove(identityService, hashingService, memberInfo, holder, lockId, partiesAndAmounts, changeHolder, TokenQueryBy())
    }

    /**
     * Generate exit of [FungibleToken]s specified by [exitStates] up to given [amount]. Possible change will be paid to
     * [changeHolder]. All checks for exit states should have been done before. For example we assume that existStates have same issuer.
     *
     * @return Pair of list of [FungibleToken] inputs that satisfy the amount to exit and change output.
     */
    @Suspendable
    fun generateExit(
        exitStates: List<StateAndRef<FungibleToken>>,
        amount: Amount<TokenType>,
        changeHolder: AbstractParty,
        hashingService: HashingService
    ): InputOutputStates<FungibleToken> {
        check(exitStates.isNotEmpty()) {
            "Exiting empty list of states"
        }
        val firstIssuer = exitStates.first().state.data.issuer
        check(exitStates.all { it.state.data.issuer == firstIssuer }) {
            "Tokens with different issuers."
        }
        // Choose states to cover amount - return ones used, and change output
        val changeOutput = change(exitStates, amount, changeHolder, hashingService)
        return InputOutputStates(exitStates, changeOutput?.let { listOf(it) } ?: emptyList())
    }

    private fun change(
        exitStates: List<StateAndRef<FungibleToken>>,
        amount: Amount<TokenType>,
        changeOwner: AbstractParty,
        hashingService: HashingService
    ): FungibleToken? {
        val assetsSum = exitStates.sumTokenStateAndRefs()
        val difference = assetsSum - amount.issuedBy(exitStates.first().state.data.amount.token.issuer)
        check(difference.quantity >= 0) {
            "Sum of exit states should be equal or greater than the amount to exit."
        }
        return if (difference.quantity == 0L) {
            null
        } else {
            difference.heldBy(changeOwner, hashingService)
        }
    }
}