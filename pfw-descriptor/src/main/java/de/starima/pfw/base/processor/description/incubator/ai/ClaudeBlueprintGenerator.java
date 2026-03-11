package de.starima.pfw.base.processor.description.incubator.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.incubator.ai.api.IAIBlueprintGenerator;
import de.starima.pfw.base.processor.description.incubator.ai.api.IProcessorCatalog;
import de.starima.pfw.base.processor.description.incubator.ai.domain.AIConversationSession;
import de.starima.pfw.base.processor.description.incubator.ai.domain.AIIntention;
import de.starima.pfw.base.processor.description.incubator.ai.domain.BlueprintDraft;
import de.starima.pfw.base.processor.description.incubator.ai.domain.DecisionExplanation;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Claude-basierter Blueprint-Generator.
 *
 * <p>Sendet die strukturierte Intention + Prozessor-Katalog-Kontext an die
 * Anthropic Claude API und parst die JSON-Antwort direkt als {@link BlueprintDraft}.
 *
 * <h3>API-Protokoll</h3>
 * Claude antwortet ausschliesslich in einem definierten JSON-Format (kein Text drumherum).
 * Das JSON wird direkt in {@link BlueprintDraft} gemappt.
 *
 * <h3>Fallback</h3>
 * Wenn kein API-Key konfiguriert ist oder ein Fehler auftritt,
 * wird ein Stub-Entwurf mit {@code confidenceScore = 0.0} zurückgegeben.
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Claude-basierter Blueprint-Generator. " +
                "Generiert beanParameterMaps aus strukturierten Intentionen via Anthropic Claude API. " +
                "Nutzt den Prozessor-Katalog als Kontext im System-Prompt.",
        categories = {"ai", "planning"},
        tags = {"generate", "blueprint", "claude", "anthropic", "beanParameterMap"}
)
public class ClaudeBlueprintGenerator extends AbstractProcessor implements IAIBlueprintGenerator {

    @ProcessorParameter(description = "Anthropic API Key (Umgebungsvariable ANTHROPIC_API_KEY empfohlen).",
            ignoreInitialization = true)
    private String apiKey;

    @ProcessorParameter(value = "claude-sonnet-4-6",
            description = "Claude Model ID für Blueprint-Generierung.")
    private String model = "claude-sonnet-4-6";

    @ProcessorParameter(value = "8192",
            description = "Maximale Ausgabe-Token-Anzahl.")
    private int maxTokens = 8192;

    @ProcessorParameter(description = "Optionales System-Prompt-Template. " +
            "Platzhalter ${catalogSummary} wird mit dem Katalog-Summary ersetzt. " +
            "Wenn nicht gesetzt wird DEFAULT_SYSTEM_PROMPT verwendet.",
            ignoreInitialization = true)
    private String systemPromptTemplate;

    // Transient — in processorOnInit() gebaut
    private transient ClaudeApiClient apiClient;
    private transient ObjectMapper objectMapper;

    @Override
    public void processorOnInit() {
        this.objectMapper = new ObjectMapper();
        if (apiKey != null && !apiKey.isBlank()) {
            this.apiClient = new ClaudeApiClient(apiKey, model, maxTokens);
            log.info("ClaudeBlueprintGenerator: API-Client initialisiert (model={})", model);
        } else {
            log.warn("ClaudeBlueprintGenerator: kein apiKey — API-Calls deaktiviert");
        }
    }

    // =========================================================================
    // generateDraft
    // =========================================================================

