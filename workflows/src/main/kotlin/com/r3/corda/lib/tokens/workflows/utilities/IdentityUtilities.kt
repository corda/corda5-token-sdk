@file:JvmName("IdentityUtilities")

package com.r3.corda.lib.tokens.workflows.utilities

import net.corda.v5.application.identity.Party
import net.corda.v5.application.services.MemberLookupService

fun MemberLookupService.isOurIdentity(party: Party): Boolean {
    return party.owningKey in myInfo().identityKeys
}