package de.starima.pfw.base.processor.description.incubator.domain;

/**
 * Cursor-Stage-Modell für das inkrementelle Laden des Descriptor-Graphs.
 *
 * <p>Jede Stage repräsentiert einen klar definierten Schritt beim Aufbau
 * eines Descriptor-Knotens. Das UI lädt nur die Stages, die es gerade braucht.
 *
 * <p><b>Biologische Analogie:</b> Wie in der Embryonalentwicklung differenzieren
 * sich Zellen Schritt für Schritt. ROOT_HEADER ist die Zygote, SCALAR_RAWVALUE
 * der vollständig ausdifferenzierte Zelltyp.
 */
public enum BuildStage {

    // =========================================================================
    // Root-Level: ProcessorDescriptor
    // =========================================================================

    /** ProcessorDescriptor ohne Slots (nur Basis-Metadaten: name, description, scope). */
    ROOT_HEADER,

    /** Slot-Keys auflisten (z.B. ["title", "path", "formatProcessor"]). */
    ROOT_SLOTS_ENUM,

    // =========================================================================
    // Slot-Level: ParameterDescriptor
    // =========================================================================

    /** ParameterDescriptor ohne ValueDescriptor (name, required, aliases, description). */
    SLOT_DESCRIPTOR,

    // =========================================================================
    // Value-Level: ValueDescriptor
    // =========================================================================

    /** ValueDescriptor-Typ bestimmen (Scalar/Collection/Map/Polymorphic/Processor). */
    VALUE_HEADER,

    // =========================================================================
    // Scalar
    // =========================================================================

    /** ValueFunction-Metadaten laden (Format, Validation, possibleValues). */
    SCALAR_FUNCTION,

    /** Konkreten Wert laden (rawValue aus der beanParameterMap). */
    SCALAR_RAWVALUE,

    // =========================================================================
    // Processor/Structure
    // =========================================================================

    /** Subprozessor-Slots laden (Rekursion: ROOT_HEADER des Subprozessors). */
    PROCESSOR_SLOTS,

    // =========================================================================
    // Collection
    // =========================================================================

    /** Collection-Metadaten laden (elementType, Gesamtgröße). */
    COLLECTION_META,

    /** Element-Descriptor-Schema laden (wie sieht ein Element aus?). */
    COLLECTION_ELEMENT_SCHEMA,

    /** Items seitenweise laden (pageSize via ExpandPolicy). */
    COLLECTION_ITEMS_PAGE,

    // =========================================================================
    // Map
    // =========================================================================

    /** Map-Metadaten laden (keyType, valueType, Gesamtgröße). */
    MAP_META,

    /** Key-Descriptor-Schema laden. */
    MAP_KEY_SCHEMA,

    /** Value-Descriptor-Schema laden. */
    MAP_VALUE_SCHEMA,

    /** Entries seitenweise laden (pageSize via ExpandPolicy). */
    MAP_ENTRIES_PAGE,

    // =========================================================================
    // Polymorphie
    // =========================================================================

    /** Polymorphie-Info laden (welche Typen sind möglich?). */
    POLY_META,

    /** getPossibleValueDescriptors() laden (Kandidaten-Index). */
    POLY_CANDIDATES_INDEX,

    /** Auswahl getroffen (selectedPrototypeId via SelectCandidatePatch). */
    POLY_SELECTION_SET,

    /** Unterteilbaum für den gewählten Typ laden. */
    POLY_SELECTED_SUBTREE
}