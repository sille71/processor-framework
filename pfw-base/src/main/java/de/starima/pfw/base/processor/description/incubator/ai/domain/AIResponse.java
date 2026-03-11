package de.starima.pfw.base.processor.description.incubator.ai.domain;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.annotation.ValueObject;
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
@ValueObject
public class AIResponse {

    /** Natürlichsprachliche Antwort für den Nutzer. */
    @ProcessorParameter(description = "Natural language response text shown to the user")
    String message;

    /** Typ der Antwort — steuert das UI-Verhalten. */
    @ProcessorParameter(description = "Response type controlling UI behaviour")
    AIResponseType type;

    /**
     * Optionaler Konfigurations-Entwurf.
     *
     * <p>Gesetzt wenn {@code type == DRAFT_READY} oder {@code REFINEMENT_APPLIED}.
     */
    @ProcessorParameter(description = "Generated configuration draft, present when type is DRAFT_READY or REFINEMENT_APPLIED")
    BlueprintDraft draft;

    /**
     * Vorgeschlagene Aktionen für das UI.
     *
     * <p>Beispiel: ["Batch-Size anpassen", "E-Mail-Benachrichtigung aktivieren"].
     */
    @ProcessorParameter(description = "Suggested follow-up actions offered to the user in the UI")
    List<String> suggestedActions;

    /**
     * Vorgeschlagene Patches für die Edit-Session.
     *
     * <p>Die KI kann automatisch Patches vorschlagen, die der Nutzer
     * per Knopfdruck übernehmen kann.
     */
    @ProcessorParameter(description = "Automatically proposed descriptor patches the user can apply with one click")
    List<DescriptorPatch> autoPatches;
}