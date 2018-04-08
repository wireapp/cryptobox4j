package com.wire.bots.cryptobox;

import java.io.Closeable;

public interface ICryptobox extends Closeable {
    PreKey newLastPreKey() throws Exception;

    PreKey[] newPreKeys(int start, int num) throws Exception;

    byte[] encryptFromPreKeys(String sid, PreKey preKey, byte[] content) throws Exception;

    byte[] encryptFromSession(String sid, byte[] content) throws Exception;

    byte[] decrypt(String sid, byte[] decode) throws Exception;

    void close();

    boolean isClosed();
}
