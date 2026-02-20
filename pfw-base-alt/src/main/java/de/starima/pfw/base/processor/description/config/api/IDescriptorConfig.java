package de.starima.pfw.base.processor.description.config.api;

import java.util.Map;
import java.util.Optional;

public interface IDescriptorConfig {
    /**
     * Der Prototyp-Identifier (BeanId) der zu verwendenden Deskriptor-Klasse.
     * Dies ist der SchlÃ¼ssel, den die ProcessorRegistry zur Instanziierung verwendet.
     */
    String getDescriptorPrototypeIdentifier();

    /**
     * Der spezifische Identifier der Instanz.
     */
    String getDescriptorIdentifier();
    /**
     * Der Scope der Deskriptor-Instanz.
     */
    ProcessorScope getDescriptorScope();
    /**
     * Hier steht eine Kurzbeschreibung des Prozessors. Falls es mehr zu sagen gibt, so kann das in descriptionAssetName erzÃ¤hlt werden.
     * Diese Beschreibung prÃ¤sentiert ReconLight. Falls der Nutzer im ReconLigth mehr erfahren will, so kann er descriptionAssetName abfragen.
     */
    String getDescription();
    /**
     * Falls die Beschreibung lÃ¤nger ist, kann sie in eine Datei/Asset ausgelagert bzw. erweitert werden.
     * Diese kann mit dem konfigurierten AssetProvider extrahiert werden.
     * Wurde kein Name angegeben, so wird nach &lt;Klassenname.md&gt; gesucht.
     */
    String getDescriptionAssetName();
    /**
     * Der mimeType der Beschreibungsdatei/des Assets.
     */
    String getDescriptionAssetMimetype();
    /**
     * Der Processor Identifier des IAssetProviders.
     */
    String getAssetProviderIdentifier();
    /**
     * Ein einfaches Key-Value-Objekt mit Parametern, die direkt auf die
     * erzeugte Deskriptor-Instanz gesetzt werden.
     */
    Map<String, Object> getParameters();
    /**
     * EnthÃ¤lt die  Blueprint-Map, die zur Erzeugung des Deskriptors
     * verwendet werden kann.
     */
    Map<String, Map<String, Object>> getDescriptorBeanParameterMap();

    /**
     * Versucht, diese Konfiguration als IValueConfig zu interpretieren.
     * Dies ist der bevorzugte Weg, um Downcasts zu vermeiden.
     *
     * @return Ein Optional, das diese Instanz als IValueConfig enthÃ¤lt,
     *         oder ein leeres Optional, wenn diese Konfiguration kein IValueConfig ist.
     */
    default Optional<IValueConfig> asValueConfig() {
        return (this instanceof IValueConfig) ? Optional.of((IValueConfig) this) : Optional.empty();
    }

    default Optional<IValueFunctionConfig> asValueFunctionConfig() {
        return (this instanceof IValueFunctionConfig) ? Optional.of((IValueFunctionConfig) this) : Optional.empty();
    }
}