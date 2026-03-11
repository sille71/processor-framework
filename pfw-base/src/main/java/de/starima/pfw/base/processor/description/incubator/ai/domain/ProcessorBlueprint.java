package de.starima.pfw.base.processor.description.incubator.ai.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Vollständiger Blueprint eines Prozessors in einer Form, die ein KI-Assistent
 * direkt als Kontext verwenden kann.
 *
 * <p>Wird als strukturierter Prompt-Kontext an die KI übergeben.
 * Die Methode {@link #toAIReadableText()} erzeugt einen für LLM-Prompts
 * optimierten Textblock.
 */
@Getter
@Builder
public class ProcessorBlueprint {

    /** Prozessor-Prototypen-ID. */
    String prototypeId;

    /** Vollständige Beschreibung. */
    String description;

    /** Alle Parameter mit vollständigen Constraint-Informationen. */
    List<ParameterBlueprint> parameters;

    /**
     * Erzeugt eine KI-lesbare Textdarstellung.
     *
     * <p>Format ist optimiert für LLM-Prompts. Enthält alle Parameter
     * mit Typen, Defaults, möglichen Werten und Kandidaten für polymorphe Felder.
     */
    public String toAIReadableText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Prozessor: ").append(prototypeId).append("\n");
        sb.append("Beschreibung: ").append(description).append("\n");
        sb.append("Parameter:\n");

        if (parameters != null) {
            for (ParameterBlueprint p : parameters) {
                sb.append("  - ").append(p.getName())
                        .append(" (").append(p.getTypeSignature()).append(")")
                        .append(p.isRequired() ? " [PFLICHT]" : "")
                        .append(": ").append(p.getDescription()).append("\n");

                if (p.getDefaultValue() != null) {
                    sb.append("    Default: ").append(p.getDefaultValue()).append("\n");
                }
                if (p.getPossibleValues() != null && !p.getPossibleValues().isEmpty()) {
                    sb.append("    Mögliche Werte: ").append(p.getPossibleValues()).append("\n");
                }
                if (p.isPolymorphic()) {
                    sb.append("    Implementierungen: ").append(p.getCandidates()).append("\n");
                }
            }
        }

        return sb.toString();
    }
}