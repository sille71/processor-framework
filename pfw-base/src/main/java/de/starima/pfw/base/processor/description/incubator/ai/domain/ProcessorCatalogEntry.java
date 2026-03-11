package de.starima.pfw.base.processor.description.incubator.ai.domain;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.annotation.ValueObject;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Ein Eintrag im Prozessor-Katalog — KI-optimierte Kurzdarstellung.
 *
 * <p>Enthält alle Informationen, die ein KI-Assistent braucht, um zu
 * entscheiden, ob dieser Prozessor für eine Aufgabe geeignet ist.
 * Wird von {@link de.starima.pfw.base.processor.description.incubator.ai.api.IProcessorCatalog}
 * als Suchergebnis geliefert.
 */
@Getter
@Builder
@ValueObject
public class ProcessorCatalogEntry {

    // =========================================================================
    // Kernfelder — aus @Processor-Annotation
    // =========================================================================

    /** Prozessor-Prototypen-ID (z.B. "csvReaderProcessor"). */
    @ProcessorParameter(description = "Prototype ID of the processor (e.g. 'csvReaderProcessor')")
    String prototypeId;

    /** Anzeigename für das UI (z.B. "CSV Reader"). */
    @ProcessorParameter(description = "Human-readable display name shown in the UI")
    String displayName;

    /** Kurzbeschreibung aus {@code @Processor(description = ...)}. */
    @ProcessorParameter(description = "Short description from the @Processor annotation")
    String shortDescription;

    /** Längere Beschreibung (z.B. aus Dokumentations-Markdown). */
    @ProcessorParameter(description = "Extended description, e.g. from documentation markdown")
    String longDescription;

    /** Kategorien aus {@code @Processor(categories = ...)}. */
    @ProcessorParameter(description = "Category tags from the @Processor annotation used for filtering")
    String[] categories;

    /** Tags aus {@code @Processor(tags = ...)}. */
    @ProcessorParameter(description = "Free-form tags from the @Processor annotation")
    String[] tags;

    /** Kurzform der Parameter-Signaturen. */
    @ProcessorParameter(description = "Summary of the processor's configurable parameters")
    List<ParameterSummary> parameters;

    // =========================================================================
    // KI-spezifische Felder
    // =========================================================================

    /**
     * Fähigkeiten in natürlicher Sprache.
     *
     * <p>Beispiel: ["Liest CSV-Dateien", "Unterstützt Encoding-Konfiguration"].
     */
    @ProcessorParameter(description = "Natural language descriptions of what this processor can do")
    String[] capabilities;

    /**
     * Typische Anwendungsfälle.
     *
     * <p>Beispiel: ["Datenimport", "ETL-Pipeline"].
     */
    @ProcessorParameter(description = "Typical use cases where this processor is applied")
    String[] typicalUseCases;

    /**
     * Kompatible Prozessoren (typische Kombinationspartner).
     *
     * <p>Beispiel: ["dbWriterProcessor", "excelExportProcessor"].
     */
    @ProcessorParameter(description = "Prototype IDs of processors commonly combined with this one")
    String[] compatibleWith;
}