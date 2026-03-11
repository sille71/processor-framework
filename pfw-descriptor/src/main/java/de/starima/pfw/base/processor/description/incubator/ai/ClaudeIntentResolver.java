package de.starima.pfw.base.processor.description.incubator.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.incubator.ai.api.IAIIntentResolver;
import de.starima.pfw.base.processor.description.incubator.ai.domain.AIConversationSession;
import de.starima.pfw.base.processor.description.incubator.ai.domain.AIIntention;
import de.starima.pfw.base.processor.description.incubator.ai.domain.AIMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Claude-basierter Intent-Resolver.
 *
 * <p>Extrahiert aus natürlichsprachlichem Text eine strukturierte
 * {@link AIIntention} — Intent-Typ und Entitäten — mittels eines
 * einzigen Claude-API-Calls mit JSON-Ausgabe.
 *
 * <h3>Intent-Typen</h3>
 * <ul>
 *   <li>{@code CONFIGURE_PROCESS} — Neuen Prozess aufbauen</li>
 *   <li>{@code MODIFY_PROCESS} — Bestehenden Prozess ändern</li>
 *   <li>{@code EXPLAIN} — Erklärung anfordern</li>
 *   <li>{@code QUERY} — Frage über das Framework</li>
 *   <li>{@code CONFIRM} — Vorschlag bestätigen</li>
 *   <li>{@code REJECT} — Vorschlag ablehnen</li>
 * </ul>
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Claude-basierter Intent-Resolver. " +
                "Übersetzt natürlichsprachliche Nutzereingaben in strukturierte AIIntentions. " +
                "Extrahiert Intent-Typ (CONFIGURE_PROCESS, EXPLAIN, etc.) und Entitäten (Pfade, Werte, Typen).",
        categories = {"ai", "intent"},
        tags = {"nlp", "intent", "extraction", "claude", "naturalLanguage"}
)
public class ClaudeIntentResolver extends AbstractProcessor implements IAIIntentResolver {

    @ProcessorParameter(description = "Anthropic API Key.",
            ignoreInitialization = true)
    private String apiKey;

    @ProcessorParameter(value = "claude-haiku-4-5-20251001",
            description = "Claude Model ID (Haiku empfohlen: schnell + günstig für Intent-Extraktion).")
    private String model = "claude-haiku-4-5-20251001";

    @ProcessorParameter(value = "512",
            description = "Maximale Ausgabe-Token-Anzahl für Intent-Extraktion.")
    private int maxTokens = 512;

    // Transient — wird in processorOnInit() gebaut
    private transient ClaudeApiClient apiClient;
    private transient ObjectMapper objectMapper;

    @Override
    public void processorOnInit() {
        this.objectMapper = new ObjectMapper();
        if (apiKey != null && !apiKey.isBlank()) {
            this.apiClient = new ClaudeApiClient(apiKey, model, maxTokens);
            log.info("ClaudeIntentResolver: API-Client initialisiert (model={})", model);
        } else {
            log.warn("ClaudeIntentResolver: kein apiKey — Fallback auf Keyword-Matching");
        }
    }

    // =========================================================================
    // resolve — ohne Konversationskontext
    // =========================================================================

    @Override
    public AIIntention resolve(String naturalLanguageText) {
        return resolve(naturalLanguageText, null);
    }

    // =========================================================================
    // resolve — mit Konversationskontext
    // =========================================================================

    @Override
    public AIIntention resolve(String naturalLanguageText, AIConversationSession session) {
        if (naturalLanguageText == null || naturalLanguageText.isBlank()) {
            return emptyIntention(naturalLanguageText);
        }

        if (apiClient == null) {
            log.debug("ClaudeIntentResolver: kein API-Client, nutze Keyword-Fallback");
            return keywordFallback(naturalLanguageText);
        }

        try {
            String systemPrompt = INTENT_SYSTEM_PROMPT;
            List<Map<String, Object>> messages = buildMessages(naturalLanguageText, session);

            JsonNode response = apiClient.sendAndParseJson(systemPrompt, messages);
            return parseIntention(response, naturalLanguageText);

        } catch (ClaudeApiClient.ClaudeApiException e) {
            log.warn("ClaudeIntentResolver: API-Fehler, nutze Keyword-Fallback: {}", e.getMessage());
            return keywordFallback(naturalLanguageText);
        }
    }

    // =========================================================================
    // Parsing
    // =========================================================================

