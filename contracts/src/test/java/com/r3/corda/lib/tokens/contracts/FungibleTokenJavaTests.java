package com.r3.corda.lib.tokens.contracts;

import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import net.corda.v5.application.identity.Party;
import net.corda.v5.crypto.SecureHash;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.r3.corda.lib.tokens.contracts.utilities.AmountUtilities.amount;
import static com.r3.corda.lib.tokens.testing.states.Rubles.RUB;

public class FungibleTokenJavaTests {
    @Test
    public void testFungibleToken() {
        Party alice = Mockito.mock(Party.class);
        IssuedTokenType issuedRubles = new IssuedTokenType(alice, RUB);
        new FungibleToken(amount(10, issuedRubles), alice, SecureHash.create("SHA-256:0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"));
    }
}
