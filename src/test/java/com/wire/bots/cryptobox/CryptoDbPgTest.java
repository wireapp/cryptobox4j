package com.wire.bots.cryptobox;


import com.wire.bots.cryptobox.storage.PgStorage;
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
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CryptoDbPgTest {
    private static String bobId;
    private static String aliceId;
    private static CryptoDb alice;
    private static CryptoDb bob;
    private static PreKey[] bobKeys;
    private static PreKey[] aliceKeys;
    private static PgStorage storage;

    @BeforeClass
    public static void setUp() throws Exception {
        Random random = new Random();
        aliceId = "" + random.nextInt();
        bobId = "" + random.nextInt();

        storage = new PgStorage();
        alice = new CryptoDb(aliceId, storage);
        bob = new CryptoDb(bobId, storage);

        bobKeys = bob.newPreKeys(0, 1);
        aliceKeys = alice.newPreKeys(0, 1);
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
        byte[] cipher = alice.encryptFromPreKeys(bobId, bobKeys[0], text.getBytes());

        // Decrypt using initSessionFromMessage
        byte[] decrypt = bob.decrypt(aliceId, cipher);

        assert Arrays.equals(decrypt, text.getBytes());
        assert text.equals(new String(decrypt));
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
    public void testSessions() throws Exception {
        String text = "Hello Alice, This is Bob, again!";

        byte[] cipher = bob.encryptFromSession(aliceId, text.getBytes());

        // Decrypt using session
        byte[] decrypt = alice.decrypt(bobId, cipher);

        assert Arrays.equals(decrypt, text.getBytes());
        assert text.equals(new String(decrypt));
    }

    @Test
    public void testMassiveSessions() throws Exception {
        Date s = new Date();
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
        Date e = new Date();
        long delta = e.getTime() - s.getTime();

        System.out.printf("Count: %,d,  Elapsed: %,d ms\n", 100, delta);
    }

    @Test
    public void testConcurrentDifferentCBSessions() throws Exception {
        Random random = new Random();
        String aliceId = "" + random.nextInt();
        CryptoDb alice = new CryptoDb(aliceId, storage);
        PreKey[] aliceKeys = alice.newPreKeys(0, 8);

        final AtomicLong elapse = new AtomicLong(0);
        final AtomicInteger counter = new AtomicInteger(0);
        byte[] bytes = ("Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello ").getBytes();

        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(12);

        for (int i = 0; i < 10000; i++) {
            executor.execute(() -> {
                try {
                    String bobId = "" + random.nextInt();
                    try (CryptoDb bob = new CryptoDb(bobId, storage)) {
                        bob.encryptFromPreKeys(aliceId, aliceKeys[0], bytes);
                        bob.encryptFromSession(aliceId, bytes);
                        bob.close();
                    }
                    counter.getAndIncrement();
                } catch (CryptoException | IOException e) {
                    System.out.println("testConcurrentDifferentCBSessions: " + e.toString());
                }
            });
        }

        Date s = new Date();
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        Date e = new Date();
        long delta = e.getTime() - s.getTime();
        elapse.getAndAdd(delta);

        System.out.printf("Count: %,d,  Elapsed: %,d ms\n", counter.get(), elapse.get());

        alice.close();
    }

    @Test
    public void testConcurrentSessions() throws Exception {
        final String text = "Hello Alice, This is Bob, again! ";

        bob.encryptFromPreKeys(aliceId, aliceKeys[0], text.getBytes());

        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(4);
        final AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < 100; i++) {
            executor.execute(() -> {
                try {
                    bob.encryptFromSession(aliceId, text.getBytes());
                    counter.getAndIncrement();
                } catch (CryptoException | IOException e) {
                    System.out.println("testConcurrentSessions: " + e.toString());
                }
            });
        }
        Date s = new Date();
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
        Date e = new Date();
        long delta = e.getTime() - s.getTime();

        System.out.printf("Count: %,d,  Elapsed: %,d ms\n", counter.get(), delta);
    }
}
