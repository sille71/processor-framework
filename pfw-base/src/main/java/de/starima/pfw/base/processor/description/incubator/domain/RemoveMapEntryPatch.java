package de.starima.pfw.base.processor.description.incubator.domain;

import lombok.Getter;

/**
 * Entfernt einen Entry aus einer Map im Descriptor-Graph.
 *
 * <p>Beispiel:
 * <pre>
 *   new RemoveMapEntryPatch("mappings", "source")
 * </pre>
 */
@Getter
public class RemoveMapEntryPatch extends DescriptorPatch {

    /** Key des zu entfernenden Map-Entries. */
    private final Object key;

    public RemoveMapEntryPatch(String targetPath, Object key) {
        super(targetPath);
        this.key = key;
    }
}