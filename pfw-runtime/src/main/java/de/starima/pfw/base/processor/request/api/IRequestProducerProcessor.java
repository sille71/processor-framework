package de.starima.pfw.base.processor.request.api;

import de.starima.pfw.base.processor.api.IProcessor;

import java.util.Map;

public interface IRequestProducerProcessor extends IProcessor {
    /**
     * Requesterzeugung
     * Erzeugt reine beanParameterMap Request
     *
     * @return - reine beanParameterMap
     */
    public Map<String, Map<String, Object>> createRequest(Object data);
}