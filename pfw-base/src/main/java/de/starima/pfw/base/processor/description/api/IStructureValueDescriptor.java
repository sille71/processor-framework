package de.starima.pfw.base.processor.description.api;

import java.util.List;
import java.util.Map;

/**
 * Beschreibt einen komplexen, zusammengesetzten Wert, der selbst wieder Parameter enthÃ¤lt.
 * Dies ist der Deskriptor fÃ¼r Objekte, die mit @Processor oder @ValueObject annotiert sind.
 * Er fungiert als Container fÃ¼r eine Gruppe von IParameterDescriptor-Instanzen.
 */
public interface IStructureValueDescriptor<T extends IStructureValueDescriptor<T>> extends IValueDescriptor {
    /**
     * erweitert den Elternprozessor: Aus der Prozessorklasse kann immer ein DefaultProcessorDescriptor erzeugt werden.
     * Dieser beschreibt, Ã¤hnlich wie die Klasse selbst, die Menge aller mÃ¶glichen Prozessor-Instanzen.
     * Sollen fachspezifische Eingrenzungen getroffen werden, kan ein spezialisierter Prozessordescriptor konfiguriert werden.
     * So kann beispielsweise die Menge der mÃ¶glichen Parameterwerte eingegrenzt oder eine spezialisierte Beschreibung gegeben werden.
     *
     * @return
     */
    T getPrototypeDescriptor();
    void setPrototypeDescriptor(T descriptor);

    Class<?> getSourceClass();
    void setSourceClass(Class<?> clazz);

    String getSourceIdentifier();
    void setSourceIdentifier(String identifier);

    String getSourcePrototypeIdentifier();
    void setSourcePrototypeIdentifier(String prototypeIdentifier);

    String getSourceScope();
    void setSourceScope(String scope);

    String getSourceFullBeanId();
    /**
     * Liefert die Deskriptoren fÃ¼r alle Parameter, die diese Struktur definiert.
     * Intern wird dies typischerweise durch einen IParameterGroupDescriptor reprÃ¤sentiert.
     * @return Eine Liste der Parameter-Deskriptoren dieser Struktur.
     */
    List<IParameterDescriptor> getParameterDescriptors();

    /**
     * Liefert die Kategorien, die dieser Struktur zugeordnet sind (aus der @Processor oder @ValueObject Annotation).
     * @return Ein Array von Kategorie-Strings.
     */
    String[] getCategories();

    /**
     * Liefert die Sub-Kategorien des beschriebenen Objekts.
     * @return Ein Array von Sub-Kategorie-Pfaden.
     */
    String[] getSubCategories();

    /**
     * Liefert die Liste der Tags des beschriebenen Objekts.
     * @return Ein Array von Tags.
     */
    String[] getTags();

    String[] getDefaultValues();

    //TODO: wird das noch benÃ¶tigt?
    public boolean isResponsibleFor(Object sourceProcessor);

    public void initBeanParameters(Object sourceProcessor, Map<String, Object> parameters);

    /**
     * Bindet die effektiven Parameterwerte des beschriebenen Struktur-Objekts an diesen Deskriptor.
     *
     * @param bean Das Struktur-Objekt, dessen Werte gebunden werden sollen.
     */
    public void bindEffectiveParameterValues(Object bean);

    /**
     * Extrahiert die effektiven Parameterwerte des beschriebenen Struktur-Objekts.
     *
     * @param bean Das Struktur-Objekt, dessen Werte extrahiert werden sollen.
     * @return Eine Map der extrahierten Parameter.
     */
    public Map<String, Object> extractEffectiveParameters(Object bean);

    public Map<String, Object> extractEffectiveParametersForResolvedValue();
}