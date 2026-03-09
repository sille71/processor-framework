package de.starima.pfw.base.util;

/**
 * Definiert das Ziel einer Serialisierung aus einer IProcessorDescriptor-Hierarchie.
 */
public enum SerializationTarget {
    /**
     * Serialisiert die von den Deskriptoren beschriebene Prozessor-Hierarchie.
     * Das Ergebnis ist eine Konfiguration fÃ¼r die Anwendungslogik.
     */
    PROCESSOR_HIERARCHY,

    /**
     * Serialisiert die Deskriptor-Hierarchie selbst.
     * Das Ergebnis ist eine Konfiguration, die die Metadaten-Struktur beschreibt.
     */
    DESCRIPTOR_HIERARCHY
}