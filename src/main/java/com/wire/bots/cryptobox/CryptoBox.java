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

import java.io.File;

/**
 * A <tt>CryptoBox</tt> is an opaque container of all the necessary key material
 * needed for exchanging end-to-end encrypted messages with peers for a single,
 * logical client or device. It maintains a pool of {@link CryptoSession}s for
 * all remote peers.
 * <p>
 * <p>Every cryptographic session with a peer is represented by a {@link CryptoSession}.
 * These sessions are pooled by a <tt>CryptoBox</tt>, i.e. if a session with the
 * same session ID is requested multiple times, the same instance is returned.
 * Consequently, <tt>CryptoSession</tt>s are kept in memory once loaded. They
 * can be explicitly closed through {@link CryptoBox#closeSession} or
 * {@link CryptoBox#}. All loaded sessions are implicitly closed
 * when the <tt>CryptoBox</tt> itself is closed via {@link CryptoBox#close}.
 * Note that it is considered programmer error to let a <tt>CryptoBox</tt>
 * become unreachable and thus eligible for garbage collection without having
 * called {@link CryptoBox#close}, even though this class overrides {@link Object#finalize}
 * as an additional safety net for deallocating all native resources.
 * </p>
 * <p>
 * <p>A <tt>CryptoBox</tt> is thread-safe.</p>
 *
 * @see CryptoSession
 */
final public class CryptoBox implements ICryptobox {
    /**
     * The max ID of an ephemeral prekey generated by {@link #newPreKeys}.
     */
    private static final int MAX_PREKEY_ID = 0xFFFE;

    static {
        System.loadLibrary("sodium");
        System.loadLibrary("cryptobox");
        System.loadLibrary("cryptobox-jni");
    }

    private long ptr;

    private CryptoBox(long ptr) {
        this.ptr = ptr;
    }

    /**
     * Open a <tt>CryptoBox</tt> that operates on the given directory.
     * <p>
     * The given directory must exist and be writeable.
     * <p>
     * <p>Note: Do not open multiple boxes that operate on the same or
     * overlapping directories. Doing so results in undefined behaviour.</p>
     *
     * @param dir The root storage directory of the box.
     */
    public static CryptoBox open(String dir) throws CryptoException {
        new File(dir).mkdirs();
        return jniOpen(dir);
    }

    /**
     * Open a <tt>CryptoBox</tt> that operates on the given directory, using
     * an existing external identity.
     * <p>
     * The given identity must match the (public or complete) identity that
     * the <tt>CryptoBox</tt> already has, if any.
     * <p>
     * The given directory must exist and be writeable.
     * <p>
     * <p>Note: Do not open multiple boxes that operate on the same or
     * overlapping directories. Doing so results in undefined behaviour.</p>
     *
     * @param dir  The root storage directory of the box.
     * @param id   The serialised external identity to use.
     * @param mode The desired local identity storage.
     */
    public static CryptoBox openWith(String dir, byte[] id, IdentityMode mode) throws CryptoException {
        new File(dir).mkdirs();
        switch (mode) {
            case COMPLETE:
                return jniOpenWith(dir, id, 0);
            case PUBLIC:
                return jniOpenWith(dir, id, 1);
            default:
                throw new IllegalStateException("Unexpected IdentityMode");
        }
    }

    private native static CryptoBox jniOpen(String dir) throws CryptoException;

    private native static CryptoBox jniOpenWith(String dir, byte[] id, int mode) throws CryptoException;

    private native static PreKey jniNewLastPreKey(long ptr) throws CryptoException;

    private native static PreKey[] jniNewPreKeys(long ptr, int start, int num) throws CryptoException;

    private native static byte[] jniGetLocalFingerprint(long ptr) throws CryptoException;

    private native static CryptoSession jniInitSessionFromPreKey(long ptr, String sid, byte[] prekey) throws CryptoException;

    private native static SessionMessage jniInitSessionFromMessage(long ptr, String sid, byte[] message) throws CryptoException;

