package de.starima.pfw.base.processor.description.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.assets.api.IAssetProviderProcessor;
import de.starima.pfw.base.processor.context.api.ITransformationContext;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface IDescriptorProcessor extends IProcessor {

    String getPath(); // z.B. "reconDeploymentInstance.file2DeployProcessorTransformator.deploymentProcessorTransformatorChain"
    Optional<IDescriptorProcessor> findDescriptor(String path);
    IDescriptorProcessor getParentDescriptor();// JNDI-Ã¤hnlich
    void setParentDescriptor(IDescriptorProcessor parentDescriptor);

    //allgemeine Beschreibung

    /**
     * Liefert die allgemeine Kurzbeschreibung, wie sie beispielsweise in der Annotation Processor(description) vergeben werden kann.
     * @return
     */
    public String getGeneralDescription();
    public void setGeneralDescription(String description);

    /**
     * Dieser Prozessor kann die allgemeine Beschreibung aus einer definierten Quelle generieren. Im allgemeinen wird das MarkDown oder AsciDoc
     * im resource Verzeichnis der jar Datei sein. Kann aber auch aus einer externen Dokumentenablage (Dokumenten Server) extrahiert werden.
     * @return
     */
    public IAssetProviderProcessor getGeneralAssetProviderProcessor();

    /**
     * Name der Datei/Asset fÃ¼e die allgemeine Beschreibung. Wird vom AssetProvider benutzt, um das Asset zu laden.
     * @return
     */
    public String getGeneralAssetName();

    /**
     * Type des Assets (html, PDF, ...)
     * @return
     */
    public String getGeneralAssetType();

    /**
     * Falls die allgemeine Langbeschreibung im String Format vorliegt, so kann sie hier schon angehangen werden. Dies kann auch durch den assetProvider erfolgen, falls dieser z.B. HTML liefert.
     * @return
     */
    public String getGeneralAsset();

    /**
     * Die URL der allgemeinen Langbeschreibung. Diese wird fÃ¼r gewÃ¶hnlich durch den assetProvider mittels dem assetName und mimetype erzeugt!
     * @return
     */
    public String getGeneralAssetUrl();

    //fachliche Beschreibung

    /**
     * Liefert die spezielle, fachspezifische Kurzbeschreibung wie sie direkt in der Konfiguration hinterlegt werden kann, z.B. im Parameter specialDescription.
     * @return
     */
    public String getSpecialDescription();
    public void setSpecialDescription(String description);

    /**
     * Dieser Prozessor kann die spezielle Beschreibung aus einer definierten Quelle generieren. Dies kann eine externe Dokumentenablage (Dokumenten Server) sein,
     * oder ein Speicherort bei der Konfig selbst sein.
     * @return
     */
    public IAssetProviderProcessor getSpecialAssetProviderProcessor();

    /**
     * Name der Datei/Asset fÃ¼e die fachliche Beschreibung. Wird vom AssetProvider benutzt, um das Asset zu laden.
     * @return
     */
    public String getSpecialAssetName();

    /**
     * Type des Assets
     * @return
     */
    public String getSpecialAssetType();

    /**
     * Falls die fachliche Langbeschreibung im String Format vorliegt, so kann sie hier schon angehangen werden. Dies kann auch durch den assetProvider erfolgen, falls dieser z.B. HTML liefert.
     * @return
     */
    public String getSpecialAsset();

    /**
     * Die URL der fachlichen Langbeschreibung. Diese wird fÃ¼r gewÃ¶hnlich durch den assetProvider mittels dem assetName und mimetype erzeugt!
     * @return
     */
    public String getSpecialAssetUrl();

    /**
     * @deprecated Wird durch die neue, zustandsbasierte Methode ersetzt. Dient nur noch als Einstiegspunkt.
     */
    public Map<String, Map<String, Object>> extractEffectiveParameterMap(Object bean);
    /**
     * @deprecated Wird durch die neue, zustandsbasierte Methode ersetzt. Dient nur noch als Einstiegspunkt.
     */
    public Map<String, Map<String, Object>> extractEffectiveParameterMap(ITransformationContext transformationContext, Object bean);

    /**
     * Die NEUE, zentrale und kontextbasierte Extraktionsmethode.
     * Jeder Deskriptor implementiert seine eigene Logik.
     *
     * @param context          Der Transformationskontext, der das zu verarbeitende Bean und alle Umgebungs-Infos enthÃ¤lt.
     * @param beanParameterMap Die globale Map, die mit den Ergebnissen befÃ¼llt wird.
     * @param visited          Ein Set zur Erkennung von Zyklen.
     */
    void extractEffectiveParameterMap(ITransformationContext context, Map<String, Map<String, Object>> beanParameterMap, Set<Object> visited);

    /**
     * Dieser Prozessor kann die allgemeine Beschreibung aus einer definierten Quelle generieren. Im allgemeinen wird das MarkDown oder AsciDoc
     * im resource Verzeichnis der jar Datei sein. Kann aber auch eine externe Dokumentenablage (Dokumenten Server) sein.
     * @return
     */
    //public IDocRendererProcessor getGeneralDocRendererProcessor();
    //public void setGeneralDocRendererProcessor(IDocRendererProcessor processor);
    /**
     * Dieser Prozessor kann die spezielle Beschreibung aus einer definierten Quelle generieren. Dies kann eine externe Dokumentenablage (Dokumenten Server) sein,
     * oder ein Speicherort bei der Konfig selbst sein.
     * @return
     */
    //public IDocRendererProcessor getSpecialDocRendererProcessor();
    //public void setSpecialDocRendererProcessor(IDocRendererProcessor processor);

    /**
     * Besitzt der sourceProcessor Parameter, die wieder Prozessoren abbilden, dann werden diese hier mit dem Parameternamen registriert.
     * Da jeder Prozessor einen Descriptor und einen ContextProvider haben kann, sollten auch diese hier mitgeliefert werden.
     * TODO: wo werden die temporÃ¤ren Prozessoren gespeichert, die durch createProcessor(...) erzeugt werden?
     *
     * @return
     */
    //public Map<String, List<IProcessor>> getParameterChildProcessors();

    //public IProcessor findParameterChildProcessor(String fullBeanId);
}