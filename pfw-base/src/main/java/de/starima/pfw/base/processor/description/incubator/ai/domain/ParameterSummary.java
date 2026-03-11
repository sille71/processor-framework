package de.starima.pfw.base.processor.description.incubator.ai.domain;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.annotation.ValueObject;
import lombok.Builder;
import lombok.Getter;

/**
 * Kurzform eines Parameter-Descriptors für den Prozessor-Katalog.
 *
 * <p>Enthält die wesentlichen Informationen, die ein KI-Assistent braucht,
 * um einen Parameter zu verstehen und zu befüllen.
 */
@Getter
@Builder
@ValueObject
public class ParameterSummary {

    /** Parametername (entspricht dem Feldnamen, z.B. "batchSize"). */
    @ProcessorParameter(description = "Parameter field name (e.g. 'batchSize')")
    String name;

    /** Java-Typ-Signatur (z.B. "int", "String", "List<IProcessor>"). */
    @ProcessorParameter(description = "Java type signature of the parameter")
    String typeSignature;

    /** Beschreibung aus {@code @ProcessorParameter(description = ...)}. */
    @ProcessorParameter(description = "Human-readable description of the parameter's purpose")
    String description;

    /** Ob der Parameter ein Pflichtfeld ist. */
    @ProcessorParameter(description = "Whether this parameter must be provided")
    boolean required;

    /** Default-Wert aus {@code @ProcessorParameter(value = ...)} oder {@code null}. */
    @ProcessorParameter(description = "Default value from @ProcessorParameter(value = ...), or null if none")
    String defaultValue;
}