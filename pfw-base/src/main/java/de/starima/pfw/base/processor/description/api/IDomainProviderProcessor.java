package de.starima.pfw.base.processor.description.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.set.api.ISetProcessor;

public interface IDomainProviderProcessor<I> extends IProcessor {
    public ISetProcessor getDomainProcessorForItemReference(String itemReference);
    public ISetProcessor getDomainProcessorForItem(I item);
}