package de.starima.pfw.base.processor.description.incubator.ai.domain;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.annotation.ValueObject;
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
@ValueObject
public class AIMessage {

    /** Autor der Nachricht. */
    public enum Role {
        /** Der menschliche Nutzer. */
        USER,
        /** Der KI-Assistent. */
        ASSISTANT
    }

    /** Autor dieser Nachricht. */
    @ProcessorParameter(description = "Role of the message author (USER or ASSISTANT)")
    Role role;

    /** Nachrichteninhalt in natürlicher Sprache. */
    @ProcessorParameter(description = "Natural language content of the message")
    String content;

    /** Zeitstempel der Nachricht. */
    @ProcessorParameter(description = "Timestamp when the message was created")
    @Builder.Default
    Instant timestamp = Instant.now();
}