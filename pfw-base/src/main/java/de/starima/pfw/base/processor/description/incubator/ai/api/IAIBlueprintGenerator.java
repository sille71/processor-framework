package de.starima.pfw.base.processor.description.incubator.ai.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.incubator.ai.domain.AIIntention;
import de.starima.pfw.base.processor.description.incubator.ai.domain.BlueprintDraft;

/**
 * Generiert eine beanParameterMap aus einer strukturierten Intention.
 *
 * <p>Nutzt den Prozessor-Katalog als Wissensbasis und die KI als Planungsengine.
 *
 * <p><b>Biologische Analogie:</b> Der Genetiker, der aus einer
 * Anforderungsbeschreibung eine DNA-Sequenz zusammenstellt.
 *
 * <p>Kategorien: {@code ai, planning}<br>
 * Tags: {@code generate, blueprint, beanParameterMap, intention}
 */
public interface IAIBlueprintGenerator extends IProcessor {

    /**
     * Generiert einen Konfigurationsentwurf aus einer strukturierten Intention.
     *
     * @param intention Die aufgelöste Nutzeranforderung (aus {@link IAIIntentResolver})
     * @param catalog   Der Prozessor-Katalog als Wissensbasis
     * @return Ein Entwurf mit beanParameterMap + Erklärungen + offenen Fragen
     */
    BlueprintDraft generateDraft(AIIntention intention, IProcessorCatalog catalog);

    /**
     * Verfeinert einen bestehenden Entwurf basierend auf Nutzer-Feedback.
     *
     * @param currentDraft Der aktuelle Entwurf
     * @param feedback     Natürlichsprachliches Feedback ("Mach den Batch größer")
     * @return Der verfeinerte Entwurf
     */
    BlueprintDraft refineDraft(BlueprintDraft currentDraft, String feedback);
}