package de.starima.pfw.base.processor.request;

import de.starima.pfw.base.util.LogOutputHelper;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.request.api.IRequestDispatcherProcessor;
import de.starima.pfw.base.processor.request.api.IRequestProcessor;
import de.starima.pfw.base.util.MapUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Processor
public class JsonRequestDispatcherProcessor extends AbstractProcessor implements IRequestDispatcherProcessor {

    @Override
    public boolean isResponsibleForRequest(Object request) {
        return MapUtils.isMapOfStringToMapOfStringToObject(request);
    }

    /**
     * MÃ¶glicher Ablauf:
     * 1. Requestprozessor erzeugen (steckt als beanParameterMap im Request)
     * 2. optionale Validierung und logging (ValidatorProcessor)
     * 3. Request durch Requestprozessor verarbeiten
     * 4. Response nachbearbeiten und Errorhandling
     * @param request
     * @return
     */
    @Override
    public Object dispatchRequest(Object request) {
        if (isResponsibleForRequest(request)) {
            Map<String, Map<String, Object>> beanParameterMap = (Map<String, Map<String, Object>>) request;
            log.info("{}: dispatchRequest with data {}",getFullBeanId() ,LogOutputHelper.toLogString(beanParameterMap));

            log.info("{}: 1. create request processor", getFullBeanId());
            IRequestProcessor requestProcessor = createProcessor(IRequestProcessor.class,
                    null, "requestProcessor", Collections.singletonList(beanParameterMap));
            //TODO: soll der requestProcessor in den Kontext gehangen werden (zumindest fÃ¼r die Zeit, die er gebraucht wird (also innerhalb dieser Methode)?
            //Wie kann dieser Prozessor in der Dokumentation erfasst werden? Muss er als Parameter hinterlegt werden? Oder scannt die Docu auch den Kontext?
            log.info("found request processor {}", requestProcessor != null ? requestProcessor.getIdentifier() : null);

            log.info("{}: 2. validate / logging ", getFullBeanId());

            //TODO: Exception handling
            log.info("{}: 3. process request ", getFullBeanId());
            return requestProcessor != null ? requestProcessor.processRequest() : null;

            //log.info("{}: 4. response post processing, error handling ", getFullBeanId());
        }
        log.warn("request is not responsible for request");
        return Map.of();
    }
}