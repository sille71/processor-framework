package de.starima.pfw.base.processor.description.config.api;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.api.IProcessor;

@Processor
public interface IDescriptorConfigProvider extends IProcessor {
    boolean isResponsibleFor(IDescriptorConfigCreationContext context);
    IDescriptorConfig provide(IDescriptorConfigCreationContext context);
}