package de.starima.pfw.base.util;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DocUtils {
    public static Map<String, InputStream> findAllResources(String basePath) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> roots = cl.getResources(basePath);
        Map<String, InputStream> result = new LinkedHashMap<>();

        while (roots.hasMoreElements()) {
            URL url = roots.nextElement();
            if (url.getProtocol().equals("file")) {
                collectFromDirectory(result, new File(url.getPath()), basePath);
            } else if (url.getProtocol().equals("jar")) {
                collectFromJar(result, url, basePath);
            }
        }

        return result;
    }

    private static void collectFromDirectory(Map<String, InputStream> map, File dir, String basePath) throws IOException {
        Path base = dir.toPath();
        Files.walk(base)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String relPath = base.relativize(path).toString().replace(File.separatorChar, '/');
                    try {
                        map.put(relPath, new FileInputStream(path.toFile()));
                    } catch (FileNotFoundException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    private static void collectFromJar(Map<String, InputStream> map, URL jarUrl, String basePath) throws IOException {
        String[] parts = jarUrl.getPath().split("!/");
        String jarFilePath = parts[0].replaceFirst("file:", "");
        try (JarFile jar = new JarFile(URLDecoder.decode(jarFilePath, "UTF-8"))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(basePath) && !entry.isDirectory()) {
                    String relPath = name.substring(basePath.length());
                    InputStream is = jar.getInputStream(entry);
                    map.put(relPath, is);
                }
            }
        }
    }

    public static String buildImageUrl(String processorName, String relativePath) {
        return "/api/docs/images/" + processorName + "/" + relativePath.replace(" ", "%20");
    }


    public static String renderMarkdownWithImages(Path mdPath, URI imagesPath, String regex) throws IOException {
        String markdown = Files.readString(mdPath, StandardCharsets.UTF_8);
        String replaced = replaceImagePathsWithUrls(markdown, imagesPath, regex);
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(parser.parse(replaced));
    }

    /**
     * Ersetzt die Bildpfade im text (z.B. markdown) durch Base64 data Urls. Somit kann kompaktes Html geliefert werden.
     * Ist aber nur fÃ¼r kleine Bilder geeignet und der Browser kann nicht cachen. AuÃŸerdem werden die Bilder durch Base64 aufgeblÃ¤ht.
     * Der bessere Weg ist die Refernzierung als URL.
     * Wenn zukÃ¼nftig nicht benÃ¶tigt wird, dann kann das wieder weg. War ein erster Versuch.
     * @param text
     * @param imagesPath
     * @param regex
     * @return
     * @throws IOException
     */
    public static String replaceImagePathsWithBase64DataUrls(String text, Path imagesPath, String regex) throws IOException {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        Map<String, InputStream> images = findAllResources(imagesPath.toString());
        //cache falls images 2 mal verwendet werden (Inputstream kann oft nur einmal gelesen werden, wenn keine reset Methode implementiert ist)
        HashMap<String, byte[]> imageCache = new LinkedHashMap<>();
        while (matcher.find()) {
            String alt = matcher.group(1);
            String path = matcher.group(2);
            byte[] image = imageCache.get(imagesPath.resolve(path).toString());
            if (image == null) {
                InputStream imageStream = images.get(imagesPath.resolve(path).toString());
                if (imageStream != null) {
                    imageCache.put(imagesPath.resolve(path).toString(), imageStream.readAllBytes());
                }
            }
            if (image != null) {
                String dataUrl = encodeImageAsDataUrl(image);
                matcher.appendReplacement(sb, "![" + alt + "](" + dataUrl + ")");
            }
            else {
                log.warn("Could not find image {}", imagesPath.resolve(path).toString());
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Ersetzt die Bildpfade im text (z.B. markdown) durch externe Urls. Damit kann das Mardown auf dem Server als Html gerendert und
     * ausgeliefert werden. Die Bildpfade verweisen dabei auf ein kontrolliertes api, unf kÃ¶nnen so remote nachgeladen werden.
     * @param text - MarkDown, in dem die image Pfade durch externe Pfade ersetzt werden mÃ¼ssen
     * @param baseUri
     * @param regex
     * @return
     * @throws IOException
     */
    public static String replaceImagePathsWithUrls(String text, URI baseUri, String regex) throws IOException {
        if (baseUri == null) return text;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String alt = matcher.group(1);
            String oldImagePath = matcher.group(2);
            // extrahiere den Bild Namen aus der alten Mardown URL
            String imageName = oldImagePath.substring(oldImagePath.lastIndexOf("/") + 1);
            matcher.appendReplacement(sb, "![" + alt + "](" + baseUri.resolve(imageName) + ")");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String encodeImageAsDataUrl(byte[] image) throws IOException {
        Tika tika = new Tika();
        String mimeType = tika.detect(image);
        String base64 = Base64.getEncoder().encodeToString(image);
        return "data:" + mimeType + ";base64," + base64;
    }

    public static String encodeImageAsDataUrl(Path imagePath) throws IOException {
        if (!Files.exists(imagePath)) {
            throw new NoSuchFileException("Bild nicht gefunden: " + imagePath);
        }
        byte[] bytes = Files.readAllBytes(imagePath);
        String mimeType = Files.probeContentType(imagePath);
        String base64 = Base64.getEncoder().encodeToString(bytes);
        return "data:" + mimeType + ";base64," + base64;
    }
}