package de.starima.pfw.base.processor.description.incubator.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hält den aktuellen (partiellen) Descriptor-Graph während einer Edit-Session.
 *
 * <p>Der Workspace verwaltet den inkrementell aufgebauten Descriptor-Graph.
 * Teile des Graphs können {@link IPlaceholderDescriptor}s sein — Knoten,
 * die noch nicht vollständig geladen wurden.
 *
 * <p><b>Biologische Analogie:</b> Der Brutkasten, in dem der Embryo heranwächst.
 * Teile des Organismus sind schon entwickelt (vollständige Descriptoren),
 * andere noch Stammzellen (Placeholders).
 *
 * <p><b>Lebenszyklus:</b>
 * <ol>
 *   <li>startEdit() → Workspace mit Root-Descriptor (ROOT_HEADER) + Slot-Placeholders</li>
 *   <li>expand() → Placeholder → vollständiger Descriptor</li>
 *   <li>applyPatch() → Descriptor-Werte ändern</li>
 *   <li>exportBeanParameterMap() → aktuellen Zustand exportieren</li>
 * </ol>
 */
public interface IDescriptorWorkspace extends IProcessor {

    /** Root-Descriptor des Workspace (typischerweise ein ProcessorDescriptor). */
    IDescriptorProcessor getRoot();

    /**
     * Findet einen Descriptor-Knoten anhand seines Pfades.
     *
     * @param path Pfad des Knotens (z.B. "formatProcessor.delimiter")
     * @return Der Knoten, falls vorhanden
     */
    Optional<IDescriptorProcessor> find(String path);

    /**
     * Ersetzt einen Knoten im Descriptor-Graph.
     *
     * <p>Typischer Einsatz: Placeholder → vollständiger Descriptor nach expand().
     *
     * @param path    Pfad des zu ersetzenden Knotens
     * @param newNode Der neue Knoten
     */
    void replace(String path, IDescriptorProcessor newNode);

    /**
     * Alle aktuell vorhandenen Placeholders im Graph.
     *
     * <p>Das UI kann diese auflisten und gezielt nachladen (Lazy Loading).
     * Nach einem expand() verkleinert sich diese Liste — neue Kinder-Placeholders
     * können hinzukommen.
     */
    List<IPlaceholderDescriptor> getPlaceholders();

    /**
     * Exportiert den aktuellen Workspace-Zustand als beanParameterMap.
     *
     * <p>Nutzt intern den extract-Pfad der InstanceProviderChain.
     * Das Ergebnis kann direkt an {@link de.starima.pfw.base.processor.description.incubator.api.IIncubator#startConstruct} übergeben werden.
     *
     * @return Vollständige beanParameterMap des aktuellen Zustands
     */
    Map<String, Map<String, Object>> exportBeanParameterMap();
}