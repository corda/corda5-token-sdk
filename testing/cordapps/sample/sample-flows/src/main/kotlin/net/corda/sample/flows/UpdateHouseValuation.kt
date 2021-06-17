package net.corda.sample.flows

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.rpc.UpdateEvolvableToken
import net.corda.sample.states.House
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.internal.uncheckedCast
import net.corda.v5.base.util.seconds
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.services.StateLoaderService
import net.corda.v5.ledger.services.vault.StateStatus
import net.corda.v5.ledger.transactions.SignedTransaction

@StartableByRPC
class UpdateHouseValuation(
    val currencyCode: String,
    val value: Long,
    val linearId: String,
) : Flow<SignedTransaction> {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var stateLoaderService: StateLoaderService

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @Suspendable
    override fun call(): SignedTransaction {

        val cursor = persistenceService.query<StateAndRef<NonFungibleToken>>(
            "LinearState.findByUuidAndStateStatus",
            mapOf(
                "uuid" to UniqueIdentifier.fromString(linearId),
                "stateStatus" to StateStatus.UNCONSUMED,
            )
        )

        val results = mutableListOf<StateAndRef<NonFungibleToken>>()
        do {
            val pollResult = cursor.poll(1, 5.seconds)
            results.addAll(pollResult.values)
        } while (!pollResult.isLastResult)

        require(results.size == 1)


        val houseNft = results.single()


        require(houseNft.state.data.token.isPointer())
        val oldHouseTokenStateAndRef = stateLoaderService
            .resolve(
                uncheckedCast<TokenType, TokenPointer<House>>(houseNft.state.data.tokenType)
                    .pointer
            )

        val newHouseToken = oldHouseTokenStateAndRef.state.data.copy(
            valuation = Amount(value, FiatCurrency.getInstance(currencyCode))
        )
        return flowEngine.subFlow(UpdateEvolvableToken(oldHouseTokenStateAndRef, newHouseToken))
    }
}
