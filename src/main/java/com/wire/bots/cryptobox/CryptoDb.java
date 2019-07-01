package com.wire.bots.cryptobox;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class CryptoDb implements ICryptobox {
    private static final String DATA = "data";
    private final String id;
    private final CryptoBox box;
    private final IStorage storage;
    private final String root;

    public CryptoDb(String id, IStorage storage) throws IOException, CryptoException {
        this(id, storage, DATA);
    }

    public CryptoDb(String id, IStorage storage, String dir) throws IOException, CryptoException {
        this.id = id;
        this.storage = storage;
        this.root = String.format("%s/%s", dir, id);

        writeIdentity(storage.fetchIdentity(id));
        writePrekeys(storage.fetchPrekeys(id));

        this.box = CryptoBox.open(root);
        storage.insertIdentity(id, readIdentity());
    }

    @Override
    public byte[] getIdentity() throws CryptoException {
        return box.getIdentity();
    }

    @Override
    public byte[] getLocalFingerprint() throws CryptoException {
        return box.getLocalFingerprint();
    }

    @Override
    public PreKey newLastPreKey() throws CryptoException {
        try {
            PreKey preKey = box.newLastPreKey();
            persistPreKey(preKey.id);
            return preKey;
        } catch (IOException e) {
            throw new CryptoException(e);
        }
    }

    @Override
    public PreKey[] newPreKeys(int start, int num) throws CryptoException {
        try {
            PreKey[] preKeys = box.newPreKeys(start, num);
            for (PreKey preKey : preKeys)
                persistPreKey(preKey.id);
            return preKeys;
        } catch (IOException e) {
            throw new CryptoException(e);
        }
    }

    @Override
    public byte[] encryptFromPreKeys(String sid, PreKey preKey, byte[] content) throws CryptoException {
        try {
            IRecord record = begin(sid);
            try {
                return box.encryptFromPreKeys(sid, preKey, content);
            } finally {
                end(sid, record);
            }
        } catch (IOException e) {
            throw new CryptoException(e);
        }
    }

    @Override
    public byte[] encryptFromSession(String sid, byte[] content) throws CryptoException {
        try {
            IRecord record = begin(sid);
            try {
                return box.encryptFromSession(sid, content);
            } finally {
                end(sid, record);
            }
        } catch (IOException e) {
            throw new CryptoException(e);
        }
    }

    @Override
    public byte[] decrypt(String sid, byte[] decode) throws CryptoException {
        try {
            IRecord record = begin(sid);
            try {
                return box.decrypt(sid, decode);
            } finally {
                end(sid, record);
            }
        } catch (IOException e) {
            throw new CryptoException(e);
        }
    }

    private IRecord begin(String sid) throws IOException {
        IRecord record = storage.fetchSession(id, sid);
        if (record != null) {
            writeSession(sid, record.getData());
        }
        return record;
    }

    private void end(String sid, IRecord record) throws IOException {
        byte[] b = readSession(sid);
        if (record != null)
            record.persist(b);
    }

    private void writeSession(String sid, byte[] session) throws IOException {
        if (session != null) {
            String file = String.format("%s/sessions/%s", root, sid);
            Files.write(Paths.get(file), session);
        }
    }

    private byte[] readSession(String sid) throws IOException {
        String file = String.format("%s/sessions/%s", root, sid);
        Path path = Paths.get(file);
        return Files.exists(path) ? Files.readAllBytes(path) : null;
    }

    private void writeIdentity(byte[] identity) throws IOException {
        if (identity != null) {
            String file = String.format("%s/identities/local", root);
            Path path = Paths.get(file);
            Files.createDirectories(path.getParent());
            Files.write(path, identity);
        }
    }

    private byte[] readIdentity() throws IOException {
        String file = String.format("%s/identities/local", root);
        Path path = Paths.get(file);
        return Files.exists(path) ? Files.readAllBytes(path) : null;
    }

    private byte[] readPrekey(int kid) throws IOException {
        String file = String.format("%s/prekeys/%d", root, kid);
        Path path = Paths.get(file);
        return Files.exists(path) ? Files.readAllBytes(path) : null;
    }

    private void writePrekeys(PreKey[] preKeys) throws IOException {
        if (preKeys != null) {
            for (PreKey preKey : preKeys) {
                String file = String.format("%s/prekeys/%d", root, preKey.id);
                Path path = Paths.get(file);
                Files.createDirectories(path.getParent());
                Files.write(path, preKey.data);
            }
        }
    }

    private void persistPreKey(int kid) throws IOException {
        byte[] data = readPrekey(kid);
        storage.insertPrekey(id, kid, data);
    }

    @Override
    public void close() {
        box.close();
    }

    @Override
    public boolean isClosed() {
        return box.isClosed();
    }

    public void purge() throws IOException {
        box.close();
        storage.purge(id);
        Path rootPath = Paths.get(root);
        Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}