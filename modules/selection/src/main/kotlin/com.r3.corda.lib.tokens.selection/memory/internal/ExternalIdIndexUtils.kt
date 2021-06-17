package com.r3.corda.lib.tokens.selection.memory.internal

import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.KeyManagementService
import java.security.PublicKey
import java.util.*

sealed class Holder {
    data class KeyIdentity(val owningKey: PublicKey) : Holder() // Just public key
    class UnmappedIdentity : Holder() // For all keys that are unmapped
    {
        override fun equals(other: Any?): Boolean {
            return this === other
        }

        override fun hashCode(): Int {
            return System.identityHashCode(this)
        }
    }

    data class MappedIdentity(val uuid: UUID) : Holder() // All keys register to this uuid
    class TokenOnly : Holder() // This is for the case where we use token class and token identifier only
    {
        override fun equals(other: Any?): Boolean {
            return this === other
        }

        override fun hashCode(): Int {
            return System.identityHashCode(this)
        }
    }

    companion object {
        fun fromUUID(uuid: UUID?): Holder {
            return if (uuid != null) {
                MappedIdentity(uuid)
            } else {
                UnmappedIdentity()
            }
        }
    }
}

fun lookupExternalIdFromKey(owningKey: PublicKey, identityService: IdentityService, keyManagementService: KeyManagementService): Holder {
    val uuid = identityService.externalIdForPublicKey(owningKey)
    return if (uuid != null || isKeyPartOfNodeKeyPairs(owningKey, keyManagementService) || isKeyIdentityKey(owningKey, identityService)) {
        val signingEntity = Holder.fromUUID(uuid)
        signingEntity
    } else {
        Holder.UnmappedIdentity()
    }
}

/**
 * Establish whether a public key is one of the node's identity keys, by looking in the node's identity database table.
 */
private fun isKeyIdentityKey(key: PublicKey, identityService: IdentityService): Boolean {
    val party = identityService.nameFromKey(key)?.let { identityService.partyFromName(it) }
    return party?.owningKey == key
}

/**
 * Check to see if the key belongs to one of the key pairs in the node_our_key_pairs table. These keys may relate to confidential
 * identities.
 */
private fun isKeyPartOfNodeKeyPairs(key: PublicKey, keyManagementService: KeyManagementService): Boolean {
    return keyManagementService.filterMyKeys(listOf(key)).toList().isNotEmpty()
}
