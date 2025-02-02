package de.tu_darmstadt.seemoo.nfcgate.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {
    public static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        byte[] data = new byte[16384];
        int n;

        // while not end of stream
        while ((n = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, n);
        }

        return buffer.toByteArray();
    }
}
