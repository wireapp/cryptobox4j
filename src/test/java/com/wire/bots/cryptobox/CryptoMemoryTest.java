package com.wire.bots.cryptobox;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import static com.wire.bots.cryptobox.Util.assertDecrypted;

public class CryptoMemoryTest {
    private final static String bobId = "bob";
    private final static String bobClientId = "bob_device";
    private final static String aliceId = "alice";
    private final static String aliceClientId = "alice_device";
    private static final MemStorage storage = new MemStorage();
    private static CryptoDb alice;
    private static CryptoDb bob;
    private static PreKey[] bobKeys;
    private static PreKey[] aliceKeys;

    @BeforeAll
    public static void setUp() throws Exception {
        alice = new CryptoDb(aliceId, storage);
        bob = new CryptoDb(bobId, storage);

        bobKeys = bob.newPreKeys(0, 8);
        aliceKeys = alice.newPreKeys(0, 8);
    }

    @AfterAll
    public static void clean() throws IOException {
        alice.close();
        bob.close();

        Util.deleteDir("data");
    }

    @Test
    public void testAliceToBob() throws Exception {
        String text = "Hello Bob, This is Alice!";

        // Encrypt using prekeys
        byte[] cipher = alice.encryptFromPreKeys(bobClientId, bobKeys[0], text.getBytes());

        // Decrypt using initSessionFromMessage
        byte[] decrypt = bob.decrypt(aliceClientId, cipher);

        assertDecrypted(decrypt, text);
    }

    @Test
    public void testBobToAlice() throws Exception {
        String text = "Hello Alice, This is Bob!";

        byte[] cipher = bob.encryptFromPreKeys(aliceClientId, aliceKeys[0], text.getBytes());

        // Decrypt using initSessionFromMessage
        byte[] decrypt = alice.decrypt(bobClientId, cipher);

        assertDecrypted(decrypt, text);
    }

    @Test
    public void testSessions() throws Exception {
        String text = "Hello Alice, This is Bob, again!";

        byte[] cipher = bob.encryptFromSession(aliceClientId, text.getBytes());

        // Decrypt using session
        byte[] decrypt = alice.decrypt(bobClientId, cipher);

        assertDecrypted(decrypt, text);
    }

    @Test
    public void testMassiveSessions() throws Exception {
        for (int i = 0; i < 100; i++) {
            String text = "Hello Alice, This is Bob, again! " + i;

            byte[] cipher = bob.encryptFromSession(aliceClientId, text.getBytes());

            // Decrypt using session
            byte[] decrypt = alice.decrypt(bobClientId, cipher);

            assertDecrypted(decrypt, text);

            text = "Hey Bob, How's life? " + i;

            cipher = alice.encryptFromSession(bobClientId, text.getBytes());

            // Decrypt using session
            decrypt = bob.decrypt(aliceClientId, cipher);

            assertDecrypted(decrypt, text);
        }
    }

    @Test
    public void testIdentity() throws Exception {
        Random random = new Random();
        final String carlId = "" + random.nextInt();
        final String dir = "data/" + carlId;

        CryptoDb carl = new CryptoDb(carlId, storage);
        PreKey[] carlPrekeys = carl.newPreKeys(0, 8);

        String daveId = "" + random.nextInt();
        CryptoDb dave = new CryptoDb(daveId, storage);
        PreKey[] davePrekeys = dave.newPreKeys(0, 8);

        String text = "Hello Bob, This is Carl!";

        // Encrypt using prekeys
        byte[] cipher = dave.encryptFromPreKeys(carlId, carlPrekeys[0], text.getBytes());
        byte[] decrypt = carl.decrypt(daveId, cipher);

        assertDecrypted(decrypt, text);

        carl.close();
        dave.close();
        Util.deleteDir(dir);

        dave = new CryptoDb(daveId, storage);
        carl = new CryptoDb(carlId, storage);
        cipher = dave.encryptFromSession(carlId, text.getBytes());
        decrypt = carl.decrypt(daveId, cipher);

        assertDecrypted(decrypt, text);

        carl.close();
        Util.deleteDir(dir);

        carl = new CryptoDb(carlId, storage);

        cipher = carl.encryptFromPreKeys(daveId, davePrekeys[0], text.getBytes());
        decrypt = dave.decrypt(carlId, cipher);
        assertDecrypted(decrypt, text);

        carl.close();
        dave.close();
    }
}
