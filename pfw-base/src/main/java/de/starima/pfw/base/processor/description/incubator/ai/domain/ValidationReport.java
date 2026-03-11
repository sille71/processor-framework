package de.starima.pfw.base.processor.description.incubator.ai.domain;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.annotation.ValueObject;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Ergebnis einer Validierung durch den
 * {@link de.starima.pfw.base.processor.description.incubator.ai.api.IAIConfigValidator}.
 *
 * <p>Enthält alle gefundenen Probleme geordnet nach Schweregrad.
 * Ein Report ohne ERROR-Einträge gilt als valide.
 */
@Getter
@Builder
@ValueObject
public class ValidationReport {

    /** {@code true} wenn keine ERROR-Einträge vorhanden. */
    @ProcessorParameter(description = "True if no ERROR-level issues were found in the validation")
    boolean valid;

    /** Alle gefundenen Probleme. */
    @ProcessorParameter(description = "All validation issues found, ordered by severity")
    List<ValidationIssue> issues;

    /**
     * Ein einzelnes Validierungsproblem.
     */
    @Getter
    @Builder
    @ValueObject
    public static class ValidationIssue {

        /** Schweregrad des Problems. */
        public enum Severity {
            /** Entwurf kann nicht konstruiert werden. */
            ERROR,
            /** Entwurf kann konstruiert werden, aber könnte Probleme haben. */
            WARNING,
            /** Hinweis auf mögliche Verbesserungen. */
            INFO
        }

        @ProcessorParameter(description = "Severity level of this validation issue (ERROR, WARNING, or INFO)")
        Severity severity;

        /** Pfad des betroffenen Parameters (z.B. "reconProcessor.batchSize"). */
        @ProcessorParameter(description = "Dot-separated path to the affected parameter (e.g. 'reconProcessor.batchSize')")
        String parameterPath;

        /** Fehlerbeschreibung (z.B. "Parameter 'batchSize' muss eine Zahl sein"). */
        @ProcessorParameter(description = "Human-readable description of the validation problem")
        String message;

        /** Korrekturvorschlag (z.B. "Vorschlag: 1000"). */
        @ProcessorParameter(description = "Suggested correction to resolve the issue")
        String suggestion;
    }
}