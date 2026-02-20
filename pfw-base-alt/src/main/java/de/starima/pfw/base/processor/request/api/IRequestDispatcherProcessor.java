package de.starima.pfw.base.processor.request.api;

import de.starima.pfw.base.processor.api.IProcessor;

public interface IRequestDispatcherProcessor extends IProcessor {
    public boolean isResponsibleForRequest(Object requestData);
    public Object dispatchRequest(Object requestData);
}