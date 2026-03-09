package de.starima.pfw.base.processor.description.incubator.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.incubator.domain.IBuildTaskContext;

public interface IDescriptorResolver extends IProcessor {
    boolean isResponsibleFor(IBuildTaskContext ctx);
    IDescriptorProcessor resolve(IBuildTaskContext ctx);
}