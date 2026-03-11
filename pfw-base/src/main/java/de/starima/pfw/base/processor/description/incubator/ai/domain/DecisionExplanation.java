package de.starima.pfw.base.processor.description.incubator.ai.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Erklärung einer einzelnen Konfigurations-Entscheidung im {@link BlueprintDraft}.
 *
 * <p>Für jeden gesetzten Parameter-Wert kann die KI erklären, warum sie
 * diese Wahl getroffen hat und welche Alternativen möglich gewesen wären.
 */
@Getter
@Builder
public class DecisionExplanation {

    /** Pfad des Parameters (z.B. "reconProcessor.sourceProcessor"). */
    String parameterPath;

    /** Der gewählte Wert (z.B. "csvReaderProcessor:reader1"). */
    String chosenValue;

    /** Begründung der Wahl (z.B. "CSV-Reader gewählt, weil Nutzer CSV-Dateien erwähnt hat"). */
    String reasoning;

    /** Verworfene Alternativen (z.B. ["xmlReaderProcessor", "jsonReaderProcessor"]). */
    List<String> alternatives;
}