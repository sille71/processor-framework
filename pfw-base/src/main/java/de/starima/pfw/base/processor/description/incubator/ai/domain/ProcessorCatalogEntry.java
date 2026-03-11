package de.starima.pfw.base.processor.description.incubator.ai.domain;

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
public class ProcessorCatalogEntry {

    // =========================================================================
    // Kernfelder — aus @Processor-Annotation
    // =========================================================================

    /** Prozessor-Prototypen-ID (z.B. "csvReaderProcessor"). */
    String prototypeId;

    /** Anzeigename für das UI (z.B. "CSV Reader"). */
    String displayName;

    /** Kurzbeschreibung aus {@code @Processor(description = ...)}. */
    String shortDescription;

    /** Längere Beschreibung (z.B. aus Dokumentations-Markdown). */
    String longDescription;

    /** Kategorien aus {@code @Processor(categories = ...)}. */
    String[] categories;

    /** Tags aus {@code @Processor(tags = ...)}. */
    String[] tags;

    /** Kurzform der Parameter-Signaturen. */
    List<ParameterSummary> parameters;

    // =========================================================================
    // KI-spezifische Felder
    // =========================================================================

    /**
     * Fähigkeiten in natürlicher Sprache.
     *
     * <p>Beispiel: ["Liest CSV-Dateien", "Unterstützt Encoding-Konfiguration"].
     */
    String[] capabilities;

    /**
     * Typische Anwendungsfälle.
     *
     * <p>Beispiel: ["Datenimport", "ETL-Pipeline"].
     */
    String[] typicalUseCases;

    /**
     * Kompatible Prozessoren (typische Kombinationspartner).
     *
     * <p>Beispiel: ["dbWriterProcessor", "excelExportProcessor"].
     */
    String[] compatibleWith;
}