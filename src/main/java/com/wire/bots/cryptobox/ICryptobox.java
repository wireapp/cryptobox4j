package com.wire.bots.cryptobox;

import java.io.Closeable;
import java.io.IOException;

public interface ICryptobox extends Closeable {
    PreKey newLastPreKey() throws CryptoException;

    PreKey[] newPreKeys(int start, int num) throws CryptoException, IOException;

    byte[] encryptFromPreKeys(String sid, PreKey preKey, byte[] content) throws CryptoException, IOException;

    byte[] encryptFromSession(String sid, byte[] content) throws CryptoException, IOException;

    byte[] decrypt(String sid, byte[] decode) throws CryptoException, IOException;

    void close();

    boolean isClosed();
}
