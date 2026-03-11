package de.starima.pfw.base.processor.description.incubator.ai.domain;

import lombok.Builder;
import lombok.Getter;

/**
 * Ein Verbesserungsvorschlag für eine bestehende Konfiguration.
 *
 * <p>Wird von {@link de.starima.pfw.base.processor.description.incubator.ai.api.IAIExplainer#suggestImprovements}
 * erzeugt. Enthält den aktuellen Wert, den Vorschlag und eine Begründung.
 */
@Getter
@Builder
public class Suggestion {

    /** Pfad des Parameters (z.B. "reconProcessor.batchSize"). */
    String parameterPath;

    /** Aktueller Wert (z.B. "500"). */
    String currentValue;

    /** Vorgeschlagener Wert (z.B. "2000"). */
    String suggestedValue;

    /** Begründung des Vorschlags. */
    String reasoning;

    /**
     * Geschätzter Einfluss der Änderung (0.0 – 1.0).
     *
     * <p>Höhere Werte bedeuten größere erwartete Verbesserung
     * (Performance, Stabilität, etc.).
     */
    float impact;
}