package com.wire.bots.cryptobox;

import java.util.concurrent.ConcurrentHashMap;

public class MemStorage implements IStorage {
    private final ConcurrentHashMap<String, Record> db = new ConcurrentHashMap<>();

    @Override
    public byte[] fetch(String id, String sid) {
        String key = id + sid;

        Record record = db.computeIfAbsent(key, k -> null);
        if (record == null)
            return null;

        for (int i = 0; i < 1000 && record.locked; i++) {
            sleep(10);
            record = db.get(key);
        }
        record.locked = true;
        db.put(key, record);
        return record.data;
    }

    @Override
    public void update(String id, String sid, byte[] data) {
        String key = id + sid;

        Record record = new Record(data);
        record.id = sid;
        db.put(key, record);
    }

    protected void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    class Record {
        String id;
        byte[] data;
        boolean locked;

        Record(byte[] data) {
            this.data = data;
        }
    }
}
