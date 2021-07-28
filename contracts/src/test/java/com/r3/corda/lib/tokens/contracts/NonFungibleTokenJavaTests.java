package com.r3.corda.lib.tokens.contracts;

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import net.corda.v5.application.identity.Party;
import net.corda.v5.crypto.SecureHash;
import net.corda.v5.ledger.UniqueIdentifier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.r3.corda.lib.tokens.testing.states.Rubles.RUB;

public class NonFungibleTokenJavaTests {
    @Test
    public void testNonFungibleToken() {
        Party alice = Mockito.mock(Party.class);
        IssuedTokenType issuedRubles = new IssuedTokenType(alice, RUB);
        new NonFungibleToken(issuedRubles, alice, new UniqueIdentifier(), SecureHash.create("SHA-256:0123456789ABCDEE"));
    }
}
