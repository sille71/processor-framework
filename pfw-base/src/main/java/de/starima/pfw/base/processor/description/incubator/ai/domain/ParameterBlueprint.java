package de.starima.pfw.base.processor.description.incubator.ai.domain;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.annotation.ValueObject;
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
@ValueObject
public class ParameterBlueprint {

    /** Parametername. */
    @ProcessorParameter(description = "Name of the parameter field")
    String name;

    /** Java-Typ-Signatur (z.B. "int", "String", "List<IProcessor>"). */
    @ProcessorParameter(description = "Java type signature of the parameter (e.g. 'int', 'String', 'List<IProcessor>')")
    String typeSignature;

    /** Beschreibung aus {@code @ProcessorParameter(description = ...)}. */
    @ProcessorParameter(description = "Human-readable description of the parameter's purpose")
    String description;

    /** Ob der Parameter ein Pflichtfeld ist. */
    @ProcessorParameter(description = "Whether this parameter must be provided (true = mandatory)")
    boolean required;

    /** Default-Wert oder {@code null}. */
    @ProcessorParameter(description = "Default value used when no explicit value is configured")
    String defaultValue;

    /**
     * Mögliche Werte für Enums oder beschränkte Ranges.
     *
     * <p>Beispiel: ["DEEP", "SHALLOW", "LAZY"] für LoadStrategy.
     */
    @ProcessorParameter(description = "Enumerated set of allowed values for enum or constrained parameters")
    List<String> possibleValues;

    /**
     * Ob der Parameter polymorph ist (d.h. eine Processor-Referenz,
     * für die verschiedene Implementierungen in Frage kommen).
     */
    @ProcessorParameter(description = "True if the parameter accepts different processor implementations")
    boolean polymorphic;

    /**
     * Kandidaten für polymorphe Parameter.
     *
     * <p>Beispiel: ["csvReaderProcessor", "xmlReaderProcessor"] für einen sourceProcessor-Parameter.
     */
    @ProcessorParameter(description = "List of concrete prototype IDs that can be used for this polymorphic parameter")
    List<String> candidates;

    /**
     * Geforderte Kategorien für polymorphe Parameter.
     *
     * <p>Beispiel: ["reader"] → nur Reader-Prozessoren als Kandidaten.
     */
    @ProcessorParameter(description = "Required processor categories that candidate implementations must belong to")
    String[] requiredCategories;
}