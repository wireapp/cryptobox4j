package com.wire.bots.cryptobox;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CryptoDbConcurrentTest {
    private final static String bobId;
    private final static String aliceId;
    private static CryptoDb alice;
    private static CryptoDb bob;
    private static PreKey[] bobKeys;
    private static PreKey[] aliceKeys;

    static {
        Random rnd = new Random();
        aliceId = "" + rnd.nextInt();
        bobId = "" + rnd.nextInt();
    }

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

        Util.deleteDir("data");
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
        private byte[] identity;
        private ArrayList<PreKey> preKeys;

        public IRecord fetchSession(String id, String sid) {
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

        @Override
        public byte[] fetchIdentity(String id) {
            return identity;
        }

        @Override
        public void insertIdentity(String id, byte[] data) {
            identity = data;
        }

        @Override
        public PreKey[] fetchPrekeys(String id) {
            return preKeys == null ? null : preKeys.toArray(new PreKey[0]);
        }

        @Override
        public void insertPrekey(String id, int kid, byte[] data) {
            if (preKeys == null)
                preKeys = new ArrayList<>();
            preKeys.add(new PreKey(kid, data));
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
