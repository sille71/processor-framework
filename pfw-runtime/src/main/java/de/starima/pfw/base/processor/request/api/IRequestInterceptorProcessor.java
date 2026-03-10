package de.starima.pfw.base.processor.request.api;

import de.starima.pfw.base.processor.api.IProcessor;

public interface IRequestInterceptorProcessor extends IProcessor {
    public void onBeforeRequest(Object draftRequest);

    //
    //public void onAfterRequest(Object draftRequest);
}