package de.starima.pfw.base.processor.description.incubator.api;

import de.starima.pfw.base.processor.api.IProcessor;

public interface IIncubatorSession<T> extends IProcessor {
    T getRoot();
}