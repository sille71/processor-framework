package de.starima.pfw.base.processor.description.incubator.ai.domain;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.annotation.ValueObject;
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
@ValueObject
public class Suggestion {

    /** Pfad des Parameters (z.B. "reconProcessor.batchSize"). */
    @ProcessorParameter(description = "Dot-separated path to the parameter being suggested for improvement")
    String parameterPath;

    /** Aktueller Wert (z.B. "500"). */
    @ProcessorParameter(description = "Current configured value of the parameter")
    String currentValue;

    /** Vorgeschlagener Wert (z.B. "2000"). */
    @ProcessorParameter(description = "Suggested replacement value for the parameter")
    String suggestedValue;

    /** Begründung des Vorschlags. */
    @ProcessorParameter(description = "Natural language explanation of why this change is recommended")
    String reasoning;

    /**
     * Geschätzter Einfluss der Änderung (0.0 – 1.0).
     *
     * <p>Höhere Werte bedeuten größere erwartete Verbesserung
     * (Performance, Stabilität, etc.).
     */
    @ProcessorParameter(description = "Estimated impact of applying this suggestion, range 0.0–1.0")
    float impact;
}