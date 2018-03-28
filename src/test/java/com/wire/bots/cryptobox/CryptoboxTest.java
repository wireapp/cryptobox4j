package com.wire.bots.cryptobox;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;

public class CryptoboxTest {
    private final static String bobId = "bob";
    private final static String bobClientId = "bob_device";
    private final static String aliceId = "alice";
    private final static String aliceClientId = "alice_device";
    private static CryptoBox alice;
    private static CryptoBox bob;
    private static PreKey[] bobKeys;
    private static PreKey[] aliceKeys;

    @BeforeClass
    public static void setUp() throws Exception {
        String alicePath = String.format("data/%s", aliceId);
        String bobPath = String.format("data/%s", bobId);

        alice = CryptoBox.open(alicePath);
        bob = CryptoBox.open(bobPath);

        bobKeys = bob.newPreKeys(0, 8);
        aliceKeys = alice.newPreKeys(0, 8);
    }

    @AfterClass
    public static void clean() throws IOException {
        alice.close();
        bob.close();
        Path rootPath = Paths.get("data");
        Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
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
}
