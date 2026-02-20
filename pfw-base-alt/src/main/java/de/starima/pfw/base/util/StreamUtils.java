package de.starima.pfw.base.util;

import de.starima.pfw.base.processor.artifact.api.IArtifactProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

@Slf4j
public class StreamUtils {
    /* private constructor to not allow creation of object */
    private StreamUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static InputStream getInputStreamFromSource(IArtifactProvider source) throws IOException {
        if (source.getArtifact(ZipFile.class) != null) {
            ZipFile zipFile = source.getArtifact(ZipFile.class);
            return new FileInputStream(zipFile.getName());
        }

        // if (source.getArtifact(Blob.class) != null) {
        //     Blob blob = source.getArtifact(Blob.class);
        //     return Channels.newInputStream(blob.reader());
        // }
        return null;
    }

    public static InputStream getEntryInputStream(InputStream inputStream, String entryName) throws IOException {
        if (inputStream == null || entryName == null) return null;
        // ZipInputStream wird durch diese Verwendung "try-with-resources" automatisch geschlossen
        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;

            // Iteriere Ã¼ber alle ZIP-EintrÃ¤ge
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(entryName)) {
                    log.debug("process zip entry {} ", entryName);

                    // Erhalte einen separaten InputStream fÃ¼r den aktuellen ZipEntry
                    InputStream entryInputStream = getInputStreamForEntry(zis);

                    zis.closeEntry(); // Beendet den aktuellen Eintrag
                    return entryInputStream;
                }
            }

            return null;
        }
    }

    // Methode, um einen separaten InputStream fÃ¼r einen ZipEntry zu erhalten
    private static InputStream getInputStreamForEntry(ZipInputStream zis) throws IOException {
        // Lies den ZipEntry-Datenstrom in einen ByteArrayOutputStream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = zis.read(buffer)) > -1) {
            baos.write(buffer, 0, len);
        }
        // Erstelle einen neuen ByteArrayInputStream aus den Daten des ZipEntry
        return new ByteArrayInputStream(baos.toByteArray());
    }
}