    private native static CryptoSession jniLoadSession(long ptr, String sid) throws CryptoException;

    private native static void jniDeleteSession(long ptr, String sid) throws CryptoException;

    private native static byte[] jniCopyIdentity(long ptr) throws CryptoException;

    private native static void jniClose(long ptr);

    /**
     * Copy the long-term identity from this <tt>CryptoBox</tt>.
     *
     * @return The opaque, serialised identity to be stored in a safe place or
     * transmitted over a safe channel for subsequent use with
     * {@link CryptoBox#openWith}.
     */
    public byte[] copyIdentity() throws CryptoException {
        errorIfClosed();
        return jniCopyIdentity(this.ptr);
    }

    /**
     * Get the local fingerprint as a hex-encoded byte array.
     */
    public byte[] getLocalFingerprint() throws CryptoException {
        errorIfClosed();
        return jniGetLocalFingerprint(this.ptr);
    }

    /**
     * Generate a new last prekey.
     * <p>
     * The last prekey is never removed as a result of {@link #initSessionFromMessage}.
     */
    public PreKey newLastPreKey() throws CryptoException {
        errorIfClosed();
        return jniNewLastPreKey(this.ptr);
    }

    /**
     * Generate a new batch of ephemeral prekeys.
     * <p>
     * If <tt>start + num > {@link #MAX_PREKEY_ID}<tt/> the IDs wrap around and start
     * over at 0. Thus after any valid invocation of this method, the last generated
     * prekey ID is always <tt>(start + num) % ({@link #MAX_PREKEY_ID} + 1)</tt>. The caller
     * can remember that ID and feed it back into {@link #newPreKeys} as the start
     * ID when the next batch of ephemeral keys needs to be generated.
     *
     * @param start The ID (>= 0 and <= {@link #MAX_PREKEY_ID}) of the first prekey to generate.
     * @param num   The total number of prekeys to generate (> 0 and <= {@link #MAX_PREKEY_ID}).
     */
    public PreKey[] newPreKeys(int start, int num) throws CryptoException {
        if (start < 0 || start > MAX_PREKEY_ID) {
            throw new IllegalArgumentException("start must be >= 0 and <= " + MAX_PREKEY_ID);
        }
        if (num < 1 || num > MAX_PREKEY_ID) {
            throw new IllegalArgumentException("num must be >= 1 and <= " + MAX_PREKEY_ID);
        }
        errorIfClosed();
        return jniNewPreKeys(this.ptr, start, num);
    }

    /**
     * Inits the session from the prekey and encrypts the given content
     *
     * @param sid     Identifier in our case: userId_clientId
     * @param content Unencrypted binary content to be encrypted
     * @return Cipher
     * @throws CryptoException throws Exception
     */
    public byte[] encryptFromPreKeys(String sid, PreKey preKey, byte[] content) throws CryptoException {
        CryptoSession cryptoSession = initSessionFromPreKey(sid, preKey);
        return cryptoSession.encrypt(content);
    }

    /**
     * Tries to fetch/open a session for the given sid if it exists on the hdd and encrypts the given content
     *
     * @param sid     Identifier in our case: userId_clientId
     * @param content Unencrypted binary content to be encrypted
     * @return Cipher or NULL in case there is no session for the given {@param #sid}
     * @throws CryptoException throws Exception
     */
    public byte[] encryptFromSession(String sid, byte[] content) throws CryptoException {
        CryptoSession session = tryGetSession(sid);
        if (session != null) {
            return session.encrypt(content);
        }
        return null;
    }

    /**
     * Decrypt cipher either using existing session or it creates new session from this cipher and decrypts
     *
     * @param sid    Session Id
     * @param decode cipher
     * @return Decrypted bytes
     * @throws CryptoException throws Exception
     */
    public byte[] decrypt(String sid, byte[] decode) throws CryptoException {
        CryptoSession cryptoSession = tryGetSession(sid);
        if (cryptoSession != null) {
            return cryptoSession.decrypt(decode);
        }
        SessionMessage sessionMessage = initSessionFromMessage(sid, decode);
        return sessionMessage.getMessage();
    }