    private AIIntention parseIntention(JsonNode json, String rawText) {
        String intentType = json.path("intentType").asText("CONFIGURE_PROCESS");
        float confidence = (float) json.path("confidence").asDouble(0.8);

        Map<String, Object> entities = new HashMap<>();
        JsonNode entitiesNode = json.path("entities");
        if (entitiesNode.isObject()) {
            entitiesNode.fields().forEachRemaining(entry ->
                    entities.put(entry.getKey(), entry.getValue().asText()));
        }

        log.debug("ClaudeIntentResolver: intent='{}', confidence={}, entities={}",
                intentType, confidence, entities);

        return AIIntention.builder()
                .intentType(intentType)
                .entities(entities)
                .rawText(rawText)
                .confidence(confidence)
                .build();
    }

    // =========================================================================
    // Fallback: Keyword-Matching
    // =========================================================================

    private AIIntention keywordFallback(String text) {
        String lower = text.toLowerCase();
        String intentType = "CONFIGURE_PROCESS";
        float confidence = 0.5f;

        if (lower.contains("erkläre") || lower.contains("was macht") || lower.contains("wie funktioniert")) {
            intentType = "EXPLAIN";
            confidence = 0.7f;
        } else if (lower.contains("ändere") || lower.contains("modifiziere") || lower.contains("update")) {
            intentType = "MODIFY_PROCESS";
            confidence = 0.65f;
        } else if (lower.contains("ja") || lower.contains("ok") || lower.contains("gut") || lower.contains("bestätig")) {
            intentType = "CONFIRM";
            confidence = 0.9f;
        } else if (lower.contains("nein") || lower.contains("nicht") || lower.contains("abbrechen")) {
            intentType = "REJECT";
            confidence = 0.85f;
        } else if (lower.contains("was") || lower.contains("wie") || lower.contains("warum")) {
            intentType = "QUERY";
            confidence = 0.6f;
        }

        return AIIntention.builder()
                .intentType(intentType)
                .entities(new HashMap<>())
                .rawText(text)
                .confidence(confidence)
                .build();
    }

    private AIIntention emptyIntention(String rawText) {
        return AIIntention.builder()
                .intentType("UNKNOWN")
                .entities(new HashMap<>())
                .rawText(rawText != null ? rawText : "")
                .confidence(0.0f)
                .build();
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    private List<Map<String, Object>> buildMessages(String text, AIConversationSession session) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // Konversationskontext als User-Nachricht einleiten (optional)
        if (session != null && session.getHistory() != null && !session.getHistory().isEmpty()) {
            int last = Math.min(4, session.getHistory().size());
            List<AIMessage> recent = session.getHistory().subList(
                    session.getHistory().size() - last, session.getHistory().size());

            StringBuilder contextBuilder = new StringBuilder("Bisheriger Konversationskontext:\n");
            for (var msg : recent) {
                contextBuilder.append(msg.getRole().name()).append(": ").append(msg.getContent()).append("\n");
            }
            contextBuilder.append("\nAktuelle Eingabe: ").append(text);

            messages.addAll(ClaudeApiClient.singleUserMessage(contextBuilder.toString()));
        } else {
            messages.addAll(ClaudeApiClient.singleUserMessage(text));
        }

        return messages;
    }

    // =========================================================================
    // System-Prompt
    // =========================================================================

    private static final String INTENT_SYSTEM_PROMPT = """
            Du bist ein Intent-Extraktor für ein Konfigurationssystem.

            Extrahiere aus der Nutzereingabe:
            1. Den Intent-Typ (genau einer aus der Liste unten)
            2. Relevante Entitäten (Pfade, Werte, Typen, Namen)
            3. Konfidenz (0.0-1.0)

            Intent-Typen:
            - CONFIGURE_PROCESS: Nutzer möchte einen neuen Prozess konfigurieren
            - MODIFY_PROCESS: Nutzer möchte bestehende Konfiguration ändern
            - EXPLAIN: Nutzer möchte eine Erklärung (was macht X, wie funktioniert Y)
            - QUERY: Nutzer stellt eine Frage über das System
            - CONFIRM: Nutzer bestätigt einen Vorschlag (ja, ok, gut, starte, bestätige)
            - REJECT: Nutzer lehnt einen Vorschlag ab (nein, nicht, abbrechen)

            Antworte AUSSCHLIESSLICH in diesem JSON-Format (kein Text darum herum):
            {
              "intentType": "CONFIGURE_PROCESS",
              "entities": {
                "source": "CSV",
                "path": "/data/import",
                "encoding": "UTF-8"
              },
              "confidence": 0.9
            }
            """;
}