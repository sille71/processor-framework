package de.starima.pfw.base.processor.description.incubator.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.incubator.domain.IConstructTaskContext;

public interface IConstructSession<T> extends IProcessor {
    T getRoot();
    IConstructTaskContext getContext();
}