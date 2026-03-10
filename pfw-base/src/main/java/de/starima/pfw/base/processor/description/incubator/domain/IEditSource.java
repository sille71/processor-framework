package de.starima.pfw.base.processor.description.incubator.domain;

/**
 * Quell-Informationen für eine Edit-Session.
 *
 * <p>Enthält das zu editierende Objekt (typischerweise ein IDescriptorProcessor
 * oder IProcessor, dessen Descriptor editiert werden soll).
 */
public interface IEditSource {

    /**
     * Das zu editierende Objekt.
     *
     * <p>Kann sein:
     * <ul>
     *   <li>Ein {@link de.starima.pfw.base.processor.description.api.IDescriptorProcessor} (direkte Bearbeitung)</li>
     *   <li>Ein {@link de.starima.pfw.base.processor.api.IProcessor} (dessen Descriptor wird generiert)</li>
     * </ul>
     */
    Object getObject();
}