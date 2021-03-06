@file:JvmName("NotaryUtilities")

package com.r3.corda.lib.tokens.workflows.utilities

import net.corda.v5.application.cordapp.CordappConfig
import net.corda.v5.application.cordapp.CordappConfigException
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.identity.Party
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.services.isValidating
import net.corda.v5.ledger.transactions.TransactionBuilder

// TODO getPreferredNotary should be loaded on start
/**
 * Gets the preferred notary from the CorDapp config file. Otherwise, the list of notaries from the network map cache
 * is returned. From this list the CorDapp developer can employ their own strategy to choose a notary. for now, the
 * strategies will be to either choose the first notary or a random notary from the list.
 *
 * @param notaryLookupService a [NotaryLookupService] instance.
 * @param backupSelector a function which selects a notary when the notary property is not set in the CorDapp config.
 * @return the selected notary [Party] object.
 */
fun getPreferredNotary(
    notaryLookupService: NotaryLookupService,
    cordappConfig: CordappConfig,
    backupSelector: (NotaryLookupService) -> Party
): Party {
    val notaryString = try {
        cordappConfig.getString("notary")
    } catch (e: CordappConfigException) {
        ""
    }
    return if (notaryString.isBlank()) {
        backupSelector(notaryLookupService)
    } else {
        val notaryX500Name = CordaX500Name.parse(notaryString)
        val notaryParty = notaryLookupService.getNotary(notaryX500Name)
            ?: throw IllegalStateException(
                "Notary with name \"$notaryX500Name\" cannot be found in the network " +
                        "map cache. Either the notary does not exist, or there is an error in the config."
            )
        notaryParty
    }
}

fun getPreferredNotary(
    notaryLookupService: NotaryLookupService,
    cordappConfig: CordappConfig,
): Party = getPreferredNotary(notaryLookupService, cordappConfig, firstNotary())

/** Choose the first notary in the list. */
fun firstNotary(): (NotaryLookupService) -> Party = { notaryLookupService: NotaryLookupService ->
    notaryLookupService.notaryIdentities.firstOrNull()
        ?: throw IllegalArgumentException("No available notaries.")
}

/** Choose a random notary from the list. */
fun randomNotary(): (NotaryLookupService) -> Party = { notaryLookupService: NotaryLookupService ->
    notaryLookupService.notaryIdentities.randomOrNull()
        ?: throw IllegalArgumentException("No available notaries.")
}

/** Choose a random non validating notary. */
fun randomNonValidatingNotary(): (NotaryLookupService) -> Party? = { notaryLookupService: NotaryLookupService ->
    notaryLookupService.notaryIdentities.filter { notary ->
        notaryLookupService.isValidating(notary).not()
    }.randomOrNull()
}

/** Choose a random validating notary. */
fun randomValidatingNotary(): (NotaryLookupService) -> Party? = { notaryLookupService: NotaryLookupService ->
    notaryLookupService.notaryIdentities.filter { notary ->
        notaryLookupService.isValidating(notary)
    }.randomOrNull()
}

/** Adds a notary to a new [TransactionBuilder]. If the notary is already set then it get overwritten by preferred notary  */
fun addNotary(notaryLookupService: NotaryLookupService, cordappConfig: CordappConfig, txb: TransactionBuilder): TransactionBuilder {
    return txb.apply { setNotary(getPreferredNotary(notaryLookupService, cordappConfig)) }
}

/** Adds a notary to the [TransactionBuilder]. If the notary is already set then it get overwritten by preferred notary  */
fun TransactionBuilder.addPreferredNotary(notaryLookupService: NotaryLookupService, cordappConfig: CordappConfig) {
    setNotary(getPreferredNotary(notaryLookupService, cordappConfig))
}

/**
 * Adds notary if not set. Otherwise checks if it's the same as the one in TransactionBuilder.
 */
// TODO Internal, because for now useful only when selecting tokens and passing builder around.
internal fun addNotaryWithCheck(txb: TransactionBuilder, notary: Party): TransactionBuilder {
    if (txb.notary == null) {
        txb.setNotary(notary)
    }
    check(txb.notary == notary) {
        "Notary passed to transaction builder (${txb.notary}) should be the same as the one used by input states ($notary)."
    }
    return txb
}
