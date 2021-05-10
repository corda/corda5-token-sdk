package com.r3.corda.lib.tokens.testing.contracts

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import net.corda.v5.ledger.contracts.Contract
import net.corda.v5.ledger.contracts.requireThat
import net.corda.v5.ledger.transactions.LedgerTransaction

class TestEvolvableTokenContract : EvolvableTokenContract(), Contract {

    companion object {
        val ID: String = this::class.java.enclosingClass.canonicalName
    }

    override fun additionalCreateChecks(tx: LedgerTransaction) {
        requireThat {
            // No additional checks
        }
    }

    override fun additionalUpdateChecks(tx: LedgerTransaction) {
        requireThat {
            // No additional checks
        }
    }

}