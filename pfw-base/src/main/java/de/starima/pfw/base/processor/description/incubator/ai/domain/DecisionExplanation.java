package de.starima.pfw.base.processor.description.incubator.ai.domain;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.annotation.ValueObject;
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
@ValueObject
public class DecisionExplanation {

    /** Pfad des Parameters (z.B. "reconProcessor.sourceProcessor"). */
    @ProcessorParameter(description = "Dot-separated path to the parameter this decision concerns")
    String parameterPath;

    /** Der gewählte Wert (z.B. "csvReaderProcessor:reader1"). */
    @ProcessorParameter(description = "The value chosen by the AI for this parameter")
    String chosenValue;

    /** Begründung der Wahl (z.B. "CSV-Reader gewählt, weil Nutzer CSV-Dateien erwähnt hat"). */
    @ProcessorParameter(description = "Natural language reasoning explaining why this value was chosen")
    String reasoning;

    /** Verworfene Alternativen (z.B. ["xmlReaderProcessor", "jsonReaderProcessor"]). */
    @ProcessorParameter(description = "Alternative values that were considered but not chosen")
    List<String> alternatives;
}