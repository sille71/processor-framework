package de.starima.pfw.base.processor.description.incubator.api;

import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfigProvider;

import java.util.Map;
import java.util.Set;

/**
 * Kontext für die bidirektionale Objekt-Erzeugung und -Extraktion.
 *
 * <p>Erweitert {@link ITransformationContext} — erbt damit:
 * <ul>
 *   <li>{@code getTypeToResolve()} / {@code setTypeToResolve()}</li>
 *   <li>{@code getObjectToResolve()} / {@code setObjectToResolve()}</li>
 *   <li>{@code getFieldToResolve()} / {@code setFieldToResolve()}</li>
 *   <li>{@code getProcessorParameter()} / {@code setProcessorParameter()}</li>
 *   <li>{@code getValueDescriptor()} / {@code setValueDescriptor()}</li>
 *   <li>{@code getParentDescriptor()}, {@code getValueFunction()}</li>
 *   <li>{@code getLoadStrategy()} / {@code setLoadStrategy()}</li>
 *   <li>{@code getRawType()} (convenience default)</li>
 *   <li>{@code getRuntimeContext()} (über ITaskContext)</li>
 * </ul>
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
 */
public interface IInstanceCreationContext extends ITransformationContext {

    // Von ITransformationContext GEERBT (nicht nochmal deklarieren!):
    // Type getTypeToResolve() / setTypeToResolve()        ← geerbt
    // Object getObjectToResolve() / setObjectToResolve()  ← geerbt
    // Field getFieldToResolve() / setFieldToResolve()     ← geerbt
    // ProcessorParameter getProcessorParameter() / setProcessorParameter() ← geerbt
    // IValueDescriptor getValueDescriptor() / setValueDescriptor() ← geerbt
    // IDescriptorProcessor getParentDescriptor()          ← geerbt
    // IValueFunction getValueFunction()                   ← geerbt
    // LoadStrategy getLoadStrategy() / setLoadStrategy()  ← geerbt
    // getRawType()                                        ← geerbt (default)
    // getRuntimeContext()                                 ← geerbt über ITaskContext

    // =========================================================================
    // IInstanceProvider-spezifische Felder
    // =========================================================================

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
     */
    Set<String> getCreationStack();

    /**
     * Identity-basiertes Set der bereits besuchten Objekte.
     *
     * <p>Verhindert Endlosrekursion bei zirkulären Objektreferenzen.
     */
    Set<Object> getVisitedSet();

    /**
     * Die beanParameterMap, die während der Extraktion aufgebaut wird.
     */
    Map<String, Map<String, Object>> getExtractionResult();
    void setExtractionResult(Map<String, Map<String, Object>> result);

    // =========================================================================
    // Zirkularitätserkennung
    // =========================================================================

    /**
     * Prüft, ob ein Prozessor mit der gegebenen fullBeanId gerade erzeugt wird.
     */
    default boolean isInCreationPath(String fullBeanId) {
        return getCreationStack() != null && getCreationStack().contains(fullBeanId);
    }

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
     * Die maximale Rekursionstiefe für einen bestimmten Typ.
     *
     * <p>Default: 1 für normale Prozessoren (keine Rekursion erlaubt).
     * Sonderregel: 2 für ProcessorDescriptoren (Descriptoren beschreiben sich selbst).
     *
     * @param type Der Typ, für den die Tiefe geprüft wird.
     * @return Die maximale erlaubte Rekursionstiefe.
     */
    default int getMaxRecursionDepth(Class<?> type) {
        return 1;
    }
}