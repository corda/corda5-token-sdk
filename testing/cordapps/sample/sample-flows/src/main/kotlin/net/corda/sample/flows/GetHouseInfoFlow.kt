package net.corda.sample.flows

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.sample.states.House
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.internal.uncheckedCast
import net.corda.v5.base.util.seconds
import net.corda.v5.ledger.UniqueIdentifier.Companion.fromString
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.services.StateLoaderService

import net.corda.v5.ledger.services.vault.StateStatus

class GetHouseInfoFlow(
    val nftLinearId: String
) : Flow<House> {

    @CordaInject
    lateinit var stateLoaderService: StateLoaderService

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @Suspendable
    override fun call(): House {
        val cursor = persistenceService.query<StateAndRef<NonFungibleToken>>(
            "LinearState.findByUuidAndStateStatus",
            mapOf(
                "uuid" to fromString(nftLinearId),
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
        val house =
            stateLoaderService.resolve(
                uncheckedCast<TokenType, TokenPointer<House>>(houseNft.state.data.tokenType)
                    .pointer
            )
        return house.state.data
    }
}
