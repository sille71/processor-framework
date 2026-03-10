package de.starima.pfw.base.processor.description.incubator.domain;

import lombok.Getter;

/**
 * Trifft eine polymorphe Auswahl im Descriptor-Graph.
 *
 * <p>Wird angewendet, wenn ein Parameter polymorphen Typ hat (z.B. {@code IProcessor})
 * und das UI aus den möglichen Kandidaten (getPossibleValueDescriptors()) eine
 * konkrete Implementierung auswählt.
 *
 * <p>Nach dem Patch wird ein SUBTREE-Placeholder für den gewählten Typ erstellt,
 * den das UI per expand() nachladen kann.
 *
 * <p>Beispiel:
 * <pre>
 *   new SelectCandidatePatch("formatProcessor", "csvFormatterProcessor")
 * </pre>
 */
@Getter
public class SelectCandidatePatch extends DescriptorPatch {

    /** prototypeId des gewählten Prozessor-Typs (z.B. "csvFormatterProcessor"). */
    private final String selectedPrototypeId;

    public SelectCandidatePatch(String targetPath, String selectedPrototypeId) {
        super(targetPath);
        this.selectedPrototypeId = selectedPrototypeId;
    }
}