package com.wire.bots.cryptobox;

import java.io.Closeable;

public interface ICryptobox extends Closeable {
    PreKey newLastPreKey() throws CryptoException;

    PreKey[] newPreKeys(int start, int num) throws CryptoException;

    byte[] encryptFromPreKeys(String sid, PreKey preKey, byte[] content) throws CryptoException;

    byte[] encryptFromSession(String sid, byte[] content) throws CryptoException;

    byte[] decrypt(String sid, byte[] decode) throws CryptoException;

    void close();

    boolean isClosed();
}
