package net.corda.sample.flows

import com.r3.corda.lib.tokens.contracts.utilities.amount
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.utilities.NonFungibleTokenBuilder
import net.corda.sample.states.House
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.TransactionState
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.transactions.SignedTransaction

@StartableByRPC
class CreateHouseToken(
    val address: String,
    val currencyCode: String,
    val value: Long,
) : Flow<SignedTransaction> {

    @CordaInject
    lateinit var notaryLookupService: NotaryLookupService

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = notaryLookupService.notaryIdentities.first()

        val house = House(
            address,
            amount(value, FiatCurrency.getInstance(currencyCode)),
            listOf(flowIdentity.ourIdentity),
            linearId = UniqueIdentifier()
        )
        val transactionState = TransactionState(house, notary = notary)
        flowEngine.subFlow(CreateEvolvableTokens(transactionState))

        val houseToken = NonFungibleTokenBuilder()
            .ofTokenType(house.toPointer<House>())
            .issuedBy(flowIdentity.ourIdentity)
            .heldBy(flowIdentity.ourIdentity)
            .buildNonFungibleToken()

        return flowEngine.subFlow(IssueTokens(listOf(houseToken)))
    }
}
