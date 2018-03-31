package com.wire.bots.cryptobox;

public interface IStorage {
    IRecord fetchSession(String id, String sid);

    byte[] fetchIdentity(String id);

    void insertIdentity(String id, byte[] data);

    PreKey[] fetchPrekeys(String id);

    void insertPrekey(String id, int kid, byte[] data);
}
