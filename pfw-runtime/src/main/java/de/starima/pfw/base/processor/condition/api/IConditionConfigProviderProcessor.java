package de.starima.pfw.base.processor.condition.api;

import de.starima.pfw.base.processor.api.IProcessor;

public interface IConditionConfigProviderProcessor<C> extends IProcessor {
    public C getConditionConfig();
}