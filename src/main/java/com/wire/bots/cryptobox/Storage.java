package com.wire.bots.cryptobox;

import java.util.concurrent.ConcurrentHashMap;

public class Storage {
    private final ConcurrentHashMap<String, Record> db = new ConcurrentHashMap<>();

    public class Record {
        String id;
        byte[] data;
        boolean locked;
    }

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

    public void update(String id, String sid, byte[] data) {
        String key = id + sid;

        Record record = new Record();
        record.id = sid;
        record.data = data;
        db.put(key, record);
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}
