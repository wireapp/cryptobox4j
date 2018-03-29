package com.wire.bots.cryptobox;

public interface IStorage {
    IRecord fetch(String id, String sid);
}
