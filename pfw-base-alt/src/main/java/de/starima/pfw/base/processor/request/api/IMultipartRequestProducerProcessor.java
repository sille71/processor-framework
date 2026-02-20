package de.starima.pfw.base.processor.request.api;

import org.springframework.util.MultiValueMap;

public interface IMultipartRequestProducerProcessor extends IRequestProducerProcessor {
    /**
     * Requesterzeugung
     * Wird fÃ¼r Requests verwendet, deren Body neben der beanParameterMap des RequestProcessors noch weitere Dateien enthÃ¤lt, geeignet fÃ¼r Upload und Download.
     *
     * @return - Multipart Response mit beanParameterMap und Dateien (Download)
     */
    public MultiValueMap<String, Object> createMultipartRequest(Object data);
}