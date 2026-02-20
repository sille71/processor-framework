package de.starima.pfw.base.processor.request.api;

import de.starima.pfw.base.processor.api.IProcessor;

public interface IResponseInterceptorProcessor extends IProcessor {
    public void onBeforeResponse(Object draftResponse);
}