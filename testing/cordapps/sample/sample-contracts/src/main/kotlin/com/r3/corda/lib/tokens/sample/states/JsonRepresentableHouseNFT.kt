package com.r3.corda.lib.tokens.sample.states

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import net.corda.v5.application.utilities.JsonRepresentable

class JsonRepresentableHouseNFT(val houseNft : NonFungibleToken) : JsonRepresentable {
    override fun toJsonString(): String {
        return """
            {
               "token" : {
                    "issuer" : "${houseNft.token.issuer.name}",
                    "linearId" : "${(houseNft.token.tokenType as TokenPointer<*>).pointer.pointer.id}"
               },
               "holder" : "${houseNft.holder.nameOrNull()}",
               "linearId" : "${houseNft.linearId}"
            }
        """.trimIndent()
    }
}