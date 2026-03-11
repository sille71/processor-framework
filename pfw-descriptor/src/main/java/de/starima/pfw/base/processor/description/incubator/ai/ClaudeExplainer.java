package de.starima.pfw.base.processor.description.incubator.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Claude-basierter Konfigurations-Erklärer.
 *
 * <p>Verwendet die Anthropic Claude API um:
 * <ul>
 *   <li>Prozessoren in natürlicher Sprache zu beschreiben</li>
 *   <li>Unterschiede zwischen zwei Konfigurationen zu erklären</li>
 *   <li>Verbesserungsvorschläge für bestehende Konfigurationen zu generieren</li>
 * </ul>
 *
 * <h3>Fallback</h3>
 * Wenn kein API-Key konfiguriert ist oder ein Fehler auftritt,
 * werden strukturierte Beschreibungen ohne KI zurückgegeben.
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Claude-basierter Konfigurations-Erklärer. " +
                "Erklärt laufende Prozessoren, Konfigurationsunterschiede und " +
                "generiert Verbesserungsvorschläge via Anthropic Claude API.",
        categories = {"ai", "explanation"},
        tags = {"explain", "describe", "diff", "suggest", "claude", "anthropic"}
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

    // Transient — in processorOnInit() gebaut
    private transient ClaudeApiClient apiClient;
    private transient ObjectMapper objectMapper;

    @Override
    public void processorOnInit() {
        this.objectMapper = new ObjectMapper();
        if (apiKey != null && !apiKey.isBlank()) {
            this.apiClient = new ClaudeApiClient(apiKey, model, maxTokens);
            log.info("ClaudeExplainer: API-Client initialisiert (model={})", model);
        } else {
            log.warn("ClaudeExplainer: kein apiKey — Fallback auf strukturierte Beschreibung");
        }
    }

    // =========================================================================
    // explainConfiguration — IProcessor
    // =========================================================================

    @Override
    public String explainConfiguration(IProcessor processor) {
        if (processor == null) return "Kein Prozessor übergeben.";

        log.info("ClaudeExplainer.explainConfiguration: processor='{}'", processor.getFullBeanId());

        if (apiClient == null) {
            return buildFallbackExplanation(processor);
        }

        try {
            String userPrompt = "Erkläre diesen Prozessor in verständlicher Sprache für einen Fachanwender:\n\n" +
                    "Prozessor-ID: " + processor.getFullBeanId() + "\n" +
                    "Typ: " + processor.getClass().getSimpleName() + "\n\n" +
                    "Beschreibe: Was macht dieser Prozessor? Wozu wird er verwendet? " +
                    "Was sind seine wichtigsten Eigenschaften?";

            return apiClient.send(EXPLAIN_SYSTEM_PROMPT,
                    ClaudeApiClient.singleUserMessage(userPrompt));

        } catch (ClaudeApiClient.ClaudeApiException e) {
            log.error("ClaudeExplainer.explainConfiguration: API-Fehler: {}", e.getMessage());
            return buildFallbackExplanation(processor);
        }
    }

    // =========================================================================
    // explainConfiguration — beanParameterMap
    // =========================================================================

    @Override
    public String explainConfiguration(Map<String, Map<String, Object>> beanParameterMap,
                                       IProcessorCatalog catalog) {
        if (beanParameterMap == null || beanParameterMap.isEmpty()) {
            return "Leere Konfiguration — kein Prozessor konfiguriert.";
        }

        log.info("ClaudeExplainer.explainConfiguration: {} Beans", beanParameterMap.size());

        if (apiClient == null) {
            return buildFallbackBpmExplanation(beanParameterMap);
        }

        try {
            StringBuilder configDesc = new StringBuilder();
            configDesc.append("Konfiguration:\n");
            beanParameterMap.forEach((beanId, params) -> {
                configDesc.append("\nProzessor: ").append(beanId).append("\n");
                if (params != null) {
                    params.forEach((k, v) ->
                            configDesc.append("  ").append(k).append(": ").append(v).append("\n"));
                }
            });

            String userPrompt = "Erkläre diese Prozessorkonfiguration in verständlicher Sprache:\n\n" +
                    configDesc + "\n\n" +
                    "Beschreibe: Was macht dieser konfigurierte Prozess? " +
                    "Wie arbeiten die Prozessoren zusammen? Was ist der Gesamtzweck?";

            return apiClient.send(EXPLAIN_SYSTEM_PROMPT,
                    ClaudeApiClient.singleUserMessage(userPrompt));

        } catch (ClaudeApiClient.ClaudeApiException e) {
            log.error("ClaudeExplainer: API-Fehler: {}", e.getMessage());
            return buildFallbackBpmExplanation(beanParameterMap);
        }
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

        if (apiClient == null) {
            return buildFallbackDiff(before, after);
        }

        try {
            String userPrompt = buildDiffPrompt(before, after);
            return apiClient.send(EXPLAIN_SYSTEM_PROMPT,
                    ClaudeApiClient.singleUserMessage(userPrompt));

        } catch (ClaudeApiClient.ClaudeApiException e) {
            log.error("ClaudeExplainer.explainDiff: API-Fehler: {}", e.getMessage());
            return buildFallbackDiff(before, after);
        }
    }

    // =========================================================================
    // suggestImprovements
    // =========================================================================

    @Override
    public List<Suggestion> suggestImprovements(IProcessor processor, IProcessorCatalog catalog) {
        if (processor == null) return List.of();

        log.info("ClaudeExplainer.suggestImprovements: processor='{}'", processor.getFullBeanId());

        if (apiClient == null) {
            return List.of();
        }

        try {
            String userPrompt = "Analysiere diesen Prozessor und schlage Verbesserungen vor:\n\n" +
                    "Prozessor-ID: " + processor.getFullBeanId() + "\n" +
                    "Typ: " + processor.getClass().getSimpleName() + "\n\n" +
                    "Antworte AUSSCHLIESSLICH in diesem JSON-Format:\n" +
                    """
                    {
                      "suggestions": [
                        {
                          "parameterPath": "prozessorId.parameterName",
                          "currentValue": "aktueller Wert",
                          "suggestedValue": "vorgeschlagener Wert",
                          "reasoning": "Begründung",
                          "impact": 0.8
                        }
                      ]
                    }
                    """;

            JsonNode response = apiClient.sendAndParseJson(EXPLAIN_SYSTEM_PROMPT,
                    ClaudeApiClient.singleUserMessage(userPrompt));

            return parseSuggestions(response);

        } catch (ClaudeApiClient.ClaudeApiException e) {
            log.error("ClaudeExplainer.suggestImprovements: API-Fehler: {}", e.getMessage());
            return List.of();
        }
    }

    // =========================================================================
    // Parsing
    // =========================================================================

    private List<Suggestion> parseSuggestions(JsonNode json) {
        List<Suggestion> suggestions = new ArrayList<>();
        JsonNode suggestionsNode = json.path("suggestions");
        if (!suggestionsNode.isArray()) return suggestions;

        for (JsonNode s : suggestionsNode) {
            suggestions.add(Suggestion.builder()
                    .parameterPath(s.path("parameterPath").asText())
                    .currentValue(s.path("currentValue").asText())
                    .suggestedValue(s.path("suggestedValue").asText())
                    .reasoning(s.path("reasoning").asText())
                    .impact((float) s.path("impact").asDouble(0.5))
                    .build());
        }

        suggestions.sort((a, b) -> Float.compare(b.getImpact(), a.getImpact()));
        return suggestions;
    }

    // =========================================================================
    // Fallback-Beschreibungen (kein API-Key)
    // =========================================================================

    private String buildFallbackExplanation(IProcessor processor) {
        return "Prozessor: " + processor.getFullBeanId() +
                " (Typ: " + processor.getClass().getSimpleName() + "). " +
                "Für eine KI-generierte Erklärung bitte apiKey konfigurieren.";
    }

    private String buildFallbackBpmExplanation(Map<String, Map<String, Object>> bpm) {
        StringBuilder sb = new StringBuilder("Konfigurierter Prozessorgraph mit ")
                .append(bpm.size()).append(" Prozessoren:\n");
        bpm.forEach((id, params) -> {
            sb.append("  - ").append(id);
            if (params != null && !params.isEmpty()) {
                sb.append(" (").append(params.size()).append(" Parameter)");
            }
            sb.append("\n");
        });
        sb.append("Für KI-Erklärung bitte apiKey konfigurieren.");
        return sb.toString();
    }

    private String buildFallbackDiff(Map<String, Map<String, Object>> before,
                                     Map<String, Map<String, Object>> after) {
        long added = after.keySet().stream().filter(k -> !before.containsKey(k)).count();
        long removed = before.keySet().stream().filter(k -> !after.containsKey(k)).count();
        long changed = after.keySet().stream()
                .filter(k -> before.containsKey(k) && !before.get(k).equals(after.get(k))).count();
        return String.format("Änderungen: %d Prozessoren hinzugefügt, %d entfernt, %d geändert.",
                added, removed, changed);
    }

    private String buildDiffPrompt(Map<String, Map<String, Object>> before,
                                   Map<String, Map<String, Object>> after) {
        StringBuilder sb = new StringBuilder("Erkläre die Unterschiede zwischen diesen zwei Konfigurationen:\n\n");

        sb.append("VORHER:\n");
        before.forEach((id, params) -> {
            sb.append("  Prozessor: ").append(id).append("\n");
            if (params != null) params.forEach((k, v) ->
                    sb.append("    ").append(k).append(": ").append(v).append("\n"));
        });

        sb.append("\nNACHHER:\n");
        after.forEach((id, params) -> {
            sb.append("  Prozessor: ").append(id).append("\n");
            if (params != null) params.forEach((k, v) ->
                    sb.append("    ").append(k).append(": ").append(v).append("\n"));
        });

        sb.append("\nBeschreibe konkret was sich geändert hat und was das bedeutet.");
        return sb.toString();
    }

    // =========================================================================
    // System-Prompt
    // =========================================================================

    private static final String EXPLAIN_SYSTEM_PROMPT = """
            Du bist ein Experte für das Prozessorframework "Starima PFW".
            Du erklärst Konfigurationen und Prozessoren in einfacher, verständlicher Sprache
            für Fachanwender ohne technisches Hintergrundwissen.

            Wichtige Regeln:
            - Erkläre in 2-4 Sätzen, präzise und verständlich
            - Vermeide technischen Jargon (kein Java, kein Framework-Intern)
            - Fokus auf: Was macht es? Wozu dient es? Was sind die Auswirkungen?
            - Antworte auf Deutsch
            """;
}