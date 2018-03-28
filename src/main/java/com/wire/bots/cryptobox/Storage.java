package com.wire.bots.cryptobox;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class Storage {
    private final ConcurrentHashMap<String, byte[]> db = new ConcurrentHashMap<>();

    public byte[] fetch(String id, String sid) {
        return db.computeIfAbsent(id + sid, k -> null);
    }

    public void update(String id, String sid, byte[] b) {
        String key = id + sid;
        byte[] old = db.get(key);
        assert !Arrays.equals(old, b);
        db.put(key, b);
    }
}
