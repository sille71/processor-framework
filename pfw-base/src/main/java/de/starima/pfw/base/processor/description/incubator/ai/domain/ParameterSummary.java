package de.starima.pfw.base.processor.description.incubator.ai.domain;

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
public class ParameterSummary {

    /** Parametername (entspricht dem Feldnamen, z.B. "batchSize"). */
    String name;

    /** Java-Typ-Signatur (z.B. "int", "String", "List<IProcessor>"). */
    String typeSignature;

    /** Beschreibung aus {@code @ProcessorParameter(description = ...)}. */
    String description;

    /** Ob der Parameter ein Pflichtfeld ist. */
    boolean required;

    /** Default-Wert aus {@code @ProcessorParameter(value = ...)} oder {@code null}. */
    String defaultValue;
}