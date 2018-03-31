package com.wire.bots.cryptobox;


import com.wire.bots.cryptobox.storage.MemStorage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

public class CryptoDbTest {
    private final static String bobId = "bob";
    private final static String bobClientId = "bob_device";
    private final static String aliceId = "alice";
    private final static String aliceClientId = "alice_device";
    private static MemStorage storage = new MemStorage();
    private static CryptoDb alice;
    private static CryptoDb bob;
    private static PreKey[] bobKeys;
    private static PreKey[] aliceKeys;

    @BeforeClass
    public static void setUp() throws Exception {
        alice = new CryptoDb(aliceId, storage);
        bob = new CryptoDb(bobId, storage);

        bobKeys = bob.newPreKeys(0, 8);
        aliceKeys = alice.newPreKeys(0, 8);
    }

    @AfterClass
    public static void clean() throws IOException {
        alice.close();
        bob.close();

        // Util.deleteDir("data");
    }

    @Test
    public void testAliceToBob() throws Exception {
        String text = "Hello Bob, This is Alice!";

        // Encrypt using prekeys
        byte[] cipher = alice.encryptFromPreKeys(bobClientId, bobKeys[0], text.getBytes());

        // Decrypt using initSessionFromMessage
        byte[] decrypt = bob.decrypt(aliceClientId, cipher);

        assert Arrays.equals(decrypt, text.getBytes());
        assert text.equals(new String(decrypt));
    }

    @Test
    public void testBobToAlice() throws Exception {
        String text = "Hello Alice, This is Bob!";

        byte[] cipher = bob.encryptFromPreKeys(aliceClientId, aliceKeys[0], text.getBytes());

        // Decrypt using initSessionFromMessage
        byte[] decrypt = alice.decrypt(bobClientId, cipher);

        assert Arrays.equals(decrypt, text.getBytes());
        assert text.equals(new String(decrypt));
    }

    @Test
    public void testSessions() throws Exception {
        String text = "Hello Alice, This is Bob, again!";

        byte[] cipher = bob.encryptFromSession(aliceClientId, text.getBytes());

        // Decrypt using session
        byte[] decrypt = alice.decrypt(bobClientId, cipher);

        assert Arrays.equals(decrypt, text.getBytes());
        assert text.equals(new String(decrypt));
    }

    @Test
    public void testMassiveSessions() throws Exception {
        for (int i = 0; i < 100; i++) {
            String text = "Hello Alice, This is Bob, again! " + i;

            byte[] cipher = bob.encryptFromSession(aliceClientId, text.getBytes());

            // Decrypt using session
            byte[] decrypt = alice.decrypt(bobClientId, cipher);

            assert Arrays.equals(decrypt, text.getBytes());
            assert text.equals(new String(decrypt));

            text = "Hey Bob, How's life? " + i;

            cipher = alice.encryptFromSession(bobClientId, text.getBytes());

            // Decrypt using session
            decrypt = bob.decrypt(aliceClientId, cipher);

            assert Arrays.equals(decrypt, text.getBytes());
            assert text.equals(new String(decrypt));
        }
    }

    @Test
    public void testIdentity() throws Exception {
        final String carlId = "carl";
        final String dir = "data/" + carlId;

        CryptoDb carl = new CryptoDb(carlId, storage);
        PreKey[] carlPrekeys = carl.newPreKeys(0, 8);

        String daveId = "dave";
        String davePath = String.format("data/%s", daveId);
        CryptoBox dave = CryptoBox.open(davePath);
        PreKey[] davePrekeys = dave.newPreKeys(0, 8);

        String text = "Hello Bob, This is Carl!";

        // Encrypt using prekeys
        byte[] cipher = dave.encryptFromPreKeys(carlId, carlPrekeys[0], text.getBytes());
        byte[] decrypt = carl.decrypt(daveId, cipher);
        assert Arrays.equals(decrypt, text.getBytes());
        assert text.equals(new String(decrypt));

        carl.close();
        Util.deleteDir(dir);

        cipher = dave.encryptFromSession(carlId, text.getBytes());
        carl = new CryptoDb(carlId, storage);
        decrypt = carl.decrypt(daveId, cipher);

        assert Arrays.equals(decrypt, text.getBytes());
        assert text.equals(new String(decrypt));

        carl.close();
        Util.deleteDir(dir);

        carl = new CryptoDb(carlId, storage);

        cipher = carl.encryptFromPreKeys(daveId, davePrekeys[0], text.getBytes());
        decrypt = dave.decrypt(carlId, cipher);
        assert Arrays.equals(decrypt, text.getBytes());
        assert text.equals(new String(decrypt));

        carl.close();
    }
}
