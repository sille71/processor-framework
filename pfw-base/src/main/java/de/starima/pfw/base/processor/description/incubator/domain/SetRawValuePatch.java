package de.starima.pfw.base.processor.description.incubator.domain;

import lombok.Getter;

/**
 * Setzt den Rohwert eines Scalar-Parameters im Descriptor-Graph.
 *
 * <p>Typischer Einsatz: Das UI gibt einen neuen Wert ein (String, Zahl, Boolean).
 * Der rawValue ist der noch nicht transformierte Eingabewert (vor ValueFunction).
 *
 * <p>Beispiel:
 * <pre>
 *   new SetRawValuePatch("title", "Mein Report")
 *   new SetRawValuePatch("formatProcessor.delimiter", ",")
 *   new SetRawValuePatch("enabled", true)
 * </pre>
 */
@Getter
public class SetRawValuePatch extends DescriptorPatch {

    /** Der neue Rohwert (String, Number, Boolean). */
    private final Object newRawValue;

    public SetRawValuePatch(String targetPath, Object newRawValue) {
        super(targetPath);
        this.newRawValue = newRawValue;
    }
}