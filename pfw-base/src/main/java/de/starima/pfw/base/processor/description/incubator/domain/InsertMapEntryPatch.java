package de.starima.pfw.base.processor.description.incubator.domain;

import lombok.Getter;

/**
 * Fügt einen Entry in eine Map im Descriptor-Graph ein.
 *
 * <p>Beispiel:
 * <pre>
 *   new InsertMapEntryPatch("mappings", "source", "csvReader")
 *   new InsertMapEntryPatch("mappings", "target", null)  // leerer Placeholder
 * </pre>
 */
@Getter
public class InsertMapEntryPatch extends DescriptorPatch {

    /** Key des neuen Map-Entries. */
    private final Object key;

    /** Value des neuen Map-Entries. {@code null} = leerer Placeholder. */
    private final Object value;

    public InsertMapEntryPatch(String targetPath, Object key, Object value) {
        super(targetPath);
        this.key = key;
        this.value = value;
    }
}