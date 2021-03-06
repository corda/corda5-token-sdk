package com.r3.corda.lib.tokens.workflows.flows.rpc

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.move.*
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import com.r3.corda.lib.tokens.workflows.utilities.sessionsForParties
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowException
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.flows.StartableByService
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.crypto.KeyManagementService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.transactions.SignedTransaction

/**
 * Initiating flow used to move amounts of tokens to parties, [partiesAndAmounts] specifies what amount of tokens is moved
 * to each participant with possible change output paid to the [changeHolder].
 *
 * Call this for one [TokenType] at a time. If you need to do multiple token types in one transaction then create a new
 * flow, calling [addMoveFungibleTokens] for each token type.
 *
 * @param partiesAndAmounts list of pairing party - amount of token that is to be moved to that party
 * @param observers optional observing parties to which the transaction will be broadcast
 * @param customPostProcessorName name of custom query post processor for token selection
 * @param changeHolder optional holder of the change outputs, it can be confidential identity, if not specified it
 *                     defaults to caller's legal identity
 */
@StartableByService
@StartableByRPC
@InitiatingFlow
class MoveFungibleTokens (
    val partiesAndAmounts: List<PartyAndAmount<TokenType>>,
    val observers: List<Party>,
    val customPostProcessorName: String?,
    val changeHolder: AbstractParty?
) : Flow<SignedTransaction> {

    constructor(
        partiesAndAmounts: List<PartyAndAmount<TokenType>>,
    ) : this(partiesAndAmounts, emptyList(), null, null)

    constructor(
        partiesAndAmounts: List<PartyAndAmount<TokenType>>,
        observers: List<Party>,
    ) : this(partiesAndAmounts, observers, null, null)

    constructor(
        partiesAndAmounts: List<PartyAndAmount<TokenType>>,
        customPostProcessorName: String?,
    ) : this(partiesAndAmounts, emptyList(), customPostProcessorName, null)

    constructor(
        partiesAndAmounts: List<PartyAndAmount<TokenType>>,
        changeHolder: AbstractParty?
    ) : this(partiesAndAmounts, emptyList(), null, changeHolder)

    constructor(
        partiesAndAmounts: List<PartyAndAmount<TokenType>>,
        observers: List<Party>,
        customPostProcessorName: String?,
    ) : this(partiesAndAmounts, observers, customPostProcessorName, null)

    constructor(
        partiesAndAmounts: List<PartyAndAmount<TokenType>>,
        observers: List<Party>,
        changeHolder: AbstractParty?
    ) : this(partiesAndAmounts, observers, null, changeHolder)

    constructor(
        partiesAndAmounts: List<PartyAndAmount<TokenType>>,
        customPostProcessorName: String?,
        changeHolder: AbstractParty?
    ) : this(partiesAndAmounts, emptyList(), customPostProcessorName, changeHolder)

    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
    ) : this(listOf(partyAndAmount), emptyList(), null, null)

    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
        observers: List<Party>,
    ) : this(listOf(partyAndAmount), observers, null, null)

    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
        customPostProcessorName: String?,
    ) : this(listOf(partyAndAmount), emptyList(), customPostProcessorName, null)

    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
        changeHolder: AbstractParty?
    ) : this(listOf(partyAndAmount), emptyList(), null, changeHolder)

    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
        observers: List<Party>,
        customPostProcessorName: String?,
    ) : this(listOf(partyAndAmount), observers, customPostProcessorName, null)

    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
        observers: List<Party>,
        changeHolder: AbstractParty?
    ) : this(listOf(partyAndAmount), observers, null, changeHolder)

    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
        customPostProcessorName: String?,
        changeHolder: AbstractParty?
    ) : this(listOf(partyAndAmount), emptyList(), customPostProcessorName, changeHolder)

    constructor(amount: Amount<TokenType>, holder: AbstractParty) : this(PartyAndAmount(holder, amount), emptyList())

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var identityService: IdentityService

    @Suspendable
    override fun call(): SignedTransaction {
        val participants = partiesAndAmounts.map(PartyAndAmount<*>::party)
        val observerSessions = sessionsForParties(identityService, flowMessaging, observers)
        val participantSessions = sessionsForParties(identityService, flowMessaging, participants)
        return flowEngine.subFlow(
            MoveFungibleTokensFlow(
                partiesAndAmounts = partiesAndAmounts,
                participantSessions = participantSessions,
                observerSessions = observerSessions,
                changeHolder = changeHolder,
                customPostProcessorName = customPostProcessorName
            )
        )
    }
}

