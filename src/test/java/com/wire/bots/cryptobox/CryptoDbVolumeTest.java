package com.wire.bots.cryptobox;


import com.wire.bots.cryptobox.storage.MemStorage;
import org.junit.AfterClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CryptoDbVolumeTest {
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(12);

    @AfterClass
    public static void clean() throws IOException {
        Path rootPath = Paths.get("data");
        Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testConcurrentMultipleSessions() throws Exception {
        MemStorage storage = new MemStorage();
        String aliceId = UUID.randomUUID().toString();
        CryptoDb alice = new CryptoDb(aliceId, storage);
        PreKey[] aliceKeys = alice.newPreKeys(0, 8);

        final AtomicLong elapse = new AtomicLong(0);
        final AtomicInteger counter = new AtomicInteger(0);
        byte[] bytes = ("Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello " +
                "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello ").getBytes();

        for (int i = 0; i < 1000; i++) {
            executor.execute(() -> {
                try {
                    String bobId = UUID.randomUUID().toString();
                    try (CryptoDb bob = new CryptoDb(bobId, storage)) {
                        bob.encryptFromPreKeys(aliceId, aliceKeys[0], bytes);
                        bob.encryptFromSession(aliceId, bytes);
                        bob.close();
                    }
                    counter.getAndIncrement();
                } catch (CryptoException | IOException e) {
                    System.out.println(e);
                }
            });
        }

        Date s = new Date();
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        Date e = new Date();
        long delta = e.getTime() - s.getTime();
        elapse.getAndAdd(delta);

        System.out.printf("testConcurrentMultipleSessions: Count: %,d,  Elapsed: %,d ms\n", counter.get(), elapse.get());

        alice.close();
    }
}
