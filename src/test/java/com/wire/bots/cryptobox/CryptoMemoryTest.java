package com.wire.bots.cryptobox;


import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.UUID;

import static com.wire.bots.cryptobox.Util.assertDecrypted;

public class CryptoMemoryTest {
    private String bobClientId;
    private String aliceClientId;
    private MemStorage storage;
    private CryptoDb alice;
    private CryptoDb bob;
    private PreKey[] bobKeys;
    private PreKey[] aliceKeys;

    private String rootFolder;

    @BeforeEach
    public void setUp() throws Exception {
        rootFolder = "cryptobox-test-data-" + UUID.randomUUID();

        final String aliceId = UUID.randomUUID().toString();
        aliceClientId = aliceId + "-client";
        final String bobId = UUID.randomUUID().toString();
        bobClientId = bobId + "-client";

        storage = new MemStorage();

        alice = new CryptoDb(aliceId, storage);
        bob = new CryptoDb(bobId, storage);

        bobKeys = bob.newPreKeys(0, 8);
        aliceKeys = alice.newPreKeys(0, 8);
    }

    @AfterEach
    public void clean() throws IOException {
        alice.close();
        bob.close();

        Util.deleteDir(rootFolder);
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
        Assertions.assertNotNull(cipher);

        // Decrypt using initSessionFromMessage
        byte[] decrypt = alice.decrypt(bobClientId, cipher);

        assertDecrypted(decrypt, text);
    }

    @Test
    @Disabled("This tests fails on broken sessions")
    public void testSessions() throws Exception {
        String text = "Hello Alice, This is Bob, again!";

        byte[] cipher = bob.encryptFromSession(aliceClientId, text.getBytes());
        // TODO this line fails
        Assertions.assertNotNull(cipher);

        // Decrypt using session
        byte[] decrypt = alice.decrypt(bobClientId, cipher);

        assertDecrypted(decrypt, text);
    }

    @Test
    @Disabled("This tests fails on broken sessions")
    public void testMassiveSessions() throws Exception {
        for (int i = 0; i < 100; i++) {
            String text = "Hello Alice, This is Bob, again! " + i;

            byte[] cipher = bob.encryptFromSession(aliceClientId, text.getBytes());
            // TODO this line fails
            Assertions.assertNotNull(cipher);

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
        final String carlId = UUID.randomUUID().toString();
        final String dir = String.format("%s/%s", rootFolder, carlId);

        CryptoDb carl = new CryptoDb(carlId, storage);
        PreKey[] carlPrekeys = carl.newPreKeys(0, 8);

        String daveId = UUID.randomUUID().toString();
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
