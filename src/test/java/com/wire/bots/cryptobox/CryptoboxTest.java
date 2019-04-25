package com.wire.bots.cryptobox;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CryptoboxTest {
    private final static String bobId;
    private final static String aliceId;
    private static CryptoBox alice;
    private static CryptoBox bob;
    private static PreKey[] bobKeys;
    private static PreKey[] aliceKeys;

    static {
        Random rnd = new Random();
        aliceId = "" + rnd.nextInt();
        bobId = "" + rnd.nextInt();
    }

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

        Util.deleteDir("data");
    }

    @Test
    public void testAliceToBob() throws Exception {
        String text = "Hello Bob, This is Alice!";

        // Encrypt using prekeys
        byte[] cipher = alice.encryptFromPreKeys(bobId, bobKeys[0], text.getBytes());

        // Decrypt using initSessionFromMessage
        byte[] decrypt = bob.decrypt(aliceId, cipher);

        assert Arrays.equals(decrypt, text.getBytes());
        assert text.equals(new String(decrypt));

        for (int i = 0; i < 10; i++) {
            // Encrypt using session
            cipher = alice.encryptFromSession(bobId, text.getBytes());

            // Decrypt using session
            decrypt = bob.decrypt(aliceId, cipher);

            assert Arrays.equals(decrypt, text.getBytes());
            assert text.equals(new String(decrypt));
        }
    }

    @Test
    public void testBobToAlice() throws Exception {
        String text = "Hello Alice, This is Bob!";

        byte[] cipher = bob.encryptFromPreKeys(aliceId, aliceKeys[0], text.getBytes());

        // Decrypt using initSessionFromMessage
        byte[] decrypt = alice.decrypt(bobId, cipher);

        assert Arrays.equals(decrypt, text.getBytes());
        assert text.equals(new String(decrypt));
    }

    @Test
    public void testMassiveSessions() throws Exception {
        for (int i = 0; i < 100; i++) {
            String text = "Hello Alice, This is Bob, again! " + i;

            byte[] cipher = bob.encryptFromSession(aliceId, text.getBytes());

            // Decrypt using session
            byte[] decrypt = alice.decrypt(bobId, cipher);

            assert Arrays.equals(decrypt, text.getBytes());
            assert text.equals(new String(decrypt));

            text = "Hey Bob, How's life? " + i;

            cipher = alice.encryptFromSession(bobId, text.getBytes());

            // Decrypt using session
            decrypt = bob.decrypt(aliceId, cipher);

            assert Arrays.equals(decrypt, text.getBytes());
            assert text.equals(new String(decrypt));
        }
    }

    @Test
    public void testConcurrentMultipleSessions() throws Exception {
        final int count = 1000;
        Random random = new Random();
        String aliceId = "" + random.nextInt();
        CryptoBox alice = CryptoBox.open("data/" + aliceId);
        PreKey[] aliceKeys = alice.newPreKeys(0, count);

        final AtomicInteger counter = new AtomicInteger(0);
        byte[] bytes = ("Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello ").getBytes();


        ArrayList<CryptoBox> boxes = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String bobId = "" + random.nextInt();
            CryptoBox bob = CryptoBox.open("data/" + bobId);
            bob.encryptFromPreKeys(aliceId, aliceKeys[i], bytes);
            boxes.add(bob);
        }

        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(12);
        Date s = new Date();
        for (CryptoBox bob : boxes) {
            executor.execute(() -> {
                try {
                    bob.encryptFromSession(aliceId, bytes);
                    counter.getAndIncrement();
                } catch (CryptoException e) {
                    System.out.println("testConcurrentDifferentCBSessions: " + e.toString());
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        Date e = new Date();
        long delta = e.getTime() - s.getTime();

        System.out.printf("testConcurrentMultipleSessions: Count: %,d,  Elapsed: %,d ms, avg: %.1f/sec\n",
                counter.get(), delta, (count * 1000f) / delta);

        for (CryptoBox bob : boxes) {
            bob.close();
        }
        alice.close();
    }

    private static String hexify(byte bytes[]) {
        StringBuilder buf = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i += 2) {
            buf.append((char) bytes[i]);
            buf.append((char) bytes[i + 1]);
            buf.append(" ");
        }
        return buf.toString().trim();
    }

    private static String encode(byte bytes[]) {
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
