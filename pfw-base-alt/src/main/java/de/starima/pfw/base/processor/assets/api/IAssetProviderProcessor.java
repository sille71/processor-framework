package de.starima.pfw.base.processor.assets.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.assets.domain.Asset;

import java.net.URL;

public interface IAssetProviderProcessor extends IProcessor {
    public Asset getAsset(String assetName, String mimeType);
    //TODO: evtl internal basePath als URL anbieten
    /**
     * dDr interne Basis Pfad zu den Assets, z.B. resources/assets/images
     */
    public String getInternalAssetBasePath();

    public URL getExternalAssetBaseUrl();
    public URL getExternalAssetUrl(String assetName,  String mimeType);
}