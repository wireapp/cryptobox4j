package com.wire.bots.cryptobox;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CryptoDbConcurrentTest {
    private final static String bobId = "bob";
    private final static String aliceId = "alice";
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(4);
    private static CryptoDb alice;
    private static CryptoDb bob;
    private static PreKey[] bobKeys;
    private static PreKey[] aliceKeys;

    @BeforeClass
    public static void setUp() throws Exception {
        alice = new CryptoDb(aliceId, new Storage());
        bob = new CryptoDb(bobId, new Storage());

        bobKeys = bob.newPreKeys(0, 8);
        aliceKeys = alice.newPreKeys(0, 8);
    }

    @AfterClass
    public static void clean() throws IOException {
        alice.close();
        bob.close();
    }

    @Test
    public void testConcurrentSessions() throws Exception {
        byte[] b = alice.encryptFromPreKeys(bobId, bobKeys[0], "Hello".getBytes());
        bob.decrypt(aliceId, b);
        b = bob.encryptFromPreKeys(aliceId, aliceKeys[0], "Hello".getBytes());
        alice.decrypt(bobId, b);

        for (int i = 0; i < 1000; i++) {
            executor.execute(() -> {
                try {
                    String text = "Hello Alice, This is Bob, again! ";

                    byte[] cipher = bob.encryptFromSession(aliceId, text.getBytes());

                    // Decrypt using session
                    alice.decrypt(bobId, cipher);

                } catch (CryptoException | IOException e) {
                    //System.out.println(e);
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(20, TimeUnit.SECONDS);
    }
}
