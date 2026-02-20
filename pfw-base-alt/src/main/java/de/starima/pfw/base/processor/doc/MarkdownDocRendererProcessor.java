package de.starima.pfw.base.processor.doc;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.assets.api.IAssetProviderProcessor;
import de.starima.pfw.base.util.DocUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Getter @Setter
@Processor
public class MarkdownDocRendererProcessor extends DocRendererProcessor {
    private static final String MARKDOWN_FILE_EXTENSION = ".md";
    @ProcessorParameter(value = "docs")
    private String basePath = "docs";

    @ProcessorParameter(value = "processors")
    private String assetDir = "processors";

    @ProcessorParameter(value = "assets/images")
    private String imagesDir = "assets/images";

    @ProcessorParameter(value = "classPathImageRequestConsumer")
    private IAssetProviderProcessor imageAssetProviderProcessor;

    @Override
    public String renderGeneralDocumentation(IProcessor processor) {
        if (processor == null || !processor.getClass().isAnnotationPresent(Processor.class)) return "";
        Processor pa = processor.getClass().getAnnotation(Processor.class);
        Path basePath = Paths.get(this.basePath);
        Path processorDocDir = basePath.resolve(this.assetDir);
        String docFileName = pa.descriptionAssetName() != null ? pa.descriptionAssetName() + MARKDOWN_FILE_EXTENSION : processor.getProtoTypeIdentifier() + MARKDOWN_FILE_EXTENSION;
        Path mdPath = processorDocDir.resolve(docFileName);

        try {
            return DocUtils.renderMarkdownWithImages(mdPath, URI.create(basePath.resolve(imagesDir).toString()), "!\\[(.*?)\\]\\((.*?)\\)");
        } catch (IOException e) {
            log.info("can not generate documentation for processor {}",processor.getProtoTypeIdentifier(), e);
            return "";
        }
    }

    public enum LayoutStyle {
        DEFAULT, COMPACT
    }

    public String renderHtml(String assetName) throws IOException {
        if (assetName == null || assetName.isEmpty()) return "";
        Path basePath = Paths.get(this.basePath);
        Path assetDir = basePath.resolve(this.assetDir);
        if (!assetName.endsWith(MARKDOWN_FILE_EXTENSION)) {
            assetName = assetName + MARKDOWN_FILE_EXTENSION;
        }
        Path assetPath = assetDir.resolve(assetName);
        String markdown = loadMarkdownWithReferencedImages(assetPath);
        return renderMarkdownToHtml(markdown);
    }

    public byte[] renderPdf(String processorName, LayoutStyle style) throws IOException {
        String html = renderHtml(processorName);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IOException("Fehler beim PDF-Rendering", e);
        }
    }

    public byte[] renderZipArchive(String processorName, LayoutStyle style) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            String basePath = "docs/" + processorName + "/";
            Map<String, InputStream> resources = DocUtils.findAllResources(basePath);

            for (Map.Entry<String, InputStream> entry : resources.entrySet()) {
                String relPath = entry.getKey();
                try (InputStream in = entry.getValue()) {
                    zos.putNextEntry(new ZipEntry(relPath));
                    in.transferTo(zos);
                    zos.closeEntry();
                }
            }

            // HTML erzeugen
            String html = renderHtml(processorName);
            zos.putNextEntry(new ZipEntry("doc.html"));
            zos.write(html.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // PDF erzeugen
            byte[] pdf = renderPdf(processorName, style);
            zos.putNextEntry(new ZipEntry("doc.pdf"));
            zos.write(pdf);
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    private String renderMarkdownToHtml(String markdown) {
        Parser parser = Parser.builder().extensions(Arrays.asList(AttributesExtension.create())).build();
        HtmlRenderer renderer = HtmlRenderer.builder().extensions(Arrays.asList(AttributesExtension.create())).build();
        return renderer.render(parser.parse(markdown));
    }

    private String loadMarkdownWithReferencedImages(Path assetPath) throws IOException {
        ClassLoader cl = getClass().getClassLoader();
        try (InputStream is = cl.getResourceAsStream(assetPath.toString())) {
            if (is == null) {
                log.debug("can not load asset {}", assetPath);
                throw new FileNotFoundException("Markdown-Datei nicht gefunden: " + assetPath);
            }
            String markdown = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return DocUtils.replaceImagePathsWithUrls(markdown, getImagesBasePath(), "!\\[(.*?)\\]\\((.*?)\\)");
        }
    }

    private URI getImagesBasePath() {
        if (this.imageAssetProviderProcessor != null && this.imageAssetProviderProcessor.getExternalAssetBaseUrl() != null) {
            try {
                return this.imageAssetProviderProcessor.getExternalAssetBaseUrl().toURI();
            } catch (URISyntaxException e) {
                log.error("{}: can not get images base path", this.getFullBeanId(), e);
            }
        }
        return null;
    }

    /**
     * LÃ¤d die MarDown Datei und ersetzt die Bilder im MarkDown durch base64 DataUrls
     * @param docSourcePath - Pfad zum MarkDown File (inkl. File )
     * @param imagesPath - Pfad zu den verwendeten Bildern
     * @return
     * @throws IOException
     */
    private String loadMarkdownWithBase64Images(Path docSourcePath, Path imagesPath) throws IOException {
        ClassLoader cl = getClass().getClassLoader();
        try (InputStream is = cl.getResourceAsStream(docSourcePath.toString())) {
            if (is == null) {
                throw new FileNotFoundException("Markdown-Datei nicht gefunden: " + docSourcePath);
            }
            String markdown = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            DocUtils.replaceImagePathsWithBase64DataUrls(markdown, imagesPath, "!\\[(.*?)\\]\\((.*?)\\)");
            return markdown;
        }
    }

}