package de.starima.pfw.base.processor.description.incubator.ai;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.incubator.ai.api.IAIExplainer;
import de.starima.pfw.base.processor.description.incubator.ai.api.IProcessorCatalog;
import de.starima.pfw.base.processor.description.incubator.ai.domain.Suggestion;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * KI-basierter Erklärer mit Claude als Sprachengine.
 *
 * <p>Analysiert Prozessoren und Konfigurationen und erzeugt natürlichsprachliche
 * Erklärungen via Anthropic Claude API.
 *
 * <p><b>Session 2 (TODO):</b> Vollständige Claude-API-Integration.
 * Aktuell: Stub-Implementierung mit strukturierten Antworten.
 *
 * <p><b>Säule-3-Anwendungsfall:</b>
 * <ul>
 *   <li>"Erkläre mir diesen Prozess" → explainConfiguration()</li>
 *   <li>"Was hat sich geändert?" → explainDiff()</li>
 *   <li>"Wie kann ich das verbessern?" → suggestImprovements()</li>
 * </ul>
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "KI-basierter Konfigurations-Erklärer (Claude API). " +
                "Erklärt laufende Prozessoren, Konfigurationsunterschiede und " +
                "schlägt Verbesserungen vor.",
        categories = {"ai", "explanation"},
        tags = {"explain", "describe", "diff", "suggest", "claude"}
)
public class ClaudeExplainer extends AbstractProcessor implements IAIExplainer {

    @ProcessorParameter(description = "Anthropic API Key.",
            ignoreInitialization = true)
    private String apiKey;

    @ProcessorParameter(value = "claude-sonnet-4-6",
            description = "Claude Model ID für Erklärungen.")
    private String model = "claude-sonnet-4-6";

    @ProcessorParameter(value = "4096",
            description = "Maximale Ausgabe-Token-Anzahl für Erklärungen.")
    private int maxTokens = 4096;

    // =========================================================================
    // explainConfiguration
    // =========================================================================

    @Override
    public String explainConfiguration(IProcessor processor) {
        if (processor == null) {
            return "Kein Prozessor übergeben.";
        }

        log.info("ClaudeExplainer.explainConfiguration: processor='{}'",
                processor.getFullBeanId());

        // TODO (Session 2): Claude API-Call mit extract()-Ergebnis als Kontext.
        return "[Stub] Prozessor: " + processor.getFullBeanId() +
                ". Claude API-Integration folgt in Session 2.";
    }

    @Override
    public String explainConfiguration(Map<String, Map<String, Object>> beanParameterMap,
                                       IProcessorCatalog catalog) {
        if (beanParameterMap == null || beanParameterMap.isEmpty()) {
            return "Leere Konfiguration — kein Prozessor konfiguriert.";
        }

        log.info("ClaudeExplainer.explainConfiguration: {} Beans", beanParameterMap.size());

        // TODO (Session 2): Claude API-Call mit beanParameterMap + Katalog als Kontext.
        StringBuilder sb = new StringBuilder("[Stub] Konfiguration mit ")
                .append(beanParameterMap.size())
                .append(" Prozessoren:\n");

        for (String beanId : beanParameterMap.keySet()) {
            sb.append("  - ").append(beanId).append("\n");
        }
        sb.append("Claude API-Integration folgt in Session 2.");
        return sb.toString();
    }

    // =========================================================================
    // explainDiff
    // =========================================================================

    @Override
    public String explainDiff(Map<String, Map<String, Object>> before,
                              Map<String, Map<String, Object>> after,
                              IProcessorCatalog catalog) {
        if (before == null || after == null) {
            return "Mindestens eine der Konfigurationen ist null.";
        }

        log.info("ClaudeExplainer.explainDiff: before={} Beans, after={} Beans",
                before.size(), after.size());

        // TODO (Session 2): Strukturierter Diff + Claude-Erklärung.
        long addedCount = after.keySet().stream()
                .filter(k -> !before.containsKey(k)).count();
        long removedCount = before.keySet().stream()
                .filter(k -> !after.containsKey(k)).count();
        long changedCount = after.keySet().stream()
                .filter(k -> before.containsKey(k)
                        && !before.get(k).equals(after.get(k))).count();

        return String.format("[Stub] Änderungen: %d hinzugefügt, %d entfernt, %d geändert. " +
                        "Claude API-Integration folgt in Session 2.",
                addedCount, removedCount, changedCount);
    }

    // =========================================================================
    // suggestImprovements
    // =========================================================================

    @Override
    public List<Suggestion> suggestImprovements(IProcessor processor, IProcessorCatalog catalog) {
        if (processor == null) {
            return List.of();
        }

        log.info("ClaudeExplainer.suggestImprovements: processor='{}'", processor.getFullBeanId());

        // TODO (Session 2): Claude API-Call für semantische Verbesserungsvorschläge.
        return List.of(
                Suggestion.builder()
                        .parameterPath("(allgemein)")
                        .currentValue("(unbekannt)")
                        .suggestedValue("(noch nicht analysiert)")
                        .reasoning("Claude API-Integration folgt in Session 2.")
                        .impact(0.0f)
                        .build()
        );
    }
}