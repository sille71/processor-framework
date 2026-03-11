package de.starima.pfw.base.processor.description.incubator.ai.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Strukturierte Nutzerintention, extrahiert aus natürlichsprachlichem Text.
 *
 * <p>Wird von {@link de.starima.pfw.base.processor.description.incubator.ai.api.IAIIntentResolver}
 * erzeugt und dient als Input für
 * {@link de.starima.pfw.base.processor.description.incubator.ai.api.IAIBlueprintGenerator}.
 *
 * <h3>Intent-Typen</h3>
 * <ul>
 *   <li>{@code CONFIGURE_PROCESS} — Nutzer möchte einen neuen Prozess aufbauen</li>
 *   <li>{@code MODIFY_PROCESS} — Nutzer möchte einen bestehenden Prozess ändern</li>
 *   <li>{@code EXPLAIN} — Nutzer möchte eine Erklärung</li>
 *   <li>{@code QUERY} — Nutzer stellt eine Frage über das Framework</li>
 *   <li>{@code CONFIRM} — Nutzer bestätigt einen Vorschlag</li>
 *   <li>{@code REJECT} — Nutzer lehnt einen Vorschlag ab</li>
 * </ul>
 */
@Getter
@Builder
public class AIIntention {

    /**
     * Typ der Intention.
     *
     * <p>Bekannte Werte: CONFIGURE_PROCESS, MODIFY_PROCESS, EXPLAIN, QUERY, CONFIRM, REJECT.
     */
    String intentType;

    /**
     * Extrahierte Entitäten aus dem Nutzertext.
     *
     * <p>Beispiel: {@code {path: "/data/import", encoding: "UTF-8", source: "CSV"}}.
     */
    Map<String, Object> entities;

    /** Der originale Nutzertext. */
    String rawText;

    /**
     * Konfidenz-Score für die Intent-Erkennung (0.0 – 1.0).
     *
     * <p>Bei niedrigem Score (< 0.5) sollte eine Rückfrage gestellt werden.
     */
    float confidence;
}