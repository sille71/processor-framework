package de.starima.pfw.base.processor.request.api;

import de.starima.pfw.base.processor.api.IProcessor;

public interface IResponseHandlerProcessor extends IProcessor {
    public Object handleResponse(Object response);
}