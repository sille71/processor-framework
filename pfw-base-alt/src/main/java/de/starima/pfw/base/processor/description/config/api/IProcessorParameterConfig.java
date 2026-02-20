package de.starima.pfw.base.processor.description.config.api;

import de.starima.pfw.base.annotation.Processor;

import java.util.List;

/**
 * Konfigurations-Interface fÃ¼r einen einzelnen Prozessor-Parameter.
 * Es beschreibt die Metadaten und das Verhalten eines Parameters innerhalb
 * eines Prozessors oder ValueObjects.
 */
@Processor(description = "Konfigurations-Interface fÃ¼r einen einzelnen Prozessor-Parameter.\n" +
        " Es beschreibt die Metadaten und das Verhalten eines Parameters innerhalb\n" +
        " eines Prozessors oder ValueObjects.")
public interface IProcessorParameterConfig extends IDescriptorConfig {
    /**
     * Der Name der Property des Prozessors (Bean).
     */
    String getPropertyName();
    void setPropertyName(String propertyName);

    /**
     * Der Name des Parameters, wie er in der beanParameterMap zu finden ist.
     * Entspricht per default dem propertyNamen.
     */
    String getParameterName();
    void setParameterName(String parameterName);

    /**
     * Der Standardwert fÃ¼r diesen Parameter.
     */
    Object getParameterDefaultValue();
    void setParameterDefaultValue(Object parameterDefaultValue);

    /**
     * Wenn auf 'true' gesetzt, wird dieses Feld als Teil des
     * zusammengesetzten SchlÃ¼ssels zur Identifizierung einer @ValueObject-Instanz verwendet.
     * Dies wird nur berÃ¼cksichtigt, wenn das ValueObject nicht das IArtifact-Interface implementiert.
     */
    boolean isKey();
    void setKey(boolean key);

    /**
     * Definiert eine Liste von alternativen oder veralteten Namen fÃ¼r diesen Parameter.
     * <p>
     * Dies ist der SchlÃ¼ssel zur AbwÃ¤rtskompatibilitÃ¤t. Wenn eine Konfiguration
     * einen der hier definierten Alias-Namen verwendet, wird er vom Framework
     * korrekt diesem Feld zugeordnet. Bei der Extraktion oder Serialisierung
     * wird jedoch immer der aktuelle Feldname verwendet, wodurch Konfigurationen
     * quasi "automatisch" migriert werden.
     *
     * @return Eine Liste von alternativen Parameternamen.
     */
    List<String> getAliases();
    void setAliases(List<String> aliases);

    /**
     * Wenn true, wird dieser Parameter bei der Initialisierung des Prozessors ignoriert.
     */
    boolean isIgnoreInitialization();
    void setIgnoreInitialization(boolean ignoreInitialization);

    /**
     * Wenn true, wird dieser Parameter bei einem Refresh des Prozessors ignoriert.
     */
    boolean isIgnoreRefresh();
    void setIgnoreRefresh(boolean ignoreRefresh);

    /**
     * Wenn true, wird dieser Parameter bei der Extraktion der Parameter-Map ignoriert.
     */
    boolean isIgnoreExtractParameter();
    void setIgnoreExtractParameter(boolean ignoreExtractParameter);

    /**
     * Gibt an, ob dieser Parameter ein Pflichtfeld ist.
     */
    boolean isRequired();
    void setRequired(boolean required);

    /**
     * Gibt an, ob dieser Parameter als Input-Parameter betrachtet wird.
     */
    boolean isInput();
    void setInput(boolean isInput);

    /**
     * Gibt an, ob dieser Parameter als Output-Parameter betrachtet wird.
     */
    boolean isOutput();
    void setOutput(boolean isOutput);

    /**
     * Die Konfiguration des WERTES, den dieser Parameter aufnimmt.
     * Dies ist die Verbindung zum "Stecker".
     */
    IValueConfig getValueConfig();
    void setValueConfig(IValueConfig valueConfig);
}