package de.starima.pfw.base.processor.description.config.api;

import java.util.List;
import java.util.Map;

/**
 * Konfigurations-Interface fÃ¼r eine strukturierte Komponente (Prozessor oder ValueObject).
 * Es bÃ¼ndelt alle Metadaten, die zur Registrierung, Instanziierung und
 * Descriptor Erstellung benÃ¶tigt werden.
 */
public interface IValueObjectConfig extends IValueConfig {
    /**
     * Definiert den Meta-Typ der registrierten Klasse.
     * Wird vom Framework intern wÃ¤hrend der Analysephase gesetzt.
     */
    default MetaType getMetaType() {return MetaType.VALUE_OBJECT;}

    /**
     * Der Identifier, um den Prozessor oder das ValueObject in der ProcessorRegistry zu registrieren.
     * FÃ¼r gewÃ¶hnlich wird der prototypeIdentifier aus dem Klassennamen extrahiert.
     * Eine explizite Angabe kann Probleme bei Hot-Module-Replacement im Entwicklungsmodus vermeiden.
     */
    String getPrototypeIdentifier();
    void setPrototypeIdentifier(String prototypeIdentifier);

    /**
     * Eine Map der Form {key: value}, wobei der Key der Parametername ist, dessen Standardwert Ã¼berschrieben werden soll.
     * Dies ist nÃ¼tzlich in Subklassen, die den Default-Wert der Super-Klasse anpassen mÃ¶chten.
     */
    Map<String, Object> getDefaultValues();
    void setDefaultValues(Map<String, Object> defaultValues);

    /**
     * Definiert eine oder mehrere hierarchische Kategorien, denen die Komponente zugeordnet ist.
     * Dient der primÃ¤ren technischen Klassifizierung (z.B. "Transformator/DeploymentArtifact").
     */
    List<String> getCategories();
    void setCategories(List<String> categories);

    /**
     * Definiert eine oder mehrere hierarchische Sub-Kategorien zur Querschnittsklassifizierung.
     * Diese sind unabhÃ¤ngig von den Hauptkategorien und beschreiben Aspekte wie "performanceOptimized",
     * "experimental", "deprecated" oder "bankspezifisch".
     */
    List<String> getSubCategories();
    void setSubCategories(List<String> subCategories);

    /**
     * Definiert eine Liste von flexiblen, nicht-hierarchischen Tags fÃ¼r ad-hoc Filterung.
     * z.B. {"database-write", "ui-component", "security-relevant"}
     */
    List<String> getTags();
    void setTags(List<String> tags);

    /**
     * EnthÃ¤lt die Konfigurationen fÃ¼r alle Parameter dieser Komponente,
     * indiziert nach dem Parameternamen.
     */
    Map<String, IProcessorParameterConfig> getParameterConfigs();
    void setParameterConfigs(Map<String, IProcessorParameterConfig> parameterConfigs);

    String getDefaultBeanParameterMapFileName();
    /**
     * Enum zur Unterscheidung des Meta-Typs einer Komponente.
     */
    enum MetaType {
        PROCESSOR,
        VALUE_OBJECT
    }
}