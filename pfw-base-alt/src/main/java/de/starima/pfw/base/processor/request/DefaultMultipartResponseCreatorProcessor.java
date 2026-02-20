package de.starima.pfw.base.processor.request;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.request.api.IResponseProducerProcessor;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

@Slf4j
@Processor
public class DefaultMultipartResponseCreatorProcessor extends AbstractProcessor implements IResponseProducerProcessor {
    @Override
    public boolean isResponsibleForBody(Object body) {
        return body instanceof MultiValueMap;
    }

    @Override
    public boolean isResponsibleForHeaders(MultiValueMap<String, String> headers) {
        return true;
    }

    @Override
    public Object createResponse(Object body, MultiValueMap<String, String> headers) {
        if (body instanceof MultiValueMap) {
            return new ResponseEntity<>(body, ProcessorUtils.createMultipartHeaders(), HttpStatus.OK);
        }
        return ResponseEntity.notFound().build();
    }
}