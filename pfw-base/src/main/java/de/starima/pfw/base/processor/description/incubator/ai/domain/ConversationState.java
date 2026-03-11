package de.starima.pfw.base.processor.description.incubator.ai.domain;

/**
 * Zustandsautomat einer {@link AIConversationSession}.
 *
 * <h3>Übergänge</h3>
 * <pre>
 * GATHERING_REQUIREMENTS
 *   → REVIEWING_DRAFT       (erster Entwurf wurde generiert)
 *   → GATHERING_REQUIREMENTS (mehr Information benötigt)
 *
 * REVIEWING_DRAFT
 *   → REFINING              (Nutzer möchte Änderungen)
 *   → CONFIRMED             (Nutzer bestätigt den Entwurf)
 *
 * REFINING
 *   → REVIEWING_DRAFT       (überarbeiteter Entwurf bereit)
 *
 * CONFIRMED
 *   → COMPLETED             (Prozess wurde konstruiert und gestartet)
 *
 * * → ABORTED               (Nutzer bricht ab)
 * </pre>
 */
public enum ConversationState {

    /** KI sammelt Anforderungen durch Rückfragen. */
    GATHERING_REQUIREMENTS,

    /** Ein erster Entwurf liegt vor und wird dem Nutzer gezeigt. */
    REVIEWING_DRAFT,

    /** Nutzer möchte den Entwurf verfeinern, KI arbeitet an einer Überarbeitung. */
    REFINING,

    /** Nutzer hat den Entwurf bestätigt, bereit zur Konstruktion. */
    CONFIRMED,

    /** Prozess wurde erfolgreich konstruiert und gestartet. */
    COMPLETED,

    /** Konversation wurde abgebrochen. */
    ABORTED
}