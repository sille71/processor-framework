package de.starima.pfw.base.processor.description.incubator.ai.domain;

import de.starima.pfw.base.processor.description.incubator.api.IEditSession;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Eine laufende Konversations-Session zwischen Nutzer und KI-Assistent.
 *
 * <p>Hält die Nachrichtenhistorie, den aktuellen Konfigurations-Entwurf
 * und die Verknüpfung zur Edit-Session (Descriptor-Graph im Workspace).
 *
 * <h3>Lebenszyklus</h3>
 * <pre>
 *   IAIConversationProcessor.startConversation()
 *     → GATHERING_REQUIREMENTS
 *     → REVIEWING_DRAFT (erster Entwurf + Edit-Session)
 *     → CONFIRMED
 *     → COMPLETED (incubator.startConstruct → lebendiger Prozess)
 * </pre>
 */
@Getter
@Setter
@Builder
public class AIConversationSession {

    /** Eindeutige Session-ID (UUID). */
    String sessionId;

    /**
     * Verknüpfte Edit-Session (Descriptor-Graph im Workspace).
     *
     * <p>Wird erstellt, sobald ein erster Entwurf vorliegt (State: REVIEWING_DRAFT).
     * {@code null} solange noch keine Edit-Session existiert.
     */
    IEditSession editSession;

    /** Nachrichtenhistorie (USER + ASSISTANT abwechselnd). */
    @Builder.Default
    List<AIMessage> history = new ArrayList<>();

    /** Der aktuelle Konfigurations-Entwurf. {@code null} am Anfang. */
    BlueprintDraft currentDraft;

    /** Aktueller Zustand des Konversations-Automaten. */
    @Builder.Default
    ConversationState state = ConversationState.GATHERING_REQUIREMENTS;

    /**
     * Fügt eine Nutzernachricht zur History hinzu.
     */
    public void addUserMessage(String content) {
        if (history == null) history = new ArrayList<>();
        history.add(AIMessage.builder()
                .role(AIMessage.Role.USER)
                .content(content)
                .build());
    }

    /**
     * Fügt eine Assistenten-Nachricht zur History hinzu.
     */
    public void addAssistantMessage(String content) {
        if (history == null) history = new ArrayList<>();
        history.add(AIMessage.builder()
                .role(AIMessage.Role.ASSISTANT)
                .content(content)
                .build());
    }
}