/**
 * Responder flow for [MoveFungibleTokens].
 */
@InitiatedBy(MoveFungibleTokens::class)
class MoveFungibleTokensHandler(val otherSession: FlowSession) : Flow<Unit> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() = flowEngine.subFlow(MoveTokensFlowHandler(otherSession))
}

/**
 * Initiating flow used to move non fungible tokens to parties, [partiesAndTokens] specifies what tokens are moved
 * to each participant.
 *
 * Call this for one [TokenType] at a time. If you need to do multiple token types in one transaction then create a new
 * flow, calling [addMoveNonFungibleTokens] for each token type.
 *
 * @param partyAndToken pairing party - token that is to be moved to that party
 * @param observers optional observing parties to which the transaction will be broadcast
 * @param customPostProcessorName name of custom query post processor for token selection
 */
@StartableByService
@StartableByRPC
@InitiatingFlow
class MoveNonFungibleTokens (
    val partyAndToken: PartyAndToken,
    val observers: List<Party>,
    val customPostProcessorName: String?
) : Flow<SignedTransaction> {

    constructor(
        partyAndToken: PartyAndToken,
    ) : this(partyAndToken, emptyList(), null)

    constructor(
        partyAndToken: PartyAndToken,
        observers: List<Party>,
    ) : this(partyAndToken, observers, null)

    constructor(
        partyAndToken: PartyAndToken,
        customPostProcessorName: String?
    ) : this(partyAndToken, emptyList(), customPostProcessorName)

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var identityService: IdentityService

    @Suspendable
    override fun call(): SignedTransaction {
        val observerSessions = sessionsForParties(identityService, flowMessaging, observers)
        val participantSessions = sessionsForParties(identityService, flowMessaging, listOf(partyAndToken.party))
        return flowEngine.subFlow(
            MoveNonFungibleTokensFlow(
                partyAndToken = partyAndToken,
                participantSessions = participantSessions,
                observerSessions = observerSessions,
                customPostProcessorName = customPostProcessorName
            )
        )
    }
}

/**
 * Responder flow for [MoveNonFungibleTokens].
 */
@InitiatedBy(MoveNonFungibleTokens::class)
class MoveNonFungibleTokensHandler(val otherSession: FlowSession) : Flow<Unit> {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() = flowEngine.subFlow(MoveTokensFlowHandler(otherSession))
}

/* Confidential flows. */

/**
 * Version of [MoveFungibleTokens] using confidential identities. Confidential identities are generated and
 * exchanged for all parties that receive tokens states.
 *
 * Call this for one [TokenType] at a time. If you need to do multiple token types in one transaction then create a new
 * flow, calling [addMoveNonFungibleTokens] for each token type and handle confidential identities exchange yourself.
 *
 * @param partiesAndAmounts list of pairing party - amount of token that is to be moved to that party
 * @param observers optional observing parties to which the transaction will be broadcast
 * @param customPostProcessorName name of custom query post processor for token selection
 * @param changeHolder holder of the change outputs, it can be confidential identity
 */
