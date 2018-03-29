package com.wire.bots.cryptobox.storage;

import com.wire.bots.cryptobox.IRecord;
import com.wire.bots.cryptobox.IStorage;

import java.util.concurrent.ConcurrentHashMap;

public class MemStorage implements IStorage {
    private final ConcurrentHashMap<String, Record> db = new ConcurrentHashMap<>();

    @Override
    public IRecord fetch(String id, String sid) {
        String key = id + sid;

        Record record = db.computeIfAbsent(key, k -> null);
        if (record == null)
            return new Record(key, null);

        for (int i = 0; i < 1000 && record.locked; i++) {
            sleep(10);
            record = db.get(key);
        }
        record.locked = true;
        db.put(key, record);
        return new Record(key, record.data);
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    private class Record implements IRecord {
        private final String key;
        public byte[] data;
        boolean locked;

        Record(String key, byte[] data) {
            this.key = key;
            this.data = data;
        }

        @Override
        public byte[] getData() {
            return data;
        }

        @Override
        public void persist(byte[] data) {
            db.put(key, this);
        }
    }
}
