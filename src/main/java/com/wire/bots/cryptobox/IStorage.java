package com.wire.bots.cryptobox;

public interface IStorage {
    byte[] fetch(String id, String sid);

    void update(String id, String sid, byte[] data);
}
