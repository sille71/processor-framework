package de.starima.pfw.base.processor.request.api;

import de.starima.pfw.base.processor.api.IProcessor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.Map;

/**
 * ReprÃ¤sentiert den eigentlichen Endpunkt, der ein Responseobjekt erzeugt.
 */
public interface IRequestProcessor extends IProcessor {
    /**
     * Wird fÃ¼r Requests verwendet, deren Body lediglich die beanParameterMap des RequestProcessors enthÃ¤lt (also keine weiteren Dateien/ Multiparts)
     *
     * @return - reine beanParameterMap
     */
    public Map<String, Map<String, Object>> processRequest();

    /**
     * Wird fÃ¼r Requests verwendet, deren Body neben der beanParameterMap des RequestProcessors noch weitere Dateien enthÃ¤lt, geeignet fÃ¼r Upload.
     *
     * @return - reine beanParameterMap
     */
    public Map<String, Map<String, Object>> processMultipartRequestWithJsonResponse(MultipartHttpServletRequest request);

    /**
     * Wird fÃ¼r Requests verwendet, deren Body neben der beanParameterMap des RequestProcessors noch weitere Dateien enthÃ¤lt, geeignet fÃ¼r Upload und Download.
     *
     * @return - Multipart Response mit beanParameterMap und Dateien (Download)
     */
    public MultiValueMap<String, Object> processMultipartRequestWithMultiPartResponse(MultipartHttpServletRequest request);

    //TODO: wird spÃ¤ter entfernt und durch processRequest abgedeckt
    public Map<String, Map<String, Object>> processMultipartRequest(Object requestPart);
}