@StartableByService
@StartableByRPC
@InitiatingFlow
class ConfidentialMoveFungibleTokens(
    val partiesAndAmounts: List<PartyAndAmount<TokenType>>,
    val observers: List<Party>,
    val customPostProcessorName: String?,
    val changeHolder: AbstractParty?
) : Flow<SignedTransaction> {

    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
        observers: List<Party>,
    ) : this(listOf(partyAndAmount), observers, null, null)

    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
        observers: List<Party>,
        customPostProcessorName: String?,
    ) : this(listOf(partyAndAmount), observers, customPostProcessorName, null)

    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
        observers: List<Party>,
        changeHolder: AbstractParty?
    ) : this(listOf(partyAndAmount), observers, null, changeHolder)

    @CordaInject
    lateinit var keyManagementService: KeyManagementService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var identityService: IdentityService

    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(): SignedTransaction {
        val participants = partiesAndAmounts.map(PartyAndAmount<*>::party)
        val observerSessions = sessionsForParties(identityService, flowMessaging, observers)
        val participantSessions = sessionsForParties(identityService, flowMessaging, participants)
        val confidentialHolder = changeHolder ?: let {
            val key = keyManagementService.freshKey()
            try {
                identityService.registerKey(key, flowIdentity.ourIdentity.name)
            } catch (e: Exception) {
                throw FlowException(
                    "Could not register a new key for party: ${flowIdentity.ourIdentity} as the provided public key is already registered " +
                            "or registered to a different party."
                )
            }
            identityService.anonymousPartyFromKey(key)
        }
        return flowEngine.subFlow(
            ConfidentialMoveFungibleTokensFlow(
                partiesAndAmounts = partiesAndAmounts,
                participantSessions = participantSessions,
                observerSessions = observerSessions,
                changeHolder = confidentialHolder,
                customPostProcessorName = customPostProcessorName
            )
        )
    }
}

/**
 * Responder flow for [ConfidentialMoveFungibleTokens]
 */
@InitiatedBy(ConfidentialMoveFungibleTokens::class)
class ConfidentialMoveFungibleTokensHandler(val otherSession: FlowSession) : Flow<Unit> {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() = flowEngine.subFlow(ConfidentialMoveTokensFlowHandler(otherSession))
}

/**
 * Version of [MoveNonFungibleTokens] using confidential identities. Confidential identities are generated and
 * exchanged for all parties that receive tokens states.
 *
 * Call this for one [TokenType] at a time. If you need to do multiple token types in one transaction then create a new
 * flow, calling [addMoveFungibleTokens] for each token type and handle confidential identities exchange yourself.
 *
 * @param partyAndToken list of pairing party - token that is to be moved to that party
 * @param observers optional observing parties to which the transaction will be broadcast
 * @param customPostProcessorName name of custom query post processor for token selection
 */
@StartableByService
@StartableByRPC
@InitiatingFlow
class ConfidentialMoveNonFungibleTokens(
    val partyAndToken: PartyAndToken,
    val observers: List<Party>,
    val customPostProcessorName: String?
) : Flow<SignedTransaction> {

    constructor(
        partyAndToken: PartyAndToken,
        observers: List<Party>,
    ) : this(partyAndToken, observers, null)

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var identityService: IdentityService

    @Suspendable
    override fun call(): SignedTransaction {
        val observerSessions = sessionsForParties(identityService, flowMessaging, observers)
        val participantSessions = sessionsForParties(identityService, flowMessaging, listOf(partyAndToken.party))
        return flowEngine.subFlow(
            ConfidentialMoveNonFungibleTokensFlow(
                partyAndToken = partyAndToken,
                participantSessions = participantSessions,
                observerSessions = observerSessions,
                customPostProcessorName = customPostProcessorName
            )
        )
    }
}

/**
 * Responder flow for [ConfidentialMoveNonFungibleTokens].
 */
@InitiatedBy(ConfidentialMoveNonFungibleTokens::class)
class ConfidentialMoveNonFungibleTokensHandler(val otherSession: FlowSession) : Flow<Unit> {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call() = flowEngine.subFlow(ConfidentialMoveTokensFlowHandler(otherSession))
}