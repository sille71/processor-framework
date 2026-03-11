package de.starima.pfw.base.processor.description.incubator.ai.domain;

import de.starima.pfw.base.processor.description.incubator.domain.DescriptorPatch;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Antwort des KI-Assistenten auf eine Nutzernachricht.
 *
 * <p>Enthält die natürlichsprachliche Antwort, den Antworttyp (für das UI),
 * einen optionalen Konfigurations-Entwurf und vorgeschlagene Aktionen.
 */
@Getter
@Builder
public class AIResponse {

    /** Natürlichsprachliche Antwort für den Nutzer. */
    String message;

    /** Typ der Antwort — steuert das UI-Verhalten. */
    AIResponseType type;

    /**
     * Optionaler Konfigurations-Entwurf.
     *
     * <p>Gesetzt wenn {@code type == DRAFT_READY} oder {@code REFINEMENT_APPLIED}.
     */
    BlueprintDraft draft;

    /**
     * Vorgeschlagene Aktionen für das UI.
     *
     * <p>Beispiel: ["Batch-Size anpassen", "E-Mail-Benachrichtigung aktivieren"].
     */
    List<String> suggestedActions;

    /**
     * Vorgeschlagene Patches für die Edit-Session.
     *
     * <p>Die KI kann automatisch Patches vorschlagen, die der Nutzer
     * per Knopfdruck übernehmen kann.
     */
    List<DescriptorPatch> autoPatches;
}