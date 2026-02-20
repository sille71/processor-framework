package de.starima.pfw.base.processor.request;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.request.api.IResponseProducerProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

@Slf4j
@Processor
public class DownloadXMLResponseCreatorProcessor extends AbstractProcessor implements IResponseProducerProcessor {
    @Override
    public boolean isResponsibleForBody(Object body) {
        return true;
    }

    @Override
    public boolean isResponsibleForHeaders(MultiValueMap<String, String> headers) {
        return true;
    }

    @Override
    public Object createResponse(Object body, MultiValueMap<String, String> headers) {
        if (body instanceof byte[]) {

            return new ResponseEntity<>(body, headers, HttpStatus.OK);
        }
        return ResponseEntity.notFound().build();
    }
}