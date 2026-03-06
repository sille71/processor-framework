package de.starima.pfw.base.processor.description.incubator.api;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.ITaskContext;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfigProvider;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

/**
 * Kontext für die bidirektionale Objekt-Erzeugung und -Extraktion.
 *
 * <p>Trägt alle Informationen, die ein {@link IInstanceProvider} benötigt:
 *
 * <p><b>Für provide() (Hydration):</b>
 * <ul>
 *   <li>{@code typeToResolve} — Zieltyp (Class, ParameterizedType)</li>
 *   <li>{@code parameterValue} — roher Konfigurationswert aus der beanParameterMap</li>
 *   <li>{@code fieldToResolve} — das Zielfeld (für Generics, Annotation-Info)</li>
 *   <li>{@code processorParameter} — die @ProcessorParameter-Annotation</li>
 *   <li>{@code creationStack} — Zirkularitätserkennung (Set von fullBeanIds)</li>
 * </ul>
 *
 * <p><b>Für extract() (Dehydration):</b>
 * <ul>
 *   <li>{@code objectToResolve} — das lebendige Objekt</li>
 *   <li>{@code fieldToResolve} — das Feld, in dem es lebt</li>
 *   <li>{@code visitedSet} — Zykluserkennung (Identity-basiert)</li>
 *   <li>{@code extractionResult} — die aufzubauende beanParameterMap</li>
 * </ul>
 *
 * <p><b>Für beide Richtungen:</b>
 * <ul>
 *   <li>{@code getRuntimeContext()} — der aktuelle IProcessorContext (geerbt von ITaskContext)</li>
 *   <li>{@code rootProvider} — die InstanceProviderChain für rekursive Aufrufe</li>
 *   <li>{@code descriptorConfigRootProvider} — für Descriptor-Auflösung</li>
 * </ul>
 */
public interface IInstanceCreationContext extends ITaskContext {

    // =========================================================================
    // Bestehende Felder (NICHT ändern)
    // =========================================================================

    /**
     * Der Java-Typ, der aufgelöst werden soll.
     * Kann ein {@code Class<?>} oder ein {@code ParameterizedType} sein (z.B. {@code List<IProcessor>}).
     */
    Type getTypeToResolve();

    /**
     * Das Java-Objekt, welches untersucht werden soll.
     * Bei provide(): die bereits vorhandene Bean-Instanz (oder null).
     * Bei extract(): das lebendige Objekt, aus dem extrahiert wird.
     */
    Object getObjectToResolve();

    /**
     * Das Feld, in das der Wert injiziert wird (bei provide) bzw. aus dem gelesen wird (bei extract).
     * Trägt Generics-Information und die @ProcessorParameter-Annotation.
     */
    Field getFieldToResolve();

    /**
     * Die @ProcessorParameter-Annotation des Feldes.
     * Enthält Konfigurationshinweise wie valueFunctionIdentifier, required, contextProvider, etc.
     */
    ProcessorParameter getProcessorParameter();

    /**
     * Der Root-Provider (die Chain), über den rekursive Aufrufe laufen.
     */
    IInstanceProvider getRootProvider();
    void setRootProvider(IInstanceProvider rootProvider);

    /**
     * Der Descriptor-Config Root-Provider (die Kette), der für rekursive Aufrufe verwendet wird.
     */
    IDescriptorConfigProvider getDescriptorConfigRootProvider();
    void setDescriptorConfigRootProvider(IDescriptorConfigProvider rootProvider);

    // =========================================================================
    // NEU: Für provide() — Parameterwert und Zirkularitätserkennung
    // =========================================================================

    /**
     * Der rohe Parameterwert aus der beanParameterMap.
     *
     * <p>Bei Prozessoren: ein String (fullBeanId) oder eine bereits erzeugte IProcessor-Instanz.
     * Bei Skalaren: der String-Wert ("42", "true", "myEnum").
     * Bei Collections: eine Liste von Rohwerten.
     * Bei ValueObjects: ein String (fullBeanId) oder eine Map (inline-Konfiguration).
     */
    Object getParameterValue();
    void setParameterValue(Object value);

    /**
     * Stack der aktuell in Erzeugung befindlichen fullBeanIds.
     *
     * <p>Wird bei jedem rekursiven {@code provide()}-Aufruf für Struktur-Typen erweitert.
     * Erkennt Zyklen, bevor eine Endlosrekursion entsteht.
     *
     * <p>Beispiel eines Stacks während der Erzeugung:
     * <pre>
     *   ["kernelProcessor@singleton", "clusterCtxProvider@singleton", "fileSystemParamProvider@singleton"]
     * </pre>
     */
    Set<String> getCreationStack();

    /**
     * Prüft, ob ein Prozessor mit der gegebenen fullBeanId gerade erzeugt wird.
     */
    default boolean isInCreationPath(String fullBeanId) {
        return getCreationStack() != null && getCreationStack().contains(fullBeanId);
    }

    /**
     * Die maximale Rekursionstiefe für einen bestimmten Typ.
     *
     * <p>Default: 1 für normale Prozessoren (keine Rekursion erlaubt).
     * Sonderregel: 2 für ProcessorDescriptoren (Descriptoren beschreiben sich selbst).
     *
     * @param type Der Typ, für den die Tiefe geprüft wird.
     * @return Die maximale erlaubte Rekursionstiefe.
     */
    default int getMaxRecursionDepth(Class<?> type) {
        return 1; // Default: keine Rekursion
    }

    // =========================================================================
    // NEU: Für extract() — Visited-Set und Ergebnis-Map
    // =========================================================================

    /**
     * Identity-basiertes Set der bereits besuchten Objekte.
     *
     * <p>Verhindert Endlosrekursion bei zirkulären Objektreferenzen.
     * Basiert auf Objekt-Identität (==), nicht auf equals().
     */
    Set<Object> getVisitedSet();

    /**
     * Prüft, ob ein Objekt bereits extrahiert wurde.
     */
    default boolean isAlreadyVisited(Object instance) {
        return getVisitedSet() != null && getVisitedSet().contains(instance);
    }

    /**
     * Markiert ein Objekt als besucht.
     */
    default void markVisited(Object instance) {
        if (getVisitedSet() != null) getVisitedSet().add(instance);
    }

    /**
     * Die beanParameterMap, die während der Extraktion aufgebaut wird.
     *
     * <p>Jeder InstanceProvider für Struktur-Typen (Prozessor, ValueObject) fügt
     * seinen Beitrag hinzu:
     * <pre>
     *   extractionResult.put(identifier, parameters);
     * </pre>
     *
     * <p>Skalare Provider schreiben NICHT in diese Map — sie geben nur den
     * rohen Wert als Return-Wert zurück.
     */
    Map<String, Map<String, Object>> getExtractionResult();
    void setExtractionResult(Map<String, Map<String, Object>> result);
}
