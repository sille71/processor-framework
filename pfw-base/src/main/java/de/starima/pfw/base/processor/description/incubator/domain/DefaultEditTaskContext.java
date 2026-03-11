package de.starima.pfw.base.processor.description.incubator.domain;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.domain.DefaultTaskContext;
import de.starima.pfw.base.processor.description.incubator.api.IConstructionManager;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Standard-Implementierung von {@link IEditTaskContext}.
 *
 * <p>Kapselt alle Eingaben für eine EDIT-Session:
 * editTarget, maxDepth (Lazy Loading), pageSize, ConstructionManager.
 */
@Getter
@Setter
@NoArgsConstructor
public class DefaultEditTaskContext extends DefaultTaskContext
        implements IEditTaskContext {

    @ProcessorParameter(description = "Der zu editierende Prozessor oder Descriptor. " +
            "IProcessor → extract für initialen Graph. " +
            "IDescriptorProcessor → direkte Bearbeitung.",
            ignoreInitialization = true)
    private Object editTarget;

    @ProcessorParameter(value = "1",
            description = "Maximale Tiefe für initiales Laden (Lazy Loading). " +
                    "1 = nur Root-Header + Slot-Index.")
    private int maxDepth = 1;

    @ProcessorParameter(value = "20",
            description = "Page-Size für Collections/Maps beim Lazy Loading.")
    private int pageSize = 20;

    @ProcessorParameter(description = "ConstructionManager für diesen Build-Lauf.",
            ignoreInitialization = true)
    private IConstructionManager constructionManager;
}