package net.corda.sample.flows

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.sample.states.House
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.flowservices.dependencies.CordaInject
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.internal.uncheckedCast
import net.corda.v5.ledger.UniqueIdentifier.Companion.fromString
import net.corda.v5.ledger.services.StateLoaderService

import net.corda.v5.ledger.services.queryBy
import net.corda.v5.ledger.services.vault.QueryCriteria.LinearStateQueryCriteria

class GetHouseInfoFlow(
    val nftLinearId: String
) : Flow<House> {

    @CordaInject
    lateinit var vaultService: VaultService

    @CordaInject
    lateinit var stateLoaderService: StateLoaderService

    @Suspendable
    override fun call(): House {
        val houseNft = vaultService.queryBy<NonFungibleToken>(
            criteria = LinearStateQueryCriteria(linearId = listOf(fromString(nftLinearId)))
        ).states.single()

        require(houseNft.state.data.token.isPointer())
        val house =
            stateLoaderService.resolve(
                uncheckedCast<TokenType, TokenPointer<House>>(houseNft.state.data.tokenType)
                    .pointer
            )
        return house.state.data
    }
}