    /**
     * Initialise a {@link CryptoSession} using the prekey of a peer.
     * <p>
     * <p>This is the entry point for the initiator of a session, i.e.
     * the side that wishes to send the first message.</p>
     *
     * @param sid    The ID of the new session.
     * @param prekey The prekey of the peer.
     */
    private CryptoSession initSessionFromPreKey(String sid, PreKey prekey) throws CryptoException {
        errorIfClosed();
        return jniInitSessionFromPreKey(this.ptr, sid, prekey.data);
    }

    /**
     * Initialise a {@link CryptoSession} using a received encrypted message.
     * <p>
     * <p>This is the entry point for the recipient of an encrypted message.</p>
     *
     * @param sid     The ID of the new session.
     * @param message The encrypted (prekey) message.
     */
    private SessionMessage initSessionFromMessage(String sid, byte[] message) throws CryptoException {
        errorIfClosed();
        return jniInitSessionFromMessage(this.ptr, sid, message);
    }

    /**
     * Get an existing session by ID.
     * <p>
     * <p>If the session does not exist, a {@link CryptoException} is thrown
     * with the code {@link CryptoException.Code#SESSION_NOT_FOUND}.</p>
     *
     * @param sid The ID of the session to get.
     */
    private CryptoSession getSession(String sid) throws CryptoException {
        errorIfClosed();
        return jniLoadSession(this.ptr, sid);
    }

    /**
     * Try to get an existing session by ID.
     * <p>
     * <p>Equivalent to {@link #getSession}, except that <tt>null</tt> is
     * returned if the session does not exist.</p>
     *
     * @param sid The ID of the session to get.
     */
    private CryptoSession tryGetSession(String sid) throws CryptoException {
        try {
            return getSession(sid);
        } catch (CryptoException ex) {
            if (ex.code == CryptoException.Code.SESSION_NOT_FOUND) {
                return null;
            }
            throw ex;
        }
    }

    /**
     * Close a session.
     * <p>
     * <p>Note: After a session has been closed, any operations other than
     * <tt>closeSession</tt> are considered programmer error and result in
     * an {@link IllegalStateException}.</p>
     * <p>
     * <p>If the session is already closed, this is a no-op.</p>
     *
     * @param sess The session to close.
     */
    private void closeSession(CryptoSession sess) {
        errorIfClosed();
        sess.close();
    }

    /**
     * Delete a session.
     * <p>
     * If the session is currently loaded, it is automatically closed before
     * being deleted.
     * <p>
     * <p>Note: After a session has been deleted, further messages received from
     * the peer can no longer be decrypted. </p>
     *
     * @param sid The ID of the session to delete.
     */
    private void deleteSession(String sid) throws CryptoException {
        errorIfClosed();
        CryptoSession cryptoSession = getSession(sid);
        if (cryptoSession != null) {
            cryptoSession.close();
        }
        jniDeleteSession(this.ptr, sid);
    }

    /**
     * Close the <tt>CryptoBox</tt>.
     * <p>
     * <p>Note: After a box has been closed, any operations other than
     * <tt>close</tt> are considered programmer error and result in
     * an {@link IllegalStateException}.</p>
     * <p>
     * <p>If the box is already closed, this is a no-op.</p>
     */
    @Override
    public void close() {
        if (ptr == 0) {
            return;
        }
        jniClose(this.ptr);
        ptr = 0;
    }

    public boolean isClosed() {
        return ptr == 0;
    }

    private void errorIfClosed() {
        if (ptr == 0) {
            throw new IllegalStateException("Invalid operation on a closed CryptoBox.");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    /**
     * The desired local storage mode for use with {@link #openWith}.
     */
    public enum IdentityMode {
        COMPLETE, PUBLIC
    }
}
