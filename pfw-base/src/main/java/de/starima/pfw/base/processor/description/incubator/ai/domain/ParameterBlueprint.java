package de.starima.pfw.base.processor.description.incubator.ai.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Vollständige Blueprint-Beschreibung eines Parameters für den KI-Kontext.
 *
 * <p>Teil eines {@link ProcessorBlueprint}. Enthält alle Informationen,
 * die eine KI braucht, um einen gültigen Parameterwert zu erzeugen.
 */
@Getter
@Builder
public class ParameterBlueprint {

    /** Parametername. */
    String name;

    /** Java-Typ-Signatur (z.B. "int", "String", "List<IProcessor>"). */
    String typeSignature;

    /** Beschreibung aus {@code @ProcessorParameter(description = ...)}. */
    String description;

    /** Ob der Parameter ein Pflichtfeld ist. */
    boolean required;

    /** Default-Wert oder {@code null}. */
    String defaultValue;

    /**
     * Mögliche Werte für Enums oder beschränkte Ranges.
     *
     * <p>Beispiel: ["DEEP", "SHALLOW", "LAZY"] für LoadStrategy.
     */
    List<String> possibleValues;

    /**
     * Ob der Parameter polymorph ist (d.h. eine Processor-Referenz,
     * für die verschiedene Implementierungen in Frage kommen).
     */
    boolean polymorphic;

    /**
     * Kandidaten für polymorphe Parameter.
     *
     * <p>Beispiel: ["csvReaderProcessor", "xmlReaderProcessor"] für einen sourceProcessor-Parameter.
     */
    List<String> candidates;

    /**
     * Geforderte Kategorien für polymorphe Parameter.
     *
     * <p>Beispiel: ["reader"] → nur Reader-Prozessoren als Kandidaten.
     */
    String[] requiredCategories;
}