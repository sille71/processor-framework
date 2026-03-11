package de.starima.pfw.base.processor.description.incubator.ai;

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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

/**
 * Natürlichsprachlicher Konfigurations-Assistent auf Basis der Claude API.
 *
 * <p>Koordiniert den vollständigen Dialog-Flow:
 * <ol>
 *   <li>{@link IAIIntentResolver} — Text → strukturierte Intention</li>
 *   <li>{@link IProcessorCatalog} — Passende Prozessoren suchen</li>
 *   <li>{@link IAIBlueprintGenerator} — Entwurf generieren</li>
 *   <li>{@link IAIConfigValidator} — Entwurf validieren</li>
 *   <li>{@link IIncubator#startEdit} — Edit-Session starten (bei DRAFT_READY)</li>
 * </ol>
 *
 * <p><b>Session 2 (TODO):</b> Vollständige Claude-API-Integration für
 * natürlichsprachliche Antworten und kontextsensitives Intent-Resolver.
 *
 * <h3>Dialog-Zustände</h3>
 * <pre>
 *   GATHERING_REQUIREMENTS → (Entwurf generierbar) → REVIEWING_DRAFT
 *   REVIEWING_DRAFT → (Feedback) → REFINING
 *   REFINING → (überarbeitet) → REVIEWING_DRAFT
 *   REVIEWING_DRAFT → (bestätigt) → CONFIRMED
 *   CONFIRMED → (konstruiert) → COMPLETED
 * </pre>
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Natürlichsprachlicher Konfigurations-Assistent (Claude API). " +
                "Verwaltet den Dialog zwischen Fachanwender und Framework. " +
                "Stellt Rückfragen, generiert Konfigurationsentwürfe und führt den Nutzer " +
                "durch den gesamten Konfigurations-Workflow.",
        categories = {"ai", "conversation"},
        tags = {"dialog", "chat", "naturalLanguage", "claude", "guidance", "assistant"}
)
public class ClaudeConversationProcessor extends AbstractProcessor
        implements IAIConversationProcessor {

    @ProcessorParameter(description = "Anthropic API Key für die Claude-Integration.",
            ignoreInitialization = true)
    private String apiKey;

    @ProcessorParameter(value = "claude-sonnet-4-6",
            description = "Claude Model ID für den Konversations-Assistenten.")
    private String model = "claude-sonnet-4-6";

    @ProcessorParameter(description = "System-Prompt-Template für den Assistenten. " +
            "Platzhalter ${catalogSummary} wird mit dem Katalog-Summary ersetzt.",
            ignoreInitialization = true)
    private String systemPromptTemplate;

    @ProcessorParameter(description = "Der Prozessor-Katalog als Wissensbasis für den Assistenten.",
            ignoreInitialization = true)
    private IProcessorCatalog processorCatalog;

    @ProcessorParameter(description = "Der Intent-Resolver für natürlichsprachliche Eingaben.",
            ignoreInitialization = true)
    private IAIIntentResolver intentResolver;

    @ProcessorParameter(description = "Der Blueprint-Generator (Claude API).",
            ignoreInitialization = true)
    private IAIBlueprintGenerator blueprintGenerator;

    @ProcessorParameter(description = "Der Konfigurations-Validator.",
            ignoreInitialization = true)
    private IAIConfigValidator configValidator;

    @ProcessorParameter(description = "Der Incubator für Edit-Sessions und Konstruktion.",
            ignoreInitialization = true)
    private IIncubator incubator;

    @ProcessorParameter(value = "0.7",
            description = "Minimaler Konfidenz-Score für automatisches Draft-Ready (0.0–1.0).")
    private float draftReadyThreshold = 0.7f;

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
    // processMessage
    // =========================================================================

    @Override
    public AIResponse processMessage(AIConversationSession session, String userMessage) {
        if (session == null) {
            log.warn("ClaudeConversationProcessor.processMessage: session ist null");
            return errorResponse("Keine aktive Session.");
        }
        if (userMessage == null || userMessage.isBlank()) {
            return questionResponse(session, "Bitte gib eine Anfrage ein.");
        }

        log.info("ClaudeConversationProcessor[{}]: Nachricht='{}', State={}",
                session.getSessionId(), userMessage, session.getState());

        // Nachricht zur History hinzufügen
        session.addUserMessage(userMessage);

        // State-basiertes Routing
        return switch (session.getState()) {
            case GATHERING_REQUIREMENTS -> handleGatheringRequirements(session, userMessage);
            case REVIEWING_DRAFT -> handleReviewingDraft(session, userMessage);
            case REFINING -> handleRefining(session, userMessage);
            case CONFIRMED -> handleConfirmed(session, userMessage);
            default -> errorResponse("Konversation ist in einem Endzustand: " + session.getState());
        };
    }

    // =========================================================================
    // State-Handler
    // =========================================================================

    private AIResponse handleGatheringRequirements(AIConversationSession session,
                                                    String userMessage) {
        // TODO (Session 2): IntentResolver + BlueprintGenerator mit echtem API-Call.

        // Intent auflösen (Stub: direkter Text)
        AIIntention intention = intentResolver != null
                ? intentResolver.resolve(userMessage, session)
                : AIIntention.builder()
                .intentType("CONFIGURE_PROCESS")
                .rawText(userMessage)
                .confidence(0.5f)
                .build();

        log.debug("handleGatheringRequirements: intent='{}', confidence={}",
                intention.getIntentType(), intention.getConfidence());

        // Wenn Konfidenz zu niedrig: Rückfrage
        if (intention.getConfidence() < 0.4f) {
            String response = buildClarificationQuestion(userMessage);
            session.addAssistantMessage(response);
            return AIResponse.builder()
                    .message(response)
                    .type(AIResponseType.QUESTION)
                    .build();
        }

        // Blueprint generieren
        BlueprintDraft draft = blueprintGenerator != null
                ? blueprintGenerator.generateDraft(intention, processorCatalog)
                : BlueprintDraft.builder()
                .overallExplanation("[Stub] BlueprintGenerator nicht konfiguriert.")
                .openQuestions(List.of("Welche Prozessoren sollen verwendet werden?"))
                .confidenceScore(0.0f)
                .build();

        session.setCurrentDraft(draft);

        // Wenn noch offene Fragen: weiter sammeln
        if (draft.getOpenQuestions() != null && !draft.getOpenQuestions().isEmpty()
                && draft.getConfidenceScore() < draftReadyThreshold) {

            String response = formatOpenQuestionsResponse(draft);
            session.addAssistantMessage(response);
            return AIResponse.builder()
                    .message(response)
                    .type(AIResponseType.QUESTION)
                    .draft(draft)
                    .build();
        }

        // Entwurf bereit: State wechseln und Edit-Session starten (TODO)
        session.setState(ConversationState.REVIEWING_DRAFT);

        String response = formatDraftReadyResponse(draft);
        session.addAssistantMessage(response);

        // Config validieren
        ValidationReport report = configValidator != null
                ? configValidator.validate(draft, processorCatalog)
                : null;

        return AIResponse.builder()
                .message(response)
                .type(report != null && !report.isValid()
                        ? AIResponseType.QUESTION
                        : AIResponseType.DRAFT_READY)
                .draft(draft)
                .build();
    }

    private AIResponse handleReviewingDraft(AIConversationSession session, String userMessage) {
        // TODO (Session 2): Semantische Analyse ob Bestätigung oder Änderungswunsch.

        String lower = userMessage.toLowerCase();
        boolean isConfirmation = lower.contains("ja") || lower.contains("ok")
                || lower.contains("gut") || lower.contains("start")
                || lower.contains("bestätig");

        if (isConfirmation) {
            session.setState(ConversationState.CONFIRMED);
            String response = "Entwurf bestätigt. Soll ich den Prozess jetzt konstruieren?";
            session.addAssistantMessage(response);
            return AIResponse.builder()
                    .message(response)
                    .type(AIResponseType.VALIDATED)
                    .draft(session.getCurrentDraft())
                    .suggestedActions(List.of("Prozess konstruieren", "Weiter bearbeiten"))
                    .build();
        }

        // Änderungswunsch → Refining
        session.setState(ConversationState.REFINING);
        return handleRefining(session, userMessage);
    }

    private AIResponse handleRefining(AIConversationSession session, String userMessage) {
        // TODO (Session 2): BlueprintGenerator.refineDraft() mit echtem API-Call.

        BlueprintDraft refined = blueprintGenerator != null
                ? blueprintGenerator.refineDraft(session.getCurrentDraft(), userMessage)
                : session.getCurrentDraft();

        session.setCurrentDraft(refined);
        session.setState(ConversationState.REVIEWING_DRAFT);

        String response = "[Stub] Entwurf überarbeitet. Feedback '" + userMessage +
                "' wurde verarbeitet. Claude API-Integration folgt in Session 2.";
        session.addAssistantMessage(response);

        return AIResponse.builder()
                .message(response)
                .type(AIResponseType.REFINEMENT_APPLIED)
                .draft(refined)
                .build();
    }

    private AIResponse handleConfirmed(AIConversationSession session, String userMessage) {
        // TODO (Session 2): incubator.startConstruct() aufrufen.
        log.info("ClaudeConversationProcessor[{}]: Konstruktion angefordert", session.getSessionId());
        session.setState(ConversationState.COMPLETED);

        String response = "[Stub] Konstruktion noch nicht implementiert (Session 2). " +
                "incubator.startConstruct() wird hier aufgerufen.";
        session.addAssistantMessage(response);

        return AIResponse.builder()
                .message(response)
                .type(AIResponseType.CONFIRMATION)
                .build();
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    private String buildClarificationQuestion(String userMessage) {
        return "Ich habe Ihre Anfrage noch nicht vollständig verstanden. " +
                "Könnten Sie bitte genauer beschreiben, was Sie benötigen?";
    }

    private String formatOpenQuestionsResponse(BlueprintDraft draft) {
        StringBuilder sb = new StringBuilder();
        if (draft.getOverallExplanation() != null) {
            sb.append(draft.getOverallExplanation()).append("\n\n");
        }
        sb.append("Ich habe noch folgende Fragen:\n");
        for (String q : draft.getOpenQuestions()) {
            sb.append("- ").append(q).append("\n");
        }
        return sb.toString();
    }

    private String formatDraftReadyResponse(BlueprintDraft draft) {
        StringBuilder sb = new StringBuilder("Entwurf erstellt");
        if (draft.getOverallExplanation() != null) {
            sb.append(": ").append(draft.getOverallExplanation());
        }
        sb.append("\n\nMöchten Sie den Prozess starten oder noch Änderungen vornehmen?");
        return sb.toString();
    }

    private AIResponse errorResponse(String message) {
        log.warn("ClaudeConversationProcessor: Fehler — {}", message);
        return AIResponse.builder()
                .message(message)
                .type(AIResponseType.ERROR)
                .build();
    }

    private AIResponse questionResponse(AIConversationSession session, String message) {
        if (session != null) session.addAssistantMessage(message);
        return AIResponse.builder()
                .message(message)
                .type(AIResponseType.QUESTION)
                .build();
    }
}