package de.starima.pfw.base.processor.description.incubator.domain;

import lombok.Getter;

/**
 * Entfernt ein Element aus einer Collection im Descriptor-Graph.
 *
 * <p>Beispiel:
 * <pre>
 *   new RemoveListItemPatch("processors", 2)  // Element an Position 2 entfernen
 * </pre>
 */
@Getter
public class RemoveListItemPatch extends DescriptorPatch {

    /** Index des zu entfernenden Elements. */
    private final int index;

    public RemoveListItemPatch(String targetPath, int index) {
        super(targetPath);
        this.index = index;
    }
}