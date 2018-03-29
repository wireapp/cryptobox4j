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
import java.util.Comparator;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CryptoDbConcurrentTest {
    private final static String bobId = "bob";
    private final static String aliceId = "alice";
    private static CryptoDb alice;
    private static CryptoDb bob;
    private static PreKey[] bobKeys;
    private static PreKey[] aliceKeys;
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(12);

    @BeforeClass
    public static void setUp() throws Exception {
        alice = new CryptoDb(aliceId, new _Storage());
        bob = new CryptoDb(bobId, new _Storage());

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
    public void testConcurrentSessions() throws Exception {
        byte[] b = alice.encryptFromPreKeys(bobId, bobKeys[0], "Hello".getBytes());
        bob.decrypt(aliceId, b);
        b = bob.encryptFromPreKeys(aliceId, aliceKeys[0], "Hello".getBytes());
        alice.decrypt(bobId, b);

        for (int i = 0; i < 5000; i++) {
            executor.execute(() -> {
                try {
                    String text = "Hello Alice, This is Bob, again! ";

                    byte[] cipher = bob.encryptFromSession(aliceId, text.getBytes());
                    alice.decrypt(bobId, cipher);
                } catch (CryptoException | IOException e) {
                    System.out.println(e);
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(20, TimeUnit.SECONDS);
    }

    static class _Storage implements IStorage {
        private final Object lock = new Object();
        private Record record;

        public IRecord fetch(String id, String sid) {
            if (record == null)
                return new Record(null);

            boolean acquired = false;
            for (int i = 0; i < 1000; i++) {
                if (acquire()) {
                    acquired = true;
                    break;
                }
                sleep(10);
            }
            if (!acquired)
                System.out.println("Ohh");

            return new Record(record.data);
        }

        private boolean acquire() {
            synchronized (lock) {
                if (!record.locked) {
                    record.locked = true;
                    return true;
                }
                return false;
            }
        }

        void sleep(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ignored) {
            }
        }

        private class Record implements IRecord {
            byte[] data;
            boolean locked;

            Record(byte[] data) {
                this.data = data;
            }

            @Override
            public byte[] getData() {
                return data;
            }

            @Override
            public void persist(byte[] data) {
                synchronized (lock) {
                    record = new Record(data);
                }
            }
        }
    }
}
