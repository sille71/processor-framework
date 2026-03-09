package de.starima.pfw.base.processor.description.incubator.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfig;
import de.starima.pfw.base.processor.description.incubator.domain.IBuildTaskContext;

public interface ITypeIntrospector extends IProcessor {
    boolean isResponsibleFor(IBuildTaskContext ctx);
    IDescriptorConfig resolve(IBuildTaskContext ctx);
}