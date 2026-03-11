package de.starima.pfw.base.processor.description.incubator.ai.domain;

/**
 * Typ einer {@link AIResponse} — steuert das UI-Verhalten.
 */
public enum AIResponseType {

    /** KI benötigt mehr Information vom Nutzer (Rückfrage). */
    QUESTION,

    /** KI bestätigt ihr Verständnis der Anforderung. */
    CONFIRMATION,

    /** Erster Konfigurations-Entwurf ist bereit. */
    DRAFT_READY,

    /** Eine Überarbeitung auf Basis von Nutzer-Feedback wurde eingearbeitet. */
    REFINEMENT_APPLIED,

    /** Entwurf ist validiert und bereit zur Konstruktion. */
    VALIDATED,

    /** Ein Fehler ist aufgetreten (z.B. API-Fehler, Validierungsfehler). */
    ERROR
}