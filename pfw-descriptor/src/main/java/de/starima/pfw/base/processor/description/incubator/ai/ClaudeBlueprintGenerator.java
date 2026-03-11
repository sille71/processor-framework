package de.starima.pfw.base.processor.description.incubator.ai;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.incubator.ai.api.IAIBlueprintGenerator;
import de.starima.pfw.base.processor.description.incubator.ai.api.IProcessorCatalog;
import de.starima.pfw.base.processor.description.incubator.ai.domain.AIIntention;
import de.starima.pfw.base.processor.description.incubator.ai.domain.BlueprintDraft;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * KI-basierter Blueprint-Generator mit Claude als Planungsengine.
 *
 * <p>Sendet die strukturierte Intention + Prozessor-Katalog-Kontext an die
 * Anthropic Claude API und parst die strukturierte JSON-Antwort als
 * {@link BlueprintDraft}.
 *
 * <p><b>Session 2 (TODO):</b> Vollständige Claude-API-Integration.
 * In dieser Version werden Requests geloggt, aber keine echten API-Calls gemacht.
 * Die Methoden geben sinnvolle Stub-Antworten zurück, die den
 * Conversation-Flow ermöglichen.
 *
 * <h3>System-Prompt-Strategie</h3>
 * Der System-Prompt wird aus {@link IProcessorCatalog#getCapabilitySummary()} generiert
 * und mit dem Prozessor-Katalog als JSON-Kontext angereichert.
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "KI-basierter Blueprint-Generator (Claude API). " +
                "Generiert beanParameterMaps aus strukturierten Intentionen. " +
                "Nutzt den Prozessor-Katalog als Kontext im System-Prompt.",
        categories = {"ai", "planning"},
        tags = {"generate", "blueprint", "claude", "anthropic"}
)
public class ClaudeBlueprintGenerator extends AbstractProcessor implements IAIBlueprintGenerator {

    @ProcessorParameter(description = "Anthropic API Key (aus Umgebungsvariable ANTHROPIC_API_KEY empfohlen).",
            ignoreInitialization = true)
    private String apiKey;

    @ProcessorParameter(value = "claude-sonnet-4-6",
            description = "Claude Model ID für Blueprint-Generierung.")
    private String model = "claude-sonnet-4-6";

    @ProcessorParameter(value = "8192",
            description = "Maximale Ausgabe-Token-Anzahl.")
    private int maxTokens = 8192;

    @ProcessorParameter(description = "System-Prompt-Template. " +
            "Platzhalter ${catalogSummary} wird mit dem Katalog-Summary ersetzt.",
            ignoreInitialization = true)
    private String systemPromptTemplate;

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

        // TODO (Session 2): Anthropic SDK einbinden und echten API-Call machen.
        // Aktuell: Stub-Antwort die zeigt dass der Intent verstanden wurde.
        String catalogSummary = catalog != null ? catalog.getCapabilitySummary() : "(kein Katalog)";
        log.debug("Katalog-Summary:\n{}", catalogSummary);

        return BlueprintDraft.builder()
                .beanParameterMap(Map.of())
                .rootProcessorId(null)
                .overallExplanation("[Stub] Claude API-Integration folgt in Session 2. " +
                        "Intent erkannt: " + intention.getIntentType() +
                        ", Entitäten: " + intention.getEntities())
                .assumptions(List.of("API-Integration noch ausstehend"))
                .openQuestions(List.of("Claude API Key konfiguriert?"))
                .confidenceScore(0.0f)
                .build();
    }

    // =========================================================================
    // refineDraft
    // =========================================================================

    @Override
    public BlueprintDraft refineDraft(BlueprintDraft currentDraft, String feedback) {
        if (currentDraft == null) {
            log.warn("ClaudeBlueprintGenerator.refineDraft: currentDraft ist null");
            return emptyDraft("Kein bestehender Entwurf übergeben.");
        }
        if (feedback == null || feedback.isBlank()) {
            log.debug("ClaudeBlueprintGenerator.refineDraft: leeres Feedback, unveränderter Entwurf");
            return currentDraft;
        }

        log.info("ClaudeBlueprintGenerator.refineDraft: feedback='{}'", feedback);

        // TODO (Session 2): Anthropic SDK — Konversations-History + Refinement-Prompt.
        return currentDraft;
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    /**
     * Baut den System-Prompt aus Template + Katalog-Summary.
     */
    String buildSystemPrompt(IProcessorCatalog catalog) {
        String template = systemPromptTemplate != null ? systemPromptTemplate : DEFAULT_SYSTEM_PROMPT;
        String summary = catalog != null ? catalog.getCapabilitySummary() : "";
        return template.replace("${catalogSummary}", summary);
    }

    private BlueprintDraft emptyDraft(String reason) {
        return BlueprintDraft.builder()
                .beanParameterMap(Map.of())
                .overallExplanation(reason)
                .confidenceScore(0.0f)
                .build();
    }

    // =========================================================================
    // Default System-Prompt
    // =========================================================================

    private static final String DEFAULT_SYSTEM_PROMPT = """
            Du bist ein Konfigurations-Assistent für das Prozessorframework.

            Verfügbare Prozessoren:
            ${catalogSummary}

            Deine Aufgabe:
            1. Verstehe die Anforderung des Nutzers
            2. Finde passende Prozessoren im Katalog
            3. Erzeuge eine gültige beanParameterMap
            4. Erkläre deine Entscheidungen

            Antworte im folgenden JSON-Format:
            {
              "type": "QUESTION|DRAFT|REFINEMENT|CONFIRMATION",
              "message": "Natürlichsprachliche Antwort",
              "rootProcessorId": "prototypeId",
              "beanParameterMap": { ... },
              "overallExplanation": "...",
              "decisions": [ {"parameterPath": "...", "chosenValue": "...", "reasoning": "..."} ],
              "assumptions": [ "..." ],
              "openQuestions": [ "..." ],
              "confidenceScore": 0.8
            }
            """;
}