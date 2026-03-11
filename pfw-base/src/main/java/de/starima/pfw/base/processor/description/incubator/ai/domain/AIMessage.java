package de.starima.pfw.base.processor.description.incubator.ai.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Eine einzelne Nachricht in einer {@link AIConversationSession}.
 *
 * <p>Kapselt Autor (USER oder ASSISTANT), Inhalt und Zeitstempel.
 */
@Getter
@Builder
public class AIMessage {

    /** Autor der Nachricht. */
    public enum Role {
        /** Der menschliche Nutzer. */
        USER,
        /** Der KI-Assistent. */
        ASSISTANT
    }

    /** Autor dieser Nachricht. */
    Role role;

    /** Nachrichteninhalt in natürlicher Sprache. */
    String content;

    /** Zeitstempel der Nachricht. */
    @Builder.Default
    Instant timestamp = Instant.now();
}