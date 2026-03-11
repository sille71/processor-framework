package de.starima.pfw.base.processor.description.incubator.domain;

/**
 * TaskContext für EDIT (Langlebiger Workspace + Lazy Loading + Patches).
 *
 * <p>Ersetzt {@code IEditSource} + {@code IEditPolicy}.
 * Enthält alle Eingaben, die der Incubator für eine Edit-Session benötigt.
 */
public interface IEditTaskContext extends IBuildTaskContext {

    @Override
    default BuildMode getMode() { return BuildMode.EDIT; }

    /**
     * Der zu editierende Prozessor oder Descriptor.
     * <ul>
     *   <li>Lebendiger IProcessor → extract für initialen Graph</li>
     *   <li>Bestehender IDescriptorProcessor → direkte Bearbeitung</li>
     * </ul>
     */
    Object getEditTarget();

    /**
     * Maximale Tiefe für initiales Laden (Lazy Loading).
     * <p>Default 1 = nur Root-Header + Slot-Index.
     */
    @Override
    default int getMaxDepth() { return 1; }

    /**
     * Page-Size für Collections/Maps beim Lazy Loading.
     */
    default int getPageSize() { return 20; }
}