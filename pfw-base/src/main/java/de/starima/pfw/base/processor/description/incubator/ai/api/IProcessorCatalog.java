package de.starima.pfw.base.processor.description.incubator.ai.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.incubator.ai.domain.ProcessorBlueprint;
import de.starima.pfw.base.processor.description.incubator.ai.domain.ProcessorCatalogEntry;

import java.util.List;

/**
 * Durchsuchbarer Katalog aller verfügbaren Prozessor-Blueprints.
 *
 * <p>Indexiert Beschreibungen, Kategorien, Tags und Parameter-Signaturen.
 * Dient als Wissensbasis für den AI-Assistenten. Der Katalog kann über
 * natürlichsprachliche Anfragen, Kategorien und Tags durchsucht werden.
 *
 * <p>Kategorien: {@code ai, catalog}<br>
 * Tags: {@code search, index, blueprint, knowledge}
 */
public interface IProcessorCatalog extends IProcessor {

    /**
     * Durchsucht den Katalog nach Prozessoren, die zu einer
     * natürlichsprachlichen Beschreibung passen.
     *
     * <p>Beispiel: "CSV Dateien lesen" → [CsvReaderProcessor, FileReaderProcessor]
     */
    List<ProcessorCatalogEntry> search(String naturalLanguageQuery);

    /**
     * Findet Prozessoren nach Kategorie.
     *
     * <p>Beispiel: "reader" → alle Reader-Prozessoren.
     */
    List<ProcessorCatalogEntry> findByCategory(String category);

    /**
     * Findet Prozessoren nach Tags.
     *
     * <p>Beispiel: ["csv", "import"] → CsvReaderProcessor.
     * Alle angegebenen Tags müssen vorhanden sein (AND-Verknüpfung).
     */
    List<ProcessorCatalogEntry> findByTags(String... tags);

    /**
     * Findet Prozessoren, die als Parameter eines anderen Prozessors
     * in Frage kommen (basierend auf requiredCategories/Tags des Parameters).
     */
    List<ProcessorCatalogEntry> findCandidatesForParameter(
            String processorId, String parameterName);

    /**
     * Liefert den vollständigen Blueprint eines Prozessors
     * in einer KI-lesbaren Form.
     *
     * @param prototypeId die Prozessor-Prototypen-ID
     * @return den Blueprint oder {@code null}, wenn nicht gefunden
     */
    ProcessorBlueprint getBlueprint(String prototypeId);

    /**
     * Liefert eine Zusammenfassung aller Fähigkeiten des Frameworks
     * in natürlicher Sprache.
     *
     * <p>Wird als Teil des System-Prompts an die KI übergeben.
     */
    String getCapabilitySummary();
}