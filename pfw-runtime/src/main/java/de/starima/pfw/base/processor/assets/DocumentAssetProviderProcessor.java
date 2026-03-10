package de.starima.pfw.base.processor.assets;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.assets.domain.Asset;
import de.starima.pfw.base.processor.doc.api.IDocRendererProcessor;
import de.starima.pfw.base.processor.request.api.IRequestConsumerProcessor;
import de.starima.pfw.base.processor.request.domain.AssetHttpRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

@Getter @Setter
@Slf4j
@Processor
public class DocumentAssetProviderProcessor extends AbstractAssetProviderProcessor implements IRequestConsumerProcessor {
    @ProcessorParameter(value = "markdownDocRendererProcessor", description = "Der Renderer wird  verwendet, um eine detailiertere, allgemeine Beschreibungen fÃ¼r Prozessoren nachzuladen (z.B. in Form von Html). \n"+
            "Die Herkunft der Dokumentation kann so gekapselt werden. Per default (markdownDocRendererProcessor) befindet sich die Dokumentation unter resource/docs in den zugehÃ¶rigen jar Files.")
    private IDocRendererProcessor docRendererProcessor;

    //TODO: der type spielt hier nochkeine Rolle, wird erst interessant, wenn der Provider ihn auswerten kann - evtl in einer chain (isResponsibleFor(mimetype)
    @Override
    public Asset getAsset(String assetName, String type) {
        if (this.docRendererProcessor == null) return null;
        Asset asset = new Asset();
        asset.setName(assetName);
        asset.setMimeType("text/html");
        try {
            String html = docRendererProcessor.renderHtml(assetName);
            asset.setData(html.getBytes());
            return asset;
        } catch (IOException e) {
            asset.setData("".getBytes());
            log.info("{} can not get asset {}", this.getFullBeanId(), assetName, e);
        }

        return asset;
    }

    @Override
    public String getInternalAssetBasePath() {
        return "";
    }

    @Override
    public Object processRequest(Object requestInput) {
        return requestInput instanceof AssetHttpRequest ? processAssetRequest((AssetHttpRequest) requestInput) : ResponseEntity.notFound().build();
    }

    private ResponseEntity<byte[]> processAssetRequest(final AssetHttpRequest requestInput) {
        Asset asset = getAsset(requestInput.getAssetName(), requestInput.getType());
        if (asset != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(asset.getMimeType() != null ? MediaType.parseMediaType(asset.getMimeType()) : MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(asset.getData().length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(asset.getData());
        }

        return ResponseEntity.notFound().build();
    }
}