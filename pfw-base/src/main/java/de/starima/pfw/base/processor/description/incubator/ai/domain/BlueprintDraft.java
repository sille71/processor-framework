package de.starima.pfw.base.processor.description.incubator.ai.domain;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.annotation.ValueObject;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Ein KI-generierter Konfigurationsentwurf.
 *
 * <p>Enthält die beanParameterMap UND natürlichsprachliche Erklärungen
 * für jede getroffene Entscheidung. Wird vom
 * {@link de.starima.pfw.base.processor.description.incubator.ai.api.IAIBlueprintGenerator}
 * erzeugt und durch
 * {@link de.starima.pfw.base.processor.description.incubator.ai.api.IAIConfigValidator}
 * validiert.
 *
 * <p>Ein Entwurf mit {@code confidenceScore < 0.7} sollte dem Nutzer mit offenen
 * Fragen präsentiert werden, bevor er konstruiert wird.
 */
@Getter
@Setter
@Builder
@ValueObject
public class BlueprintDraft {

    /** Die generierte Konfiguration als beanParameterMap (beanId → parameter-Map). */
    @ProcessorParameter(description = "Generated configuration as beanParameterMap (beanId to parameter map)")
    Map<String, Map<String, Object>> beanParameterMap;

    /** Die Root-BeanId des konfigurierten Prozessors. */
    @ProcessorParameter(description = "Root bean ID of the top-level configured processor")
    String rootProcessorId;

    // =========================================================================
    // KI-Erklärungen
    // =========================================================================

    /** Gesamterklärung: "Ich habe folgenden Prozess konfiguriert..." */
    @ProcessorParameter(description = "Overall natural language explanation of the generated configuration")
    String overallExplanation;

    /** Pro Parameter: warum diese Wahl? */
    @ProcessorParameter(description = "Per-parameter explanations for each configuration decision made by the AI")
    List<DecisionExplanation> decisions;

    /** Annahmen der KI: "Ich nehme an, das CSV verwendet UTF-8..." */
    @ProcessorParameter(description = "Assumptions the AI made when generating this draft")
    List<String> assumptions;

    /** Offene Fragen: "Welches Encoding hat die CSV-Datei?" */
    @ProcessorParameter(description = "Open questions the AI could not resolve from the user input alone")
    List<String> openQuestions;

    /**
     * Konfidenz-Score (0.0 – 1.0).
     *
     * <p>Gibt an, wie sicher die KI bei diesem Entwurf ist.
     * Bei < 0.7 empfiehlt sich eine Rückfrage an den Nutzer.
     */
    @ProcessorParameter(description = "Overall confidence score for this draft, range 0.0–1.0")
    float confidenceScore;
}