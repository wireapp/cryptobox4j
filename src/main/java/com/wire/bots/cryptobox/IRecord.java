package com.wire.bots.cryptobox;

public interface IRecord {
    byte[] getData();

    void persist(byte[] data);
}
