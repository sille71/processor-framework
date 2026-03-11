package de.starima.pfw.base.processor.description.incubator.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Low-Level HTTP-Client für die Anthropic Claude API.
 *
 * <p>Kein Processor — reiner Infrastruktur-Helfer, der von den
 * Claude*-Prozessoren in {@code processorOnInit()} instanziiert wird.
 *
 * <h3>Anthropic Messages API</h3>
 * <pre>
 * POST https://api.anthropic.com/v1/messages
 * Headers:
 *   x-api-key: &lt;apiKey&gt;
 *   anthropic-version: 2023-06-01
 *   content-type: application/json
 * Body: { model, max_tokens, system, messages: [{role, content}] }
 * </pre>
 *
 * <h3>JSON-Extraktion</h3>
 * Claude kann JSON in Markdown-Codeblöcken zurückgeben. {@link #extractJson(String)}
 * bereinigt das automatisch.
 */
@Slf4j
public class ClaudeApiClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ClaudeApiClient(String apiKey, String model, int maxTokens) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    // =========================================================================
    // send — Freitext-Antwort
    // =========================================================================

    /**
     * Sendet eine Konversation an Claude und liefert den Antwort-Text.
     *
     * @param systemPrompt System-Prompt (Rolle und Format-Anweisungen)
     * @param messages     Konversations-History, jede Map hat "role" und "content"
     * @return Text-Antwort von Claude
     * @throws ClaudeApiException bei HTTP-Fehlern oder Parse-Fehlern
     */
    public String send(String systemPrompt, List<Map<String, Object>> messages)
            throws ClaudeApiException {
        try {
            String requestJson = buildRequestJson(systemPrompt, messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", API_VERSION)
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            log.debug("ClaudeApiClient: Sende Request an Claude (model={}, maxTokens={})",
                    model, maxTokens);

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("ClaudeApiClient: HTTP-Fehler {} — {}",
                        response.statusCode(), response.body());
                throw new ClaudeApiException("Anthropic API HTTP-Fehler: "
                        + response.statusCode() + " — " + response.body());
            }

            JsonNode responseJson = objectMapper.readTree(response.body());
            String text = responseJson
                    .path("content")
                    .get(0)
                    .path("text")
                    .asText();

            log.debug("ClaudeApiClient: Antwort empfangen ({} Zeichen)", text.length());
            return text;

        } catch (ClaudeApiException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ClaudeApiException("Claude API-Anfrage fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    /**
     * Sendet eine Konversation und parst die JSON-Antwort.
     *
     * <p>Extrahiert automatisch JSON aus Markdown-Codeblöcken.
     *
     * @throws ClaudeApiException bei HTTP-Fehlern oder wenn kein valides JSON zurückkommt
     */
    public JsonNode sendAndParseJson(String systemPrompt, List<Map<String, Object>> messages)
            throws ClaudeApiException {
        String text = send(systemPrompt, messages);
        String json = extractJson(text);
        try {
            return objectMapper.readTree(json);
        } catch (IOException e) {
            log.error("ClaudeApiClient: JSON-Parse-Fehler. Antwort: {}", text);
            throw new ClaudeApiException(
                    "Claude-Antwort ist kein valides JSON: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // Convenience-Factory für Nachrichtenhistorie
    // =========================================================================

    /**
     * Erzeugt eine Messages-Liste aus einem einzelnen User-Prompt.
     */
    public static List<Map<String, Object>> singleUserMessage(String content) {
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", content);
        messages.add(msg);
        return messages;
    }

    /**
     * Konvertiert eine {@link de.starima.pfw.base.processor.description.incubator.ai.domain.AIMessage}-Liste
     * in das Anthropic-Messages-Format.
     */
    public static List<Map<String, Object>> fromHistory(
            List<de.starima.pfw.base.processor.description.incubator.ai.domain.AIMessage> history) {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (history == null) return messages;
        for (var msg : history) {
            if (msg.getContent() == null || msg.getContent().isBlank()) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("role", msg.getRole().name().toLowerCase());
            m.put("content", msg.getContent());
            messages.add(m);
        }
        return messages;
    }

    // =========================================================================
    // JSON-Extraktion
    // =========================================================================

    /**
     * Extrahiert JSON aus einem Claude-Antworttext.
     *
     * <p>Claude verpackt JSON manchmal in Markdown-Codeblöcke (```json ... ```).
     * Diese Methode bereinigt das.
     */
    public static String extractJson(String text) {
        if (text == null) return "{}";

        // ```json ... ``` Block
        int start = text.indexOf("```json");
        if (start >= 0) {
            int end = text.indexOf("```", start + 7);
            if (end > start) return text.substring(start + 7, end).trim();
        }

        // ``` ... ``` Block
        start = text.indexOf("```");
        if (start >= 0) {
            int end = text.indexOf("```", start + 3);
            if (end > start) return text.substring(start + 3, end).trim();
        }

        // Roher { ... } Block
        int braceStart = text.indexOf('{');
        int braceEnd = text.lastIndexOf('}');
        if (braceStart >= 0 && braceEnd > braceStart) {
            return text.substring(braceStart, braceEnd + 1);
        }

        return text;
    }

    // =========================================================================
    // Interna
    // =========================================================================

    private String buildRequestJson(String systemPrompt,
                                    List<Map<String, Object>> messages) throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("model", model);
        request.put("max_tokens", maxTokens);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            request.put("system", systemPrompt);
        }
        request.put("messages", messages);
        return objectMapper.writeValueAsString(request);
    }

    // =========================================================================
    // Getter
    // =========================================================================

    public String getModel() { return model; }
    public int getMaxTokens() { return maxTokens; }

    // =========================================================================
    // Exception
    // =========================================================================

    /** Fehlerklasse für alle Claude-API-Fehler. */
    public static class ClaudeApiException extends RuntimeException {
        public ClaudeApiException(String message) { super(message); }
        public ClaudeApiException(String message, Throwable cause) { super(message, cause); }
    }
}