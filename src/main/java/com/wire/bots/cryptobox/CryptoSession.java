// Copyright (C) 2015 Wire Swiss GmbH <support@wire.com>
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package com.wire.bots.cryptobox;

import java.io.Closeable;

/**
 * A <tt>CryptoSession</tt> represents a cryptographic session with a peer
 * (e.g. client or device) and is used to encrypt and decrypt messages sent
 * and received, respectively.
 * <p>
 * <p>A <tt>CryptoSession</tt> is thread-safe.</p>
 */
final class CryptoSession implements Closeable {
    private final long boxPtr;
    private final String id;
    private long ptr;

    private CryptoSession(long boxPtr, long ptr, String id) {
        this.boxPtr = boxPtr;
        this.ptr = ptr;
        this.id = id;
    }

    private native static void jniSave(long boxPtr, long ptr) throws CryptoException;

    private native static byte[] jniEncrypt(long ptr, byte[] plaintext) throws CryptoException;

    private native static byte[] jniDecrypt(long ptr, byte[] ciphertext) throws CryptoException;

    private native static byte[] jniGetRemoteFingerprint(long ptr) throws CryptoException;

    private native static void jniClose(long ptr);

    /**
     * Save the session, persisting any changes made to the underlying
     * key material as a result of any {@link #encrypt} and {@link #decrypt}
     * operations since the last save.
     */
    void save() throws CryptoException {
        errorIfClosed();
        try {
            jniSave(boxPtr, ptr);
        } finally {
            close();
        }
    }

    /**
     * Encrypt a byte array containing plaintext.
     *
     * @param plaintext The plaintext to encrypt.
     * @return A byte array containing the ciphertext.
     */
    byte[] encrypt(byte[] plaintext) throws CryptoException {
        errorIfClosed();
        try {
            return jniEncrypt(ptr, plaintext);
        } finally {
            save();
        }
    }

    /**
     * Decrypt a byte array containing ciphertext.
     *
     * @param cipher The ciphertext to decrypt.
     * @return A byte array containing the plaintext.
     */
    byte[] decrypt(byte[] cipher) throws CryptoException {
        errorIfClosed();
        try {
            return jniDecrypt(ptr, cipher);
        } finally {
            save();
        }
    }

    @Override
    public void close() {
        if (isClosed()) {
            return;
        }
        jniClose(ptr);
        ptr = 0;
    }

    private boolean isClosed() {
        return ptr == 0;
    }

    private void errorIfClosed() {
        if (isClosed()) {
            throw new IllegalStateException("Invalid operation on a closed CryptoSession.");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    public String getId() {
        return id;
    }
}
