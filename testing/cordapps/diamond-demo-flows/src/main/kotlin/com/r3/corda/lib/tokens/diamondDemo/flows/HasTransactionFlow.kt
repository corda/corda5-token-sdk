package com.r3.corda.lib.tokens.diamondDemo.flows

import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.JsonConstructor
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.crypto.SecureHash
import net.corda.v5.ledger.services.TransactionStorage
import net.corda.v5.ledger.transactions.SignedTransactionDigest

@StartableByRPC
class HasTransactionFlow
@JsonConstructor constructor(
    val params: RpcStartFlowRequestParameters
) : Flow<Boolean> {

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var transactionStorage: TransactionStorage

    @Suspendable
    override fun call(): Boolean {
        val parameters = jsonMarshallingService.parseParameters(params)
        val signedTransactionDigest: SignedTransactionDigest =
            jsonMarshallingService.parseJson(parameters.getMandatoryParameter("transactionDigest"))
        val tx = transactionStorage.getTransaction(SecureHash.create(signedTransactionDigest.txId))
        return tx != null
    }
}