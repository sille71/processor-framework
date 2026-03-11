package de.starima.pfw.base.processor.description.incubator.ai.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.incubator.ai.domain.Suggestion;

import java.util.List;
import java.util.Map;

/**
 * Erklärt bestehende Konfigurationen in natürlicher Sprache.
 *
 * <p>Kann einen laufenden Prozessor beschreiben, Unterschiede zwischen
 * zwei Konfigurationen erklären oder Verbesserungsvorschläge machen.
 *
 * <p>Kategorien: {@code ai, explanation}<br>
 * Tags: {@code explain, describe, diff, suggest}
 */
public interface IAIExplainer extends IProcessor {

    /**
     * Erklärt einen laufenden Prozessor in natürlicher Sprache.
     *
     * <p>Output-Beispiel: "Dieser Prozess liest täglich CSV-Dateien aus /data/import,
     * gleicht sie mit der Kundendatenbank ab und..."
     */
    String explainConfiguration(IProcessor processor);

    /**
     * Erklärt eine beanParameterMap in natürlicher Sprache.
     *
     * @param beanParameterMap Die zu erklärende Konfiguration
     * @param catalog          Der Katalog für Prozessor-Metadaten
     */
    String explainConfiguration(Map<String, Map<String, Object>> beanParameterMap,
                                IProcessorCatalog catalog);

    /**
     * Erklärt die Unterschiede zwischen zwei Konfigurationen.
     *
     * <p>Output-Beispiel: "Im neuen Prozess wurde der Batch-Size von 500 auf 1000
     * erhöht und der E-Mail-Benachrichtigungsprozessor hinzugefügt."
     */
    String explainDiff(Map<String, Map<String, Object>> before,
                       Map<String, Map<String, Object>> after,
                       IProcessorCatalog catalog);

    /**
     * Schlägt Verbesserungen für eine bestehende Konfiguration vor.
     *
     * @return Liste von Verbesserungsvorschlägen, sortiert nach geschätztem Einfluss
     */
    List<Suggestion> suggestImprovements(IProcessor processor, IProcessorCatalog catalog);
}