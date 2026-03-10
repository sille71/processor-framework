package de.starima.pfw.base.processor.request.api;

import de.starima.pfw.base.processor.api.IProcessor;

public interface IResponseDispatcherProcessor extends IProcessor {
    public boolean isResponsibleForResponse(Object response);
    public Object dispatchResponse(Object response);
}