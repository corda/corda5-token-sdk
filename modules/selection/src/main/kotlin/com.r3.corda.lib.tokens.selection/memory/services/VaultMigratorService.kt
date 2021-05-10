package com.r3.corda.lib.tokens.selection.memory.services

import net.corda.v5.application.node.services.CordaService

class VaultMigratorService : CordaService {
    //TODO - we should attempt to migrate the old vault contents. This must be done a service because we cannot guarantee
    //the order of migration scripts and therefore cannot initiate hibernate
}
