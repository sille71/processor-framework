package de.starima.pfw.base.processor.assets;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.assets.api.IAssetProviderProcessor;
import de.starima.pfw.base.processor.request.api.IUrlProviderProcessor;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@Getter
@Setter
@Slf4j
@Processor
public abstract class AbstractAssetProviderProcessor extends AbstractProcessor implements IAssetProviderProcessor {
    @ProcessorParameter(value = "api/docs/", description = "Der externe Basispfad beschreibt z.B. wie die Ressource Ã¼ber einen REST Controller zu erreichen  ist.\n" +
            "Er wird noch ergÃ¤nzt durch das Protokoll, den Server, diesen Prozessor Identifier und den Asset Namen und optionalen Mimetype.\n" +
            "z.B. https://docuserver.de/api/docs/classPathImageRequestConsumer/logo.png")
    private String externalBasePath = "api/docs/";
    @ProcessorParameter(value = "defaultUrlProviderProcessor:documentationUrlProvider", description = "Die Url zum Dokumenten Server (in der Regel die Instanz selbst). Wird im json unter documentationUrlProvider ergÃ¤nzt. Z.B. https://docuserver.de")
    private IUrlProviderProcessor urlProviderProcessor;

    @Override
    public URL getExternalAssetBaseUrl() {
        try {
            URI baseUri = URI.create(this.urlProviderProcessor.getUrl());
            URL url = baseUri.resolve(this.externalBasePath).resolve(ProcessorUtils.toUrlSafe(this.getFullBeanId()) + "/").toURL();
            log.info("{}: external base path: {}, external Asset Base URL: {}",this.getFullBeanId(), this.externalBasePath, url);
            return url;
        } catch (MalformedURLException | NullPointerException e) {
            log.error("{}: can not create url for external asset base {}/{}", this.getFullBeanId(), this.externalBasePath, this.getFullBeanId(), e);
            return null;
        }
    }

    @Override
    public URL getExternalAssetUrl(String assetName, String type) {
        URL url = getExternalAssetBaseUrl();
        try {
            if (url != null) {
                URI uri = url.toURI().resolve(assetName);
                if (type != null) {
                    uri = uri.resolve(type);
                }
                return uri.toURL();
            }
            return null;
        } catch (MalformedURLException | URISyntaxException e) {
            log.error("{}: can not create url for external url base {} and asset {}", this.getFullBeanId(), url, assetName, e);
            return null;
        }
    }
}