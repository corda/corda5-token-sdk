package com.r3.corda.lib.tokens.workflows.flows.move

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.MemberLookupService
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.transactions.TransactionBuilder

/**
 * Inlined flow used to move amounts of tokens to parties, [partiesAndAmounts] specifies what amount of tokens is moved
 * to each participant with possible change output paid to the [changeOwner].
 *
 * Call this for one [TokenType] at a time. If you need to do multiple token types in one transaction then create a new
 * flow, calling [addMoveFungibleTokens] for each token type.
 *
 * @param partiesAndAmounts list of pairing party - amount of token that is to be moved to that party
 * @param participantSessions sessions with the participants of move transaction
 * @param observerSessions optional sessions with the observer nodes, to witch the transaction will be broadcasted
 * @param customPostProcessorName name of custom query post processor for token selection
 * @param changeHolder optional holder of the change outputs, it can be confidential identity, if not specified it
 *                     defaults to caller's legal identity
 */
class MoveFungibleTokensFlow(
    val partiesAndAmounts: List<PartyAndAmount<TokenType>>,
    override val participantSessions: List<FlowSession>,
    override val observerSessions: List<FlowSession>,
    val customPostProcessorName: String?,
    val changeHolder: AbstractParty?
) : AbstractMoveTokensFlow() {

    constructor(
        partiesAndAmounts: List<PartyAndAmount<TokenType>>,
        participantSessions: List<FlowSession>,
    ) : this(partiesAndAmounts, participantSessions, emptyList(), null, null)

    constructor(
        partiesAndAmounts: List<PartyAndAmount<TokenType>>,
        participantSessions: List<FlowSession>,
        observerSessions: List<FlowSession>,
    ) : this(partiesAndAmounts, participantSessions, observerSessions, null, null)

    constructor(
        partiesAndAmounts: List<PartyAndAmount<TokenType>>,
        participantSessions: List<FlowSession>,
        customPostProcessorName: String?
    ) : this(partiesAndAmounts, participantSessions, emptyList(), customPostProcessorName, null)

    constructor(
        partiesAndAmounts: List<PartyAndAmount<TokenType>>,
        participantSessions: List<FlowSession>,
        changeHolder: AbstractParty?
    ) : this(partiesAndAmounts, participantSessions, emptyList(), null, changeHolder)

    constructor(
        partiesAndAmounts: List<PartyAndAmount<TokenType>>,
        participantSessions: List<FlowSession>,
        observerSessions: List<FlowSession>,
        customPostProcessorName: String?
    ) : this(partiesAndAmounts, participantSessions, observerSessions, customPostProcessorName, null)

    constructor(
        partiesAndAmounts: List<PartyAndAmount<TokenType>>,
        participantSessions: List<FlowSession>,
        observerSessions: List<FlowSession>,
        changeHolder: AbstractParty?
    ) : this(partiesAndAmounts, participantSessions, observerSessions, null, changeHolder)

    constructor(
        partiesAndAmounts: List<PartyAndAmount<TokenType>>,
        participantSessions: List<FlowSession>,
        customPostProcessorName: String?,
        changeHolder: AbstractParty?
    ) : this(partiesAndAmounts, participantSessions, emptyList(), customPostProcessorName, changeHolder)



    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
        participantSessions: List<FlowSession>,
    ) : this(listOf(partyAndAmount), participantSessions)

    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
        participantSessions: List<FlowSession>,
        observerSessions: List<FlowSession>,
    ) : this(listOf(partyAndAmount), participantSessions, observerSessions)

    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
        participantSessions: List<FlowSession>,
        customPostProcessorName: String?
    ) : this(listOf(partyAndAmount), participantSessions, customPostProcessorName)

    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
        participantSessions: List<FlowSession>,
        changeHolder: AbstractParty?
    ) : this(listOf(partyAndAmount), participantSessions, changeHolder)

    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
        participantSessions: List<FlowSession>,
        observerSessions: List<FlowSession>,
        customPostProcessorName: String?
    ) : this(listOf(partyAndAmount), participantSessions, observerSessions, customPostProcessorName)

    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
        participantSessions: List<FlowSession>,
        observerSessions: List<FlowSession>,
        changeHolder: AbstractParty?
    ) : this(listOf(partyAndAmount), participantSessions, observerSessions, changeHolder)

    constructor(
        partyAndAmount: PartyAndAmount<TokenType>,
        participantSessions: List<FlowSession>,
        customPostProcessorName: String?,
        changeHolder: AbstractParty?
    ) : this(listOf(partyAndAmount), participantSessions, customPostProcessorName, changeHolder)

    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @CordaInject
    lateinit var identityService: IdentityService

    @CordaInject
    lateinit var memberLookupService: MemberLookupService

    @CordaInject
    lateinit var hashingService: HashingService

    @Suspendable
    override fun addMove(transactionBuilder: TransactionBuilder) {
        addMoveFungibleTokens(
            transactionBuilder = transactionBuilder,
            persistenceService,
            identityService,
            hashingService,
            flowEngine,
            memberLookupService.myInfo(),
            partiesAndAmounts = partiesAndAmounts,
            changeHolder = changeHolder ?: flowIdentity.ourIdentity,
            TokenQueryBy(customPostProcessorName)
        )
    }
}