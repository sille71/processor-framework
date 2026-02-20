package de.starima.pfw.base.processor.description.incubator.api;

import de.starima.pfw.base.processor.api.IProcessor;

public interface IInstanceProvider extends IProcessor {
    boolean isResponsibleFor(IInstanceCreationContext context);
    Object provide(IInstanceCreationContext context);
}