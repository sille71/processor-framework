package de.starima.pfw.base.processor.description.incubator.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.incubator.domain.DescriptorPatch;
import de.starima.pfw.base.processor.description.incubator.domain.ExpandPolicy;
import de.starima.pfw.base.processor.description.incubator.domain.ExpandResult;
import de.starima.pfw.base.processor.description.incubator.domain.IEditTaskContext;
import de.starima.pfw.base.processor.description.incubator.domain.PatchResult;

/**
 * Langlebiger Workspace für UI-Editing und Lazy Loading.
 *
 * <p><b>Lebenszyklus:</b>
 * <pre>
 *   startEdit() → [expand/patch]* → exportBeanParameterMap() → close()
 * </pre>
 *
 * <p><b>Biologische Analogie:</b> Der gesamte Entwicklungsprozess von der Zygote
 * bis zum fertigen Organismus. Das UI kann jederzeit den aktuellen Zustand
 * inspizieren und Signale senden (Patches).
 *
 * <p>Der Workspace hält den partiellen Descriptor-Graph mit Placeholder-Knoten.
 * Das UI expandiert Placeholders bei Bedarf (Lazy Loading) und sendet Patches
 * für Änderungen.
 */
public interface IEditSession extends IProcessor {

    /** Eindeutige Session-ID (UUID). */
    String getSessionId();

    /** Task-Context der Session (für spätere Erweiterungen). */
    IEditTaskContext getContext();

    /** Der Descriptor-Workspace mit dem partiellen Descriptor-Graph. */
    IDescriptorWorkspace getWorkspace();

    /**
     * Expandiert einen Placeholder-Knoten — ersetzt ihn durch den vollständigen Descriptor.
     *
     * <p>Das Ergebnis enthält den expandierten Knoten und neue Kinder-Placeholders.
     *
     * <p><b>Biologische Analogie:</b> Eine Stammzelle differenziert sich.
     *
     * @param path   Pfad des zu expandierenden Placeholders
     * @param policy Steuerung der Expansion (maxDepth, stages, pageSize)
     * @return Expandierter Knoten + neue Kinder-Placeholders
     */
    ExpandResult expand(String path, ExpandPolicy policy);

    /**
     * Wendet eine Änderung auf den Descriptor-Graph an.
     *
     * <p>Unterstützte Patches: {@link de.starima.pfw.base.processor.description.incubator.domain.SetRawValuePatch},
     * {@link de.starima.pfw.base.processor.description.incubator.domain.SelectCandidatePatch},
     * {@link de.starima.pfw.base.processor.description.incubator.domain.InsertListItemPatch},
     * {@link de.starima.pfw.base.processor.description.incubator.domain.RemoveListItemPatch},
     * {@link de.starima.pfw.base.processor.description.incubator.domain.InsertMapEntryPatch},
     * {@link de.starima.pfw.base.processor.description.incubator.domain.RemoveMapEntryPatch}.
     *
     * <p><b>Biologische Analogie:</b> Ein epigenetisches Signal verändert die Genexpression.
     *
     * @param patch Die Änderung
     * @return Erfolgs-Status, betroffener Knoten, Nebeneffekte
     */
    PatchResult applyPatch(DescriptorPatch patch);
}