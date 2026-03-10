package de.starima.pfw.base.processor.description.incubator.domain;

import lombok.Getter;

/**
 * Fügt ein Element in eine Collection im Descriptor-Graph ein.
 *
 * <p>Beispiel:
 * <pre>
 *   new InsertListItemPatch("processors", -1, null)          // leerer Placeholder am Ende
 *   new InsertListItemPatch("processors", 0, "csvReader")    // konkreter Wert an Position 0
 * </pre>
 */
@Getter
public class InsertListItemPatch extends DescriptorPatch {

    /** Position, an der das Element eingefügt wird. {@code -1} = am Ende. */
    private final int index;

    /** Rohwert des neuen Elements. {@code null} = leerer Placeholder. */
    private final Object itemRawValue;

    public InsertListItemPatch(String targetPath, int index, Object itemRawValue) {
        super(targetPath);
        this.index = index;
        this.itemRawValue = itemRawValue;
    }
}