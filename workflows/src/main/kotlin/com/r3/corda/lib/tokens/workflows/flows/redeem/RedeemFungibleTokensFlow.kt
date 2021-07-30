package com.r3.corda.lib.tokens.workflows.flows.redeem

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.crypto.HashingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.transactions.TransactionBuilder

/**
 * Inlined flow used to redeem amount of [FungibleToken]s issued by the particular issuer with possible change output
 * paid to the [changeHolder].
 *
 * @param amount amount of token to redeem
 * @param changeHolder owner of possible change output, which defaults to the node identity of the calling node
 * @param issuerSession session with the issuer tokens should be redeemed with
 * @param observerSessions optional sessions with the observer nodes, to witch the transaction will be broadcasted
 * @param customPostProcessorName name of custom query post processor for token selection
 */
class RedeemFungibleTokensFlow (
    val amount: Amount<TokenType>,
    override val issuerSession: FlowSession,
    val changeHolder: AbstractParty?,
    override val observerSessions: List<FlowSession>,
    val customPostProcessorName: String?
) : AbstractRedeemTokensFlow() {

    constructor(
        amount: Amount<TokenType>,
        issuerSession: FlowSession,
    ) : this(amount, issuerSession, null, emptyList(), null)

    constructor(
        amount: Amount<TokenType>,
        issuerSession: FlowSession,
        changeHolder: AbstractParty?,
    ) : this(amount, issuerSession, changeHolder, emptyList(), null)

    constructor(
        amount: Amount<TokenType>,
        issuerSession: FlowSession,
        observerSessions: List<FlowSession>,
    ) : this(amount, issuerSession, null, observerSessions, null)

    constructor(
        amount: Amount<TokenType>,
        issuerSession: FlowSession,
        customPostProcessorName: String?
    ) : this(amount, issuerSession, null, emptyList(), customPostProcessorName)

    constructor(
        amount: Amount<TokenType>,
        issuerSession: FlowSession,
        changeHolder: AbstractParty?,
        observerSessions: List<FlowSession>,
    ) : this(amount, issuerSession, changeHolder, observerSessions, null)

    constructor(
        amount: Amount<TokenType>,
        issuerSession: FlowSession,
        changeHolder: AbstractParty?,
        customPostProcessorName: String?
    ) : this(amount, issuerSession, changeHolder, emptyList(), customPostProcessorName)

    constructor(
        amount: Amount<TokenType>,
        issuerSession: FlowSession,
        observerSessions: List<FlowSession>,
        customPostProcessorName: String?
    ) : this(amount, issuerSession, null, observerSessions, customPostProcessorName)

    @CordaInject
    lateinit var identityService: IdentityService

    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @CordaInject
    lateinit var hashingService: HashingService

    @Suspendable
    override fun generateExit(transactionBuilder: TransactionBuilder) {
        addFungibleTokensToRedeem(
            transactionBuilder = transactionBuilder,
            persistenceService = persistenceService,
            identityService = identityService,
            hashingService = hashingService,
            flowEngine = flowEngine,
            amount = amount,
            tokenQueryBy = TokenQueryBy(issuer = issuerSession.counterparty, customPostProcessorName = customPostProcessorName),
            changeHolder = changeHolder ?: flowIdentity.ourIdentity,
        )
    }
}