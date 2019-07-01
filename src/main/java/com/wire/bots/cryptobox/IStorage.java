package com.wire.bots.cryptobox;

public interface IStorage {
    IRecord fetchSession(String id, String sid) throws StorageException;

    byte[] fetchIdentity(String id) throws StorageException;

    void insertIdentity(String id, byte[] data) throws StorageException;

    PreKey[] fetchPrekeys(String id) throws StorageException;

    void insertPrekey(String id, int kid, byte[] data) throws StorageException;

    void purge(String id) throws StorageException;
}
