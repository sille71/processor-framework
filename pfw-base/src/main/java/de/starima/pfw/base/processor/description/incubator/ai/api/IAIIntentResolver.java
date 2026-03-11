package de.starima.pfw.base.processor.description.incubator.ai.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.incubator.ai.domain.AIConversationSession;
import de.starima.pfw.base.processor.description.incubator.ai.domain.AIIntention;

/**
 * Übersetzt natürlichsprachliche Nutzereingaben in strukturierte Intentionen.
 *
 * <p>Extrahiert Entitäten (z.B. Dateipfade, Encodings, Prozessor-Typen),
 * erkennt den Intentionstyp (CONFIGURE_PROCESS, EXPLAIN, MODIFY, QUERY)
 * und gibt eine strukturierte {@link AIIntention} zurück.
 *
 * <p>Kategorien: {@code ai, intent}<br>
 * Tags: {@code nlp, intent, extraction, naturalLanguage}
 */
public interface IAIIntentResolver extends IProcessor {

    /**
     * Löst eine Nutzereingabe ohne Konversationshistorie auf.
     *
     * @param naturalLanguageText Der natürlichsprachliche Text des Nutzers
     * @return Die strukturierte Intention
     */
    AIIntention resolve(String naturalLanguageText);

    /**
     * Löst eine Nutzereingabe im Kontext einer laufenden Konversation auf.
     *
     * <p>Nutzt die Konversationshistorie für kontextsensitive Entitätsextraktion
     * (z.B. "Ja, die von vorhin" → referenziert ein vorher genanntes Objekt).
     *
     * @param naturalLanguageText Der natürlichsprachliche Text des Nutzers
     * @param session             Die aktive Konversations-Session (mit History)
     * @return Die strukturierte Intention mit Kontext-Informationen
     */
    AIIntention resolve(String naturalLanguageText, AIConversationSession session);
}