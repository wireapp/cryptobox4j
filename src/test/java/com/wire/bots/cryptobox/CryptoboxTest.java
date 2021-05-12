package com.wire.bots.cryptobox;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.wire.bots.cryptobox.Util.assertDecrypted;

public class CryptoboxTest {
    private String bobId;
    private String aliceId;

    private CryptoBox alice;
    private CryptoBox bob;
    private PreKey[] bobKeys;
    private PreKey[] aliceKeys;

    private String rootFolder;

    @BeforeEach
    public void setUp() throws Exception {
        rootFolder = "cryptobox-test-data-" + UUID.randomUUID();

        aliceId = UUID.randomUUID().toString();
        bobId = UUID.randomUUID().toString();

        String alicePath = String.format("%s/%s", rootFolder, aliceId);
        String bobPath = String.format("%s/%s", rootFolder, bobId);

        alice = CryptoBox.open(alicePath);
        bob = CryptoBox.open(bobPath);

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
        byte[] cipher = alice.encryptFromPreKeys(bobId, bobKeys[0], text.getBytes());

        // Decrypt using initSessionFromMessage
        byte[] decrypt = bob.decrypt(aliceId, cipher);

        assertDecrypted(decrypt, text);

        for (int i = 0; i < 10; i++) {
            // Encrypt using session
            cipher = alice.encryptFromSession(bobId, text.getBytes());

            // Decrypt using session
            decrypt = bob.decrypt(aliceId, cipher);

            assertDecrypted(decrypt, text);
        }
    }

    @Test
    public void testBobToAlice() throws Exception {
        String text = "Hello Alice, This is Bob!";

        byte[] cipher = bob.encryptFromPreKeys(aliceId, aliceKeys[0], text.getBytes());

        // Decrypt using initSessionFromMessage
        byte[] decrypt = alice.decrypt(bobId, cipher);

        assertDecrypted(decrypt, text);
    }

    @Test
    public void testMassiveSessions() throws Exception {
        String text = "Hello Bob, This is Alice!";
        // Encrypt using prekeys
        byte[] cipher = alice.encryptFromPreKeys(bobId, bobKeys[0], text.getBytes());
        // Decrypt using initSessionFromMessage
        byte[] decrypt = bob.decrypt(aliceId, cipher);

        assertDecrypted(decrypt, text);

        for (int i = 0; i < 100; i++) {
            text = "Hello Alice, This is Bob, again! " + i;

            cipher = bob.encryptFromSession(aliceId, text.getBytes());
            Assertions.assertNotNull(cipher);

            // Decrypt using session
            decrypt = alice.decrypt(bobId, cipher);

            assertDecrypted(decrypt, text);

            text = "Hey Bob, How's life? " + i;

            cipher = alice.encryptFromSession(bobId, text.getBytes());

            // Decrypt using session
            decrypt = bob.decrypt(aliceId, cipher);

            assertDecrypted(decrypt, text);
        }
    }

    @Test
    public void testConcurrentMultipleSessions() throws Exception {
        final int count = 1000;
        final String aliceId = UUID.randomUUID().toString();
        final CryptoBox alice = CryptoBox.open(String.format("%s/%s", rootFolder, aliceId));
        final PreKey[] aliceKeys = alice.newPreKeys(0, count);

        final AtomicInteger counter = new AtomicInteger(0);
        final byte[] bytes = ("Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello ").getBytes();


        ArrayList<CryptoBox> boxes = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String bobId = UUID.randomUUID().toString();
            CryptoBox bob = CryptoBox.open(String.format("%s/%s", rootFolder, bobId));
            bob.encryptFromPreKeys(aliceId, aliceKeys[i], bytes);
            boxes.add(bob);
        }

        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(12);
        Date s = new Date();
        AtomicBoolean testFailed = new AtomicBoolean(false);
        for (CryptoBox bob : boxes) {
            executor.execute(() -> {
                try {
                    bob.encryptFromSession(aliceId, bytes);
                    counter.getAndIncrement();
                } catch (CryptoException e) {
                    System.out.println("testConcurrentDifferentCBSessions: " + e.getMessage());
                    e.printStackTrace();
                    testFailed.set(true);
                }
            });
        }

        executor.shutdown();
        //noinspection ResultOfMethodCallIgnored
        executor.awaitTermination(60, TimeUnit.SECONDS);

        Date e = new Date();
        long delta = e.getTime() - s.getTime();

        System.out.printf("testConcurrentMultipleSessions: Count: %,d,  Elapsed: %,d ms, avg: %.1f/sec\n",
                counter.get(), delta, (count * 1000f) / delta);

        for (CryptoBox bob : boxes) {
            bob.close();
        }
        alice.close();

        if (testFailed.get()) {
            Assertions.fail("See logs");
        }
    }

    private static String hexify(byte[] bytes) {
        StringBuilder buf = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i += 2) {
            buf.append((char) bytes[i]);
            buf.append((char) bytes[i + 1]);
            buf.append(" ");
        }
        return buf.toString().trim();
    }

    private static String encode(byte[] bytes) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        StringBuilder buf = new StringBuilder(bytes.length * 2);
        for (byte aByte : bytes) {
            buf.append(hexDigits[(aByte & 0xf0) >> 4]);
            buf.append(hexDigits[aByte & 0x0f]);
        }
        return buf.toString();
    }

    @Test
    public void testFingerprint() throws Exception {
        byte[] localFingerprint = alice.getLocalFingerprint();
        byte[] identity = alice.getIdentity();
        String hexify = hexify(localFingerprint);

        System.out.println(hexify);
    }
}
