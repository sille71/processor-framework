package de.starima.pfw.base.processor.request;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.request.api.IRequestProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.Map;

@Getter @Setter
@Slf4j
@Processor
public class DescriptionRequestProcessor extends AbstractProcessor implements IRequestProcessor {
    @ProcessorParameter
    private String processorIdentifier;
    @ProcessorParameter
    private String contextIdentifier;

    @Override
    public Map<String, Map<String, Object>> processRequest() {
        this.runtimeContext.getProcessor(processorIdentifier);
        return this.extractEffectiveProcessorParameterMap();
    }

    @Override
    public Map<String, Map<String, Object>> processMultipartRequestWithJsonResponse(MultipartHttpServletRequest request) {
        return Map.of();
    }

    @Override
    public MultiValueMap<String, Object> processMultipartRequestWithMultiPartResponse(MultipartHttpServletRequest request) {
        return null;
    }

    @Override
    public Map<String, Map<String, Object>> processMultipartRequest(Object requestPart) {
        return Map.of();
    }
}