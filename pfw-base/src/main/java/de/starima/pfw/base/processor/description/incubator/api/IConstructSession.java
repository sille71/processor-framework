package de.starima.pfw.base.processor.description.incubator.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.incubator.domain.IConstructSource;

public interface IConstructSession<T> extends IProcessor {
    T getRoot();
    IConstructSource getSource();
}