    @Override
    public BlueprintDraft generateDraft(AIIntention intention, IProcessorCatalog catalog) {
        if (intention == null) {
            log.warn("ClaudeBlueprintGenerator.generateDraft: intention ist null");
            return emptyDraft("Keine Intention übergeben.");
        }

        log.info("ClaudeBlueprintGenerator.generateDraft: intentType='{}', entities={}",
                intention.getIntentType(), intention.getEntities());

        if (apiClient == null) {
            log.warn("ClaudeBlueprintGenerator: kein API-Client — Stub zurückgegeben");
            return emptyDraft("API-Key nicht konfiguriert. " +
                    "Setze @ProcessorParameter apiKey oder Umgebungsvariable ANTHROPIC_API_KEY.");
        }

        try {
            String systemPrompt = buildSystemPrompt(catalog);
            String userPrompt = buildGenerateUserPrompt(intention);
            List<Map<String, Object>> messages = ClaudeApiClient.singleUserMessage(userPrompt);

            JsonNode response = apiClient.sendAndParseJson(systemPrompt, messages);
            return parseBlueprintDraft(response);

        } catch (ClaudeApiClient.ClaudeApiException e) {
            log.error("ClaudeBlueprintGenerator.generateDraft: API-Fehler: {}", e.getMessage());
            return emptyDraft("Claude API-Fehler: " + e.getMessage());
        }
    }

    // =========================================================================
    // refineDraft
    // =========================================================================

    @Override
    public BlueprintDraft refineDraft(BlueprintDraft currentDraft, String feedback) {
        if (currentDraft == null) {
            log.warn("ClaudeBlueprintGenerator.refineDraft: currentDraft ist null");
            return emptyDraft("Kein bestehender Entwurf.");
        }
        if (feedback == null || feedback.isBlank()) {
            return currentDraft;
        }

        log.info("ClaudeBlueprintGenerator.refineDraft: feedback='{}'", feedback);

        if (apiClient == null) {
            log.warn("ClaudeBlueprintGenerator: kein API-Client — unveränderter Entwurf");
            return currentDraft;
        }

        try {
            String systemPrompt = buildSystemPrompt(null);

            // Conversation: aktueller Entwurf als Assistant-Antwort, Feedback als neue User-Nachricht
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user",
                    "content", "Generiere einen Konfigurations-Entwurf für das Prozessorframework."));
            messages.add(Map.of("role", "assistant",
                    "content", "```json\n" + draftToJson(currentDraft) + "\n```"));
            messages.add(Map.of("role", "user",
                    "content", "Überarbeite den Entwurf basierend auf diesem Feedback: " + feedback));

            JsonNode response = apiClient.sendAndParseJson(systemPrompt, messages);
            return parseBlueprintDraft(response);

        } catch (ClaudeApiClient.ClaudeApiException e) {
            log.error("ClaudeBlueprintGenerator.refineDraft: API-Fehler: {}", e.getMessage());
            return currentDraft;
        }
    }

    // =========================================================================
    // Parsing
    // =========================================================================

    private BlueprintDraft parseBlueprintDraft(JsonNode json) {
        String rootProcessorId = json.path("rootProcessorId").asText(null);
        String overallExplanation = json.path("overallExplanation").asText(null);
        float confidenceScore = (float) json.path("confidenceScore").asDouble(0.5);

        // beanParameterMap
        Map<String, Map<String, Object>> bpm = parseNestedMap(json.path("beanParameterMap"));

        // decisions
        List<DecisionExplanation> decisions = new ArrayList<>();
        JsonNode decisionsNode = json.path("decisions");
        if (decisionsNode.isArray()) {
            for (JsonNode d : decisionsNode) {
                decisions.add(DecisionExplanation.builder()
                        .parameterPath(d.path("parameterPath").asText())
                        .chosenValue(d.path("chosenValue").asText())
                        .reasoning(d.path("reasoning").asText())
                        .build());
            }
        }

        // assumptions
        List<String> assumptions = parseStringArray(json.path("assumptions"));

        // openQuestions
        List<String> openQuestions = parseStringArray(json.path("openQuestions"));

        log.debug("ClaudeBlueprintGenerator: Draft geparst — root='{}', bpm={} Einträge, score={}",
                rootProcessorId, bpm.size(), confidenceScore);

        return BlueprintDraft.builder()
                .rootProcessorId(rootProcessorId)
                .beanParameterMap(bpm)
                .overallExplanation(overallExplanation)
                .decisions(decisions)
                .assumptions(assumptions)
                .openQuestions(openQuestions)
                .confidenceScore(confidenceScore)
                .build();
    }

    // =========================================================================
    // System-Prompt / User-Prompt Aufbau
    // =========================================================================

    String buildSystemPrompt(IProcessorCatalog catalog) {
        String template = systemPromptTemplate != null ? systemPromptTemplate : DEFAULT_SYSTEM_PROMPT;
        String summary = catalog != null ? catalog.getCapabilitySummary() : "(kein Katalog verfügbar)";
        return template.replace("${catalogSummary}", summary);
    }

    private String buildGenerateUserPrompt(AIIntention intention) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generiere einen Konfigurations-Entwurf für folgende Anforderung:\n\n");
        sb.append("Anforderung: ").append(intention.getRawText()).append("\n");
        sb.append("Intent-Typ: ").append(intention.getIntentType()).append("\n");

        if (intention.getEntities() != null && !intention.getEntities().isEmpty()) {
            sb.append("Extrahierte Entitäten:\n");
            intention.getEntities().forEach((key, value) ->
                    sb.append("  - ").append(key).append(": ").append(value).append("\n"));
        }

        return sb.toString();
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    private Map<String, Map<String, Object>> parseNestedMap(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return new java.util.LinkedHashMap<>();
        }
        try {
            return objectMapper.convertValue(node,
                    new TypeReference<Map<String, Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("ClaudeBlueprintGenerator: beanParameterMap konnte nicht geparst werden: {}",
                    e.getMessage());
            return new java.util.LinkedHashMap<>();
        }
    }

    private List<String> parseStringArray(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(el -> result.add(el.asText()));
        }
        return result;
    }

    private String draftToJson(BlueprintDraft draft) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "rootProcessorId", draft.getRootProcessorId() != null ? draft.getRootProcessorId() : "",
                    "beanParameterMap", draft.getBeanParameterMap() != null ? draft.getBeanParameterMap() : Map.of(),
                    "overallExplanation", draft.getOverallExplanation() != null ? draft.getOverallExplanation() : "",
                    "confidenceScore", draft.getConfidenceScore()
            ));
        } catch (Exception e) {
            return "{}";
        }
    }

    private BlueprintDraft emptyDraft(String reason) {
        return BlueprintDraft.builder()
                .beanParameterMap(new java.util.LinkedHashMap<>())
                .overallExplanation(reason)
                .assumptions(new ArrayList<>())
                .openQuestions(new ArrayList<>())
                .confidenceScore(0.0f)
                .build();
    }

    // =========================================================================
    // Default System-Prompt
    // =========================================================================

    private static final String DEFAULT_SYSTEM_PROMPT = """
            Du bist ein Konfigurations-Assistent für das Prozessorframework "Starima PFW".
            Du hilfst dabei, Prozessoren zu konfigurieren, indem du beanParameterMaps generierst.

            Verfügbare Prozessoren:
            ${catalogSummary}

            AUFGABE: Generiere eine vollständige beanParameterMap für die Anforderung des Nutzers.

            Antworte AUSSCHLIESSLICH in diesem JSON-Format (kein Text darum herum):
            {
              "rootProcessorId": "prototypeId",
              "beanParameterMap": {
                "beanId": {
                  "parameterName": "parameterValue"
                }
              },
              "overallExplanation": "Erklärung was du konfiguriert hast",
              "decisions": [
                {
                  "parameterPath": "beanId.parameterName",
                  "chosenValue": "gewählter Wert",
                  "reasoning": "Begründung der Wahl"
                }
              ],
              "assumptions": ["Annahme 1", "Annahme 2"],
              "openQuestions": ["Offene Frage 1"],
              "confidenceScore": 0.8
            }

            WICHTIG:
            - Verwende nur Prozessoren aus dem Katalog oben
            - Setze confidenceScore < 0.7 wenn wichtige Informationen fehlen
            - Füge offene Fragen hinzu wenn Pflichtparameter unbekannt sind
            - BeanIds haben das Format: "prototypeId:instanceName@scope"
            """;
}