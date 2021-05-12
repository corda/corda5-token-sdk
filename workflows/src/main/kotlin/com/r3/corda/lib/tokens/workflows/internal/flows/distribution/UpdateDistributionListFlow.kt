package com.r3.corda.lib.tokens.workflows.internal.flows.distribution

import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.flowservices.CustomProgressTracker
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.flows.flowservices.dependencies.CordaInject
import net.corda.v5.application.node.services.IdentityService
import net.corda.v5.application.node.services.PersistenceService
import net.corda.v5.application.utilities.ProgressTracker
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.contracts.TransactionState
import net.corda.v5.ledger.services.StateRefLoaderService
import net.corda.v5.ledger.services.VaultService
import net.corda.v5.ledger.transactions.SignedTransaction

// TODO: Handle updates of the distribution list for observers.
@InitiatingFlow
class UpdateDistributionListFlow(val signedTransaction: SignedTransaction) : Flow<Unit>, CustomProgressTracker {

    companion object {
        object ADD_DIST_LIST : ProgressTracker.Step("Adding to distribution list.")
        object UPDATE_DIST_LIST : ProgressTracker.Step("Updating distribution list.")

        fun tracker() = ProgressTracker(ADD_DIST_LIST, UPDATE_DIST_LIST)
    }

    override val progressTracker: ProgressTracker = tracker()

    @CordaInject
    lateinit var identityService: IdentityService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @CordaInject
    lateinit var vaultService: VaultService

    @CordaInject
    lateinit var stateRefLoaderService: StateRefLoaderService

    @Suspendable
    override fun call() {
        val tx = signedTransaction.tx
        val tokensWithTokenPointers: List<AbstractToken> = tx.outputs
            .map(TransactionState<*>::data)
            .filterIsInstance<AbstractToken>()
            .filter { it.tokenType is TokenPointer<*> } // IntelliJ bug?? Check is not always true!
        // There are no evolvable tokens so we don't need to update any distribution lists. Otherwise, carry on.
        if (tokensWithTokenPointers.isEmpty()) return
        val issueCmds: List<IssueTokenCommand> = tx.commands
            .map(Command<*>::value)
            .filterIsInstance<IssueTokenCommand>()
            .filter { it.token.tokenType is TokenPointer<*> }
        val moveCmds: List<MoveTokenCommand> = tx.commands
            .map(Command<*>::value)
            .filterIsInstance<MoveTokenCommand>()
            .filter { it.token.tokenType is TokenPointer<*> }
        if (issueCmds.isNotEmpty()) {
            // If it's an issue transaction then the party calling this flow will be the issuer and they just need to
            // update their local distribution list with the parties that have been just issued tokens.
            val issueTypes: List<TokenPointer<*>> = issueCmds.map { it.token.tokenType }.mapNotNull { it as? TokenPointer<*> }
            progressTracker.currentStep = ADD_DIST_LIST
            val issueStates: List<AbstractToken> = tokensWithTokenPointers.filter {
                it.tokenType in issueTypes
            }
            addToDistributionList(identityService, persistenceService, issueStates)
        }
        if (moveCmds.isNotEmpty()) {
            // If it's a move then we need to call back to the issuer to update the distribution lists with the new
            // token holders.
            val moveTypes = moveCmds.map { it.token.tokenType }
            progressTracker.currentStep = UPDATE_DIST_LIST
            val moveStates = tokensWithTokenPointers.filter { it.tokenType in moveTypes }
            updateDistributionList(
                identityService,
                vaultService,
                stateRefLoaderService,
                flowMessaging,
                flowIdentity.ourIdentity,
                moveStates
            )
        }
    }
}