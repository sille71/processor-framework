package de.starima.pfw.base.processor.description.incubator.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.description.incubator.ai.api.IAIBlueprintGenerator;
import de.starima.pfw.base.processor.description.incubator.ai.api.IAIConfigValidator;
import de.starima.pfw.base.processor.description.incubator.ai.api.IAIConversationProcessor;
import de.starima.pfw.base.processor.description.incubator.ai.api.IAIIntentResolver;
import de.starima.pfw.base.processor.description.incubator.ai.api.IProcessorCatalog;
import de.starima.pfw.base.processor.description.incubator.ai.domain.*;
import de.starima.pfw.base.processor.description.incubator.api.IIncubator;
import de.starima.pfw.base.processor.description.incubator.domain.DefaultConstructTaskContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Natürlichsprachlicher Konfigurations-Assistent auf Basis der Anthropic Claude API.
 *
 * <p>Implementiert einen vollständigen Dialog-Flow:
 * <ol>
 *   <li>Natürliche Sprache → strukturierte Antwort via Claude</li>
 *   <li>Inkrementelles Sammeln von Anforderungen durch Rückfragen</li>
 *   <li>Automatische Blueprint-Generierung wenn genug Info vorhanden</li>
 *   <li>Entwurf-Validierung via {@link IAIConfigValidator}</li>
 *   <li>Edit-Session-Start via {@link IIncubator}</li>
 *   <li>Prozess-Konstruktion via {@link IIncubator#startConstruct}</li>
 * </ol>
 *
 * <h3>API-Protokoll</h3>
 * Claude antwortet auf jede Nutzernachricht mit strukturiertem JSON,
 * das {@code type}, {@code message} und optional {@code beanParameterMap} enthält.
 * Das ermöglicht state-freie Server-Implementierungen.
 *
 * <h3>Fallback</h3>
 * Ohne API-Key bleibt die State-Machine funktional, aber ohne KI-Antworten.
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Natürlichsprachlicher Konfigurations-Assistent (Anthropic Claude API). " +
                "Führt Fachanwender per Dialog durch die vollständige Prozesskonfiguration. " +
                "Stellt Rückfragen, generiert beanParameterMaps und startet den konstruierten Prozess.",
        categories = {"ai", "conversation"},
        tags = {"dialog", "chat", "naturalLanguage", "claude", "guidance", "assistant", "anthropic"}
)
public class ClaudeConversationProcessor extends AbstractProcessor
        implements IAIConversationProcessor {

    // =========================================================================
    // @ProcessorParameter-Felder
    // =========================================================================

    @ProcessorParameter(description = "Anthropic API Key.",
            ignoreInitialization = true)
    private String apiKey;

    @ProcessorParameter(value = "claude-sonnet-4-6",
            description = "Claude Model ID.")
    private String model = "claude-sonnet-4-6";

    @ProcessorParameter(value = "4096",
            description = "Maximale Ausgabe-Token-Anzahl pro Konversations-Turn.")
    private int maxTokens = 4096;

    @ProcessorParameter(description = "System-Prompt-Template. " +
            "Platzhalter ${catalogSummary} wird ersetzt.",
            ignoreInitialization = true)
    private String systemPromptTemplate;

    @ProcessorParameter(description = "Prozessor-Katalog als Wissensbasis.",
            ignoreInitialization = true)
    private IProcessorCatalog processorCatalog;

    @ProcessorParameter(description = "Intent-Resolver (optional — ConversationProcessor kann auch ohne ihn arbeiten).",
            ignoreInitialization = true)
    private IAIIntentResolver intentResolver;

    @ProcessorParameter(description = "Blueprint-Generator für programmatische Entwurfs-Generierung.",
            ignoreInitialization = true)
    private IAIBlueprintGenerator blueprintGenerator;

    @ProcessorParameter(description = "Konfigurations-Validator.",
            ignoreInitialization = true)
    private IAIConfigValidator configValidator;

    @ProcessorParameter(description = "Incubator für Edit-Sessions und Konstruktion.",
            ignoreInitialization = true)
    private IIncubator incubator;

    @ProcessorParameter(value = "0.75",
            description = "Minimaler Konfidenz-Score für automatischen DRAFT_READY-Übergang.")
    private float draftReadyThreshold = 0.75f;

    // Transient — in processorOnInit() gebaut
    private transient ClaudeApiClient apiClient;
    private transient ObjectMapper objectMapper;

    @Override
    public void processorOnInit() {
        this.objectMapper = new ObjectMapper();
        if (apiKey != null && !apiKey.isBlank()) {
            this.apiClient = new ClaudeApiClient(apiKey, model, maxTokens);
            log.info("ClaudeConversationProcessor: API-Client initialisiert (model={})", model);
        } else {
            log.warn("ClaudeConversationProcessor: kein apiKey — nur Fallback-Modus verfügbar");
        }
    }

    // =========================================================================
    // startConversation
    // =========================================================================

    @Override
    public AIConversationSession startConversation(IProcessorContext context) {
        String sessionId = UUID.randomUUID().toString();
        log.info("ClaudeConversationProcessor: Neue Session gestartet '{}'", sessionId);

        return AIConversationSession.builder()
                .sessionId(sessionId)
                .state(ConversationState.GATHERING_REQUIREMENTS)
                .build();
    }

    // =========================================================================
    // processMessage — Haupt-Einstiegspunkt
    // =========================================================================

    @Override
    public AIResponse processMessage(AIConversationSession session, String userMessage) {
        if (session == null) {
            return errorResponse("Keine aktive Session.");
        }
        if (userMessage == null || userMessage.isBlank()) {
            return questionResponse(session, "Bitte gib eine Anfrage ein.");
        }

        log.info("ClaudeConversationProcessor[{}]: Nachricht='{}', State={}",
                session.getSessionId(),
                userMessage.length() > 80 ? userMessage.substring(0, 80) + "..." : userMessage,
                session.getState());

        // User-Nachricht zur History hinzufügen
        session.addUserMessage(userMessage);

        // CONFIRMED → Konstruktion starten
        if (session.getState() == ConversationState.CONFIRMED) {
            return handleConstruct(session);
        }

        // Endzustände
        if (session.getState() == ConversationState.COMPLETED
                || session.getState() == ConversationState.ABORTED) {
            return errorResponse("Konversation ist abgeschlossen (State: " + session.getState() + ").");
        }

        // API verfügbar: alles über Claude
        if (apiClient != null) {
            return processWithClaude(session, userMessage);
        }

        // Fallback: regelbasierte State-Machine
        return processFallback(session, userMessage);
    }

    // =========================================================================
    // Claude-basierte Verarbeitung (Haupt-Implementierung)
    // =========================================================================

    private AIResponse processWithClaude(AIConversationSession session, String userMessage) {
        try {
            String systemPrompt = buildConversationSystemPrompt();
            List<Map<String, Object>> messages = ClaudeApiClient.fromHistory(session.getHistory());

            JsonNode response = apiClient.sendAndParseJson(systemPrompt, messages);
            return parseAndApplyResponse(session, response);

        } catch (ClaudeApiClient.ClaudeApiException e) {
            log.error("ClaudeConversationProcessor[{}]: API-Fehler: {}",
                    session.getSessionId(), e.getMessage());
            // Fallback auf regelbasierte Verarbeitung
            return processFallback(session, userMessage);
        }
    }

    /**
     * Parst die JSON-Antwort von Claude und wendet sie auf die Session an.
     */
    private AIResponse parseAndApplyResponse(AIConversationSession session, JsonNode json) {
        String typeStr = json.path("type").asText("QUESTION");
        String message = json.path("message").asText("(keine Antwort)");
        float confidence = (float) json.path("confidenceScore").asDouble(0.5);

        // Assistant-Antwort zur History hinzufügen
        session.addAssistantMessage(message);

        // Optional: beanParameterMap extrahieren
        JsonNode bpmNode = json.path("beanParameterMap");
        Map<String, Map<String, Object>> bpm = null;
        if (bpmNode.isObject() && !bpmNode.isEmpty()) {
            try {
                bpm = objectMapper.convertValue(bpmNode,
                        new TypeReference<Map<String, Map<String, Object>>>() {});
            } catch (Exception e) {
                log.warn("ClaudeConversationProcessor: beanParameterMap-Parse-Fehler: {}", e.getMessage());
            }
        }

        // BlueprintDraft aufbauen wenn bpm vorhanden
        BlueprintDraft draft = null;
        if (bpm != null && !bpm.isEmpty()) {
            String rootProcessorId = json.path("rootProcessorId").asText(null);
            String overallExplanation = json.path("overallExplanation").asText(null);
            List<String> openQuestions = parseStringList(json.path("openQuestions"));
            List<String> assumptions = parseStringList(json.path("assumptions"));

            draft = BlueprintDraft.builder()
                    .rootProcessorId(rootProcessorId)
                    .beanParameterMap(bpm)
                    .overallExplanation(overallExplanation)
                    .openQuestions(openQuestions)
                    .assumptions(assumptions)
                    .confidenceScore(confidence)
                    .build();
            session.setCurrentDraft(draft);
        }

        // State-Übergang
        AIResponseType responseType = mapResponseType(typeStr, confidence, draft);
        applyStateTransition(session, responseType);

        // Validierung wenn Entwurf bereit
        if (responseType == AIResponseType.DRAFT_READY && draft != null
                && configValidator != null) {
            ValidationReport report = configValidator.validate(draft, processorCatalog);
            if (!report.isValid()) {
                log.warn("ClaudeConversationProcessor: Entwurf invalid ({} Errors)",
                        report.getIssues().stream()
                                .filter(i -> i.getSeverity() == ValidationReport.ValidationIssue.Severity.ERROR)
                                .count());
                responseType = AIResponseType.QUESTION;
            }
        }

        return AIResponse.builder()
                .message(message)
                .type(responseType)
                .draft(draft)
                .build();
    }

    // =========================================================================
    // Konstruktion (CONFIRMED-State)
    // =========================================================================

    private AIResponse handleConstruct(AIConversationSession session) {
        log.info("ClaudeConversationProcessor[{}]: Starte Konstruktion", session.getSessionId());

        BlueprintDraft draft = session.getCurrentDraft();
        if (draft == null || draft.getRootProcessorId() == null) {
            return errorResponse("Kein valider Entwurf vorhanden — Konstruktion abgebrochen.");
        }

        if (incubator == null) {
            String response = "Kein Incubator konfiguriert — Konstruktion nicht möglich.";
            session.addAssistantMessage(response);
            return AIResponse.builder().message(response).type(AIResponseType.ERROR).build();
        }

        try {
            // ConstructTaskContext mit Blueprint-Daten aufbauen
            final BlueprintDraft finalDraft = draft;
            DefaultConstructTaskContext ctx = new DefaultConstructTaskContext() {
                @Override
                public Map<String, Map<String, Object>> getBeanParameterMap() {
                    return finalDraft.getBeanParameterMap() != null
                            ? finalDraft.getBeanParameterMap()
                            : new LinkedHashMap<>();
                }
            };
            ctx.setRootBeanId(draft.getRootProcessorId());
            ctx.setRuntimeContext(getRuntimeContext());

            var constructSession = incubator.startConstruct(ctx);

            if (constructSession != null && constructSession.getRoot() != null) {
                session.setState(ConversationState.COMPLETED);
                String response = "Prozess '" + draft.getRootProcessorId() +
                        "' wurde erfolgreich konstruiert und ist bereit.";
                session.addAssistantMessage(response);
                return AIResponse.builder()
                        .message(response)
                        .type(AIResponseType.CONFIRMATION)
                        .build();
            } else {
                String response = "Konstruktion fehlgeschlagen — Prozess konnte nicht erzeugt werden.";
                session.addAssistantMessage(response);
                return AIResponse.builder()
                        .message(response)
                        .type(AIResponseType.ERROR)
                        .build();
            }

        } catch (Exception e) {
            log.error("ClaudeConversationProcessor[{}]: Konstruktion fehlgeschlagen: {}",
                    session.getSessionId(), e.getMessage());
            return errorResponse("Konstruktionsfehler: " + e.getMessage());
        }
    }

    // =========================================================================
    // Fallback: regelbasierte State-Machine (kein API-Key)
    // =========================================================================

    private AIResponse processFallback(AIConversationSession session, String userMessage) {
        String lower = userMessage.toLowerCase();

        // Bestätigungserkennung
        boolean isConfirmation = lower.contains("ja") || lower.contains("ok")
                || lower.contains("gut") || lower.contains("start") || lower.contains("bestätig");
        boolean isRejection = lower.contains("nein") || lower.contains("nicht")
                || lower.contains("abbrechen") || lower.contains("stop");

        String response;
        AIResponseType type;

        switch (session.getState()) {
            case GATHERING_REQUIREMENTS -> {
                response = "Um einen Prozess zu konfigurieren, benötige ich mehr Informationen. " +
                        "Was soll der Prozess tun? (API-Key nicht konfiguriert — bitte apiKey setzen.)";
                type = AIResponseType.QUESTION;
            }
            case REVIEWING_DRAFT -> {
                if (isConfirmation) {
                    session.setState(ConversationState.CONFIRMED);
                    response = "Entwurf bestätigt. Starte die Konstruktion...";
                    type = AIResponseType.VALIDATED;
                } else if (isRejection) {
                    session.setState(ConversationState.ABORTED);
                    response = "Konfiguration abgebrochen.";
                    type = AIResponseType.CONFIRMATION;
                } else {
                    session.setState(ConversationState.REFINING);
                    response = "Änderungswunsch notiert. Bitte gib an was geändert werden soll.";
                    type = AIResponseType.QUESTION;
                }
            }
            case REFINING -> {
                session.setState(ConversationState.REVIEWING_DRAFT);
                response = "Überarbeitung verarbeitet. Entwurf aktualisiert. Bestätigen?";
                type = AIResponseType.REFINEMENT_APPLIED;
            }
            default -> {
                response = "Unerwarteter Zustand: " + session.getState();
                type = AIResponseType.ERROR;
            }
        }

        session.addAssistantMessage(response);
        return AIResponse.builder()
                .message(response)
                .type(type)
                .draft(session.getCurrentDraft())
                .build();
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    private String buildConversationSystemPrompt() {
        String template = systemPromptTemplate != null
                ? systemPromptTemplate
                : CONVERSATION_SYSTEM_PROMPT;
        String summary = processorCatalog != null
                ? processorCatalog.getCapabilitySummary()
                : "(kein Katalog verfügbar)";
        return template.replace("${catalogSummary}", summary);
    }

    private AIResponseType mapResponseType(String typeStr, float confidence, BlueprintDraft draft) {
        return switch (typeStr.toUpperCase()) {
            case "QUESTION" -> AIResponseType.QUESTION;
            case "DRAFT_READY", "DRAFT" -> AIResponseType.DRAFT_READY;
            case "REFINEMENT_APPLIED", "REFINEMENT" -> AIResponseType.REFINEMENT_APPLIED;
            case "VALIDATED", "CONFIRMATION", "CONFIRMED" -> AIResponseType.VALIDATED;
            case "ERROR" -> AIResponseType.ERROR;
            default -> (draft != null && confidence >= draftReadyThreshold)
                    ? AIResponseType.DRAFT_READY
                    : AIResponseType.QUESTION;
        };
    }

    private void applyStateTransition(AIConversationSession session, AIResponseType type) {
        switch (type) {
            case DRAFT_READY -> session.setState(ConversationState.REVIEWING_DRAFT);
            case REFINEMENT_APPLIED -> session.setState(ConversationState.REVIEWING_DRAFT);
            case VALIDATED -> session.setState(ConversationState.CONFIRMED);
            default -> { /* State bleibt */ }
        }
    }

    private List<String> parseStringList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(el -> result.add(el.asText()));
        }
        return result;
    }

    private AIResponse errorResponse(String message) {
        log.warn("ClaudeConversationProcessor: Fehler — {}", message);
        return AIResponse.builder().message(message).type(AIResponseType.ERROR).build();
    }

    private AIResponse questionResponse(AIConversationSession session, String message) {
        if (session != null) session.addAssistantMessage(message);
        return AIResponse.builder().message(message).type(AIResponseType.QUESTION).build();
    }

    // =========================================================================
    // Default System-Prompt
    // =========================================================================

    private static final String CONVERSATION_SYSTEM_PROMPT = """
            Du bist ein Konfigurations-Assistent für das Prozessorframework "Starima PFW".
            Deine Aufgabe: Fachanwender durch die Konfiguration eines Prozesses führen.

            Verfügbare Prozessoren:
            ${catalogSummary}

            DIALOG-REGELN:
            1. Stelle gezielte Rückfragen bis du genug Information hast
            2. Generiere eine beanParameterMap sobald du genug weißt
            3. Erkläre was du konfiguriert hast und warum
            4. Frage nach Bestätigung bevor du den Prozess startest

            ANTWORT-FORMAT (IMMER dieses JSON, kein Text darum herum):
            {
              "type": "QUESTION",
              "message": "Natürlichsprachliche Antwort an den Nutzer (Deutsch)",
              "rootProcessorId": null,
              "beanParameterMap": {},
              "overallExplanation": null,
              "decisions": [],
              "assumptions": [],
              "openQuestions": [],
              "confidenceScore": 0.0
            }

            TYPE-WERTE:
            - "QUESTION"            → Rückfrage, mehr Info benötigt
            - "DRAFT_READY"         → Erster vollständiger Entwurf fertig (confidenceScore >= 0.75)
            - "REFINEMENT_APPLIED"  → Überarbeitung auf Basis von Feedback
            - "VALIDATED"           → Entwurf bestätigt, bereit zur Konstruktion
            - "ERROR"               → Fehler aufgetreten

            WICHTIG:
            - Setze beanParameterMap nur wenn type = DRAFT_READY oder REFINEMENT_APPLIED
            - confidenceScore < 0.75 → type = QUESTION (mehr Info sammeln)
            - Antworte immer auf Deutsch
            - BeanId-Format: "prototypeId:instanceName@scope" (z.B. "csvReaderProcessor:src1@parentcontext")
            """;
}