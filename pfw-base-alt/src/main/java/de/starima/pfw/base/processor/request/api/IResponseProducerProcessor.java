package de.starima.pfw.base.processor.request.api;

import de.starima.pfw.base.processor.api.IProcessor;
import org.springframework.util.MultiValueMap;

public interface IResponseProducerProcessor extends IProcessor {
    public boolean isResponsibleForBody(Object body);
    public boolean isResponsibleForHeaders(MultiValueMap<String, String> headers);
    /**
     * Requesterzeugung
     * Erzeugt reine beanParameterMap Request
     *
     * @return - reine beanParameterMap
     */
    public Object createResponse(Object body, MultiValueMap<String, String> headers);
}