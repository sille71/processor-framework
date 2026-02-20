package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.assets.api.IAssetProviderProcessor;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Getter
@Slf4j
@Processor
public abstract class DescriptorProcessor extends AbstractProcessor implements IDescriptorProcessor {
    @ProcessorParameter(ignoreInitialization = true, ignoreExtractParameter = true, description = "Der Ã¼bergeordnete Deskriptor in der Hierarchie.")
    private IDescriptorProcessor parentDescriptor;

    @Override
    public String getPath() {
        throw new UnsupportedOperationException("getPath() must be implemented by subclasses.");
    }

    @Override
    public Optional<IDescriptorProcessor> findDescriptor(String path) {
        throw new UnsupportedOperationException("findDescriptor(String path) must be implemented by subclasses.");
    }

    @ProcessorParameter(description = "Die allgemeine Kurzbeschreibung des Prozessors. Wird in der Regel aus der Processor Annotation erzeugt.")
    private String generalDescription;
    @ProcessorParameter(description = "Die fachspezifische Kurzbeschreibung des Prozessors. Wird in der Konfiguration des Prozessors festgehalten. Jeder Prozessor kann fÃ¼r sein spezielles fachliches Einsathgebiet konfiguriert werden. Somit entstehen Prozessorinstanzen.")
    private String specialDescription;
    @ProcessorParameter(value = "documentAssetProviderProcessor", description = "Der Renderer wird  verwendet, um eine detailiertere, allgemeine Beschreibungen fÃ¼r Prozessoren nachzuladen (z.B. in Form von Html). \n"+
    "Die Herkunft der Dokumentation kann so gekapselt werden. Per default (markdownDocRendererProcessor) befindet sich die Dokumentation unter resource/docs in den zugehÃ¶rigen jar Files.")
    private IAssetProviderProcessor generalAssetProviderProcessor;
    @ProcessorParameter(description = "Name der Datei/Asset fÃ¼e die allgemeine Beschreibung. Wird vom AssetProvider benutzt, um das Asset zu laden.")
    private String generalAssetName;
    @ProcessorParameter(description = "Type des Assets (html, pdf, ...)")
    private String generalAssetType;
    @ProcessorParameter(description = "Falls die Langbeschreibung im String Format vorliegt, so kann sie hier schon angehangen werden. Dies kann auch durch den assetProvider erfolgen, falls dieser z.B. HTML liefert.")
    private String generalAsset;
    @ProcessorParameter(description = "Die URL der Langbeschreibung. Diese wird fÃ¼r gewÃ¶hnlich durch den assetProvider mittels dem assetName und mimetype erzeugt!\n"+
    "Steht also selten direkt in der Konfig, sondern dient vielmehr als output Parameter.")
    private String generalAssetUrl;
    @ProcessorParameter(description = "Der Renderer wird  verwendet, um eine detailiertere, fachliche Beschreibungen fÃ¼r konkrete Prozessorkonfigurationen nachzuladen (z.B. in Form von Html). \n"+
            "Die Herkunft der Dokumentation kann so gekapselt werden. In der Regel wird die fachspezifische Doku nahe der Prozessorkonfiguration selbst abgelegt.")
    private IAssetProviderProcessor specialAssetProviderProcessor;
    @ProcessorParameter(description = "Name der Datei/Asset fÃ¼e die spezielle Beschreibung. Wird vom AssetProvider benutzt, um das Asset zu laden.")
    private String specialAssetName;
    @ProcessorParameter(description = "Type des Assets (html, pdf, ...")
    private String specialAssetType;
    @ProcessorParameter(description = "Falls die fachliche Langbeschreibung im String Format vorliegt, so kann sie hier schon angehangen werden. Dies kann auch durch den assetProvider erfolgen, falls dieser z.B. HTML liefert.")
    private String specialAsset;
    @ProcessorParameter(description = "Die URL der fachlichen Langbeschreibung. Diese wird fÃ¼r gewÃ¶hnlich durch den assetProvider mittels dem assetName und mimetype erzeugt!\n"+
            "Steht also selten direkt in der Konfig, sondern dient vielmehr als output Parameter.")
    private String specialAssetUrl;

    public String getGeneralAssetUrl() {
        if (generalAssetUrl == null && getGeneralAssetProviderProcessor() != null) {
            generalAssetUrl = getGeneralAssetProviderProcessor().getExternalAssetUrl(generalAssetName, generalAssetType).toString();
        }
        return generalAssetUrl;
    }
}