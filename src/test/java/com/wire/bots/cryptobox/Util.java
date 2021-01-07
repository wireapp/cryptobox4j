package com.wire.bots.cryptobox;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Util {
    public static void deleteDir(String dir) throws IOException {
        Path rootPath = Paths.get(dir);
        Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public static void assertDecrypted(byte[] decrypt, String text) {
        assertArrayEquals(decrypt, text.getBytes());
        assertEquals(text, new String(decrypt));
    }

}
