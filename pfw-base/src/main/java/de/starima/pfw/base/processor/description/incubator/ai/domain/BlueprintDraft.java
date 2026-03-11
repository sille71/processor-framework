package de.starima.pfw.base.processor.description.incubator.ai.domain;

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
public class BlueprintDraft {

    /** Die generierte Konfiguration als beanParameterMap (beanId → parameter-Map). */
    Map<String, Map<String, Object>> beanParameterMap;

    /** Die Root-BeanId des konfigurierten Prozessors. */
    String rootProcessorId;

    // =========================================================================
    // KI-Erklärungen
    // =========================================================================

    /** Gesamterklärung: "Ich habe folgenden Prozess konfiguriert..." */
    String overallExplanation;

    /** Pro Parameter: warum diese Wahl? */
    List<DecisionExplanation> decisions;

    /** Annahmen der KI: "Ich nehme an, das CSV verwendet UTF-8..." */
    List<String> assumptions;

    /** Offene Fragen: "Welches Encoding hat die CSV-Datei?" */
    List<String> openQuestions;

    /**
     * Konfidenz-Score (0.0 – 1.0).
     *
     * <p>Gibt an, wie sicher die KI bei diesem Entwurf ist.
     * Bei < 0.7 empfiehlt sich eine Rückfrage an den Nutzer.
     */
    float confidenceScore;
}