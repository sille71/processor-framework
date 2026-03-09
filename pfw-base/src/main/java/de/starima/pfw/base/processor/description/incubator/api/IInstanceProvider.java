package de.starima.pfw.base.processor.description.incubator.api;

import de.starima.pfw.base.processor.api.IProcessor;

/**
 * Bidirektionaler Provider für Objekt-Erzeugung und -Extraktion.
 *
 * <p>Vereinheitlicht die bisherigen drei Erzeugungswege (ProcessorProvider,
 * ValueFunction, BeanProvider) zu einem einzigen Einstiegspunkt.
 *
 * <p><b>Bidirektionalität:</b>
 * <ul>
 *   <li>{@link #provide} — Hydration: Config/beanParameterMap → lebendiges Objekt</li>
 *   <li>{@link #extract} — Dehydration: lebendiges Objekt → Config/beanParameterMap</li>
 * </ul>
 *
 * <p>Die Roundtrip-Invarianz {@code extract(provide(config)) == config} ist der
 * algebraische Beweis, dass das Erzeugungssystem korrekt implementiert ist.
 *
 * <p>Folgt dem Chain-of-Responsibility-Pattern — konsistent mit
 * TypeRefProviderChain, DescriptorConfigProviderChain und ContextProviderResolverChain.
 */
public interface IInstanceProvider extends IProcessor {

    /**
     * Prüft, ob dieser Provider für den gegebenen Kontext zuständig ist.
     *
     * <p>Die Entscheidung basiert auf:
     * <ul>
     *   <li>{@code typeToResolve} — die Zielklasse (IProcessor, @ValueObject, String, List, ...)</li>
     *   <li>{@code objectToResolve} — eine bereits vorhandene Instanz (bei extract)</li>
     *   <li>{@code fieldToResolve} — das Zielfeld (für Generics-Auflösung)</li>
     * </ul>
     *
     * @param context Der Kontext mit allen Informationen über das zu erzeugende/extrahierende Objekt.
     * @return {@code true}, wenn dieser Provider die Erzeugung/Extraktion übernehmen kann.
     */
    boolean isResponsibleFor(IInstanceCreationContext context);

    /**
     * Hydration: Erzeugt ein Objekt aus dem Kontext.
     *
     * <p>Der Kontext enthält:
     * <ul>
     *   <li>{@code typeToResolve} — der gewünschte Zieltyp</li>
     *   <li>{@code parameterValue} — der rohe Konfigurationswert (String, Map, etc.)</li>
     *   <li>{@code getRuntimeContext()} — der aktuelle IProcessorContext (beanParameterMap)</li>
     *   <li>{@code getRootProvider()} — die Chain für rekursive Aufrufe</li>
     *   <li>{@code getCreationStack()} — Zirkularitätserkennung</li>
     * </ul>
     *
     * <p>Für Struktur-Typen (Prozessoren, ValueObjects) wird rekursiv
     * {@code getRootProvider().provide(childContext)} aufgerufen.
     *
     * @param context Der Erzeugungskontext.
     * @return Das erzeugte/transformierte Objekt, oder {@code null} falls die Erzeugung fehlschlägt.
     */
    Object provide(IInstanceCreationContext context);

    /**
     * Dehydration: Extrahiert die Konfiguration aus einem lebendigen Objekt.
     *
     * <p>Der Kontext enthält:
     * <ul>
     *   <li>{@code objectToResolve} — das lebendige Objekt</li>
     *   <li>{@code fieldToResolve} — das Feld, in dem das Objekt lebt (für Typ-Info)</li>
     *   <li>{@code getVisitedSet()} — Zykluserkennung</li>
     *   <li>{@code getExtractionResult()} — die aufzubauende beanParameterMap</li>
     * </ul>
     *
     * <p><b>Return-Wert:</b> Der "rohe" Referenzwert, den der Eltern-Aufruf
     * in seine Parameter-Map schreibt:
     * <ul>
     *   <li>Für Prozessoren: die fullBeanId (z.B. {@code "myProcessor:instance1@singleton"})</li>
     *   <li>Für ValueObjects: der Identifier</li>
     *   <li>Für Skalare: der primitive Wert als String</li>
     *   <li>Für Collections: eine Liste von Raw-Werten</li>
     * </ul>
     *
     * <p><b>Nebeneffekt:</b> Bei Struktur-Typen wird die {@code extractionResult}-Map
     * im Kontext um die Parameter des Objekts ergänzt.
     *
     * @param context Der Extraktionskontext.
     * @return Der rohe Referenzwert für die Eltern-Parameter-Map.
     */
    Object extract(IInstanceCreationContext context);
}
