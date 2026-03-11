package de.starima.pfw.base.processor.description.incubator.ai.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.description.incubator.ai.domain.AIConversationSession;
import de.starima.pfw.base.processor.description.incubator.ai.domain.AIResponse;

/**
 * Verwaltet einen natürlichsprachlichen Dialog zwischen Fachanwender und Framework.
 *
 * <p>Stellt Rückfragen, bestätigt Annahmen und führt den Nutzer durch die
 * Konfiguration. Koordiniert das Zusammenspiel von {@link IAIIntentResolver},
 * {@link IAIBlueprintGenerator}, {@link IAIConfigValidator} und {@link IProcessorCatalog}.
 *
 * <p><b>Biologische Analogie:</b> Der Arzt, der mit dem Patienten spricht,
 * um die richtige Therapie (Konfiguration) zu finden.
 *
 * <p>Kategorien: {@code ai, conversation}<br>
 * Tags: {@code dialog, chat, naturalLanguage, guidance}
 */
public interface IAIConversationProcessor extends IProcessor {

    /**
     * Startet eine neue Konversation in einem gegebenen Kontext.
     *
     * @param context Der Prozessor-Kontext (für Zugriff auf Katalog, Incubator etc.)
     * @return Eine neue Konversations-Session im Zustand GATHERING_REQUIREMENTS
     */
    AIConversationSession startConversation(IProcessorContext context);

    /**
     * Verarbeitet eine Nutzernachricht und liefert die Antwort.
     *
     * <p>Koordiniert intern: IntentResolver → BlueprintGenerator → ConfigValidator
     * → EditSession (falls Entwurf bereit ist).
     *
     * @param session     Die aktive Konversations-Session
     * @param userMessage Die Nutzernachricht
     * @return Die KI-Antwort mit optionalem Entwurf und vorgeschlagenen Aktionen
     */
    AIResponse processMessage(AIConversationSession session, String userMessage);
}