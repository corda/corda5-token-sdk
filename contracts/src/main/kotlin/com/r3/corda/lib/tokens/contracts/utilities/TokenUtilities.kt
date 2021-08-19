package com.r3.corda.lib.tokens.contracts.utilities

import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import net.corda.v5.application.identity.Party
import net.corda.v5.crypto.toStringShort

class TokenUtilities

/**
 * Converts [AbstractToken.holder] into a more friendly string. It uses only the x500 organisation for [Party] objects
 * and shortens the public key for [AnonymousParty]s to the first 16 characters.
 */
val AbstractToken.holderString: String
    get() =
        (holder as? Party)?.name?.organisation ?: holder.owningKey.toStringShort().substring(0, 16)
