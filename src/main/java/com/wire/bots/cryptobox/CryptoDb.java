package com.wire.bots.cryptobox;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CryptoDb implements Closeable {
    private static final String DATA = "data";
    private final String id;
    private final CryptoBox box;
    private final Storage storage;
    private final String root;

    public CryptoDb(String id, Storage storage) throws CryptoException {
        this.id = id;
        root = String.format("%s/%s", DATA, id);

        box = CryptoBox.open(root);
        this.storage = storage;
    }

    public PreKey newLastPreKey() throws CryptoException {
        return box.newLastPreKey();
    }

    public PreKey[] newPreKeys(int start, int num) throws CryptoException {
        return box.newPreKeys(start, num);
    }

    public byte[] encryptFromPreKeys(String sid, PreKey preKey, byte[] content) throws CryptoException, IOException {
        try {
            begin(sid);
            return box.encryptFromPreKeys(sid, preKey, content);
        } finally {
            end(sid);
        }
    }

    public byte[] encryptFromSession(String sid, byte[] content) throws CryptoException, IOException {
        try {
            begin(sid);
            return box.encryptFromSession(sid, content);
        } finally {
            end(sid);
        }
    }

    public byte[] decrypt(String sid, byte[] decode) throws CryptoException, IOException {
        try {
            begin(sid);
            return box.decrypt(sid, decode);
        } finally {
            end(sid);
        }
    }

    private void begin(String sid) throws IOException {
        byte[] b = storage.fetch(id, sid);
        if (b != null) {
            writeFile(sid, b);
        }
    }

    private void end(String sid) throws IOException {
        byte[] b = readFile(sid);
        if (b != null)
            storage.update(id, sid, b);
    }

    private void writeFile(String sid, byte[] b) throws IOException {
        String file = String.format("%s/sessions/%s", root, sid);
        Files.write(Paths.get(file), b);
    }

    private byte[] readFile(String sid) throws IOException {
        String file = String.format("%s/sessions/%s", root, sid);
        Path path = Paths.get(file);
        return Files.exists(path) ? Files.readAllBytes(path) : null;
    }

    @Override
    public void close() {
        box.close();
    }

    public boolean isClosed() {
        return box.isClosed();
    }
}
