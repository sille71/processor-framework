package de.starima.pfw.base.processor.description.incubator.ai.domain;

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
public class ValidationReport {

    /** {@code true} wenn keine ERROR-Einträge vorhanden. */
    boolean valid;

    /** Alle gefundenen Probleme. */
    List<ValidationIssue> issues;

    /**
     * Ein einzelnes Validierungsproblem.
     */
    @Getter
    @Builder
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

        Severity severity;

        /** Pfad des betroffenen Parameters (z.B. "reconProcessor.batchSize"). */
        String parameterPath;

        /** Fehlerbeschreibung (z.B. "Parameter 'batchSize' muss eine Zahl sein"). */
        String message;

        /** Korrekturvorschlag (z.B. "Vorschlag: 1000"). */
        String suggestion;
    }
}