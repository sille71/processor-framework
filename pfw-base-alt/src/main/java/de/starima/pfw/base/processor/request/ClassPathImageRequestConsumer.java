package de.starima.pfw.base.processor.request;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.assets.AbstractAssetProviderProcessor;
import de.starima.pfw.base.processor.assets.domain.Asset;
import de.starima.pfw.base.processor.request.api.IRequestConsumerProcessor;
import de.starima.pfw.base.processor.request.domain.AssetHttpRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;

@Getter @Setter
@Slf4j
@Processor
public class ClassPathImageRequestConsumer extends AbstractAssetProviderProcessor implements IRequestConsumerProcessor {
    @ProcessorParameter(value = "docs", description = "Der tatsÃ¤chliche Basispfad der Ressource auf dem jeweiligen Zielserver.")
    private String internalBasePath = "docs";

    @ProcessorParameter(value = "assets/images")
    private String imagesDir = "assets/images";

    @Override
    public Object processRequest(Object requestInput) {
        return requestInput instanceof AssetHttpRequest ? processAssetRequest((AssetHttpRequest) requestInput) : ResponseEntity.notFound().build();
    }

    private ResponseEntity<byte[]> processAssetRequest(final AssetHttpRequest requestInput) {
        Asset asset = getAsset(requestInput.getAssetName(), requestInput.getType());
        if (asset != null) {
            return ResponseEntity.ok()
                    .contentType(asset.getMimeType() != null ? MediaType.parseMediaType(asset.getMimeType()) : MediaType.APPLICATION_OCTET_STREAM)
                    .body(asset.getData());
        }

        return ResponseEntity.notFound().build();
    }

    @Override
    public Asset getAsset(String assetName, String mimeType) {
        Path basePath = Paths.get(this.getInternalAssetBasePath());
        Path assetPath = basePath.resolve(assetName);

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(assetPath.toString())) {
            if (is == null) {
                return null;
            }
            Asset asset = new Asset();
            asset.setData(is.readAllBytes());
            asset.setMimeType(URLConnection.guessContentTypeFromName(assetPath.toString()));
            asset.setName(assetPath.toString());
            asset.setSize(asset.getData().length);
            return asset;
        } catch (IOException | NullPointerException e) {
            log.error("{}: kann not get asset from path {}",this.getFullBeanId(), assetPath, e);
            return null;
        }
    }

    @Override
    public String getInternalAssetBasePath() {
        Path basePath = Paths.get(this.internalBasePath);
        Path assetsPath = basePath.resolve(this.imagesDir);
        return assetsPath.toString();
    }
}