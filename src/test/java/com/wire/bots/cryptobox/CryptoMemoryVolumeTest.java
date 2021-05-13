package com.wire.bots.cryptobox;


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

public class CryptoMemoryVolumeTest {
    private ScheduledExecutorService executor;

    @BeforeEach
    public void setUp() {
        executor = new ScheduledThreadPoolExecutor(12);
    }

    @Test
    public void testConcurrentMultipleSessions() throws Exception {
        final int count = 1000;

        MemStorage storage = new MemStorage();
        String aliceId = UUID.randomUUID().toString();
        CryptoDb alice = new CryptoDb(aliceId, storage);
        PreKey[] aliceKeys = alice.newPreKeys(0, count);

        final AtomicInteger counter = new AtomicInteger(0);
        byte[] bytes = ("Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello ").getBytes();

        ArrayList<CryptoDb> boxes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            try {
                String bobId = UUID.randomUUID().toString();
                CryptoDb bob = new CryptoDb(bobId, storage);
                bob.encryptFromPreKeys(aliceId, aliceKeys[i], bytes);
                boxes.add(bob);
            } catch (CryptoException | IOException e) {
                System.out.println(e);
            }
        }

        Date s = new Date();
        AtomicBoolean testFailed = new AtomicBoolean(false);
        for (CryptoDb bob : boxes) {
            executor.execute(() -> {
                try {
                    bob.encryptFromSession(aliceId, bytes);
                    counter.getAndIncrement();
                } catch (Exception e) {
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

        alice.close();
        for (CryptoDb bob : boxes) {
            bob.close();
        }

        if (testFailed.get()) {
            Assertions.fail("See logs");
        }
    }
}
