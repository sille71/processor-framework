package de.starima.pfw.base.processor.request;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.request.api.IRequestConsumerProcessor;
import de.starima.pfw.base.processor.request.api.IRequestDispatcherProcessor;
import de.starima.pfw.base.util.LogOutputHelper;
import de.starima.pfw.base.util.MapUtils;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Processor(description = "Ist fÃ¼r die Verarbeitung von MultipartHttpServletRequests verantwortlich. In Spring muss dazu der MultipartResolver aktiv sein!")
public class DefaultMultipartHttpServletRequestDispatcherProcessor extends AbstractProcessor implements IRequestDispatcherProcessor {

    @Override
    public boolean isResponsibleForRequest(Object request) {
        return request instanceof MultipartHttpServletRequest;
    }

    @Override
    public Object dispatchRequest(Object request) {
        if (isResponsibleForRequest(request)) {
            MultipartHttpServletRequest req = (MultipartHttpServletRequest) request;
            log.info("{}: dispatchRequest ...", getFullBeanId());
            String requestProcessorIdentifier = req.getParameter(ProcessorUtils.key_requestProcessorIdentifier);
            Map<String,Map<String, Object>> beanParameterMap = null;
            try {
                beanParameterMap = MapUtils.getBeanParameterMapFromJson(req.getFile(ProcessorUtils.key_beanParameterMap).getBytes());
            } catch (IOException e) {
                log.warn("{}: Could not read bean parameter map from request.", getFullBeanId());
            }
            //es muss nicht notwendigerweise eine Map mitgeliefert werden
            if (beanParameterMap == null) beanParameterMap = new LinkedHashMap<>();

            if (requestProcessorIdentifier != null && beanParameterMap.get(requestProcessorIdentifier) == null) {
                beanParameterMap.put(requestProcessorIdentifier, new LinkedHashMap<>());
            }

            //optional kÃ¶nnen alle MultiPartFiles als Parameter mitgegeben werden, damit stehen sie schon bei der Prozessorinitialisierung des requestProcessor zur VerfÃ¼gung, falls nÃ¶tig
            beanParameterMap.get(ProcessorUtils.getIdentifier(requestProcessorIdentifier)).putAll(req.getFileMap());

            log.info("{}: call processRequest with request processor {} and beanParameterMap data {}", getFullBeanId() ,requestProcessorIdentifier, LogOutputHelper.toLogString(beanParameterMap));

            //wir versuchen zunÃ¤chst den request Prozessor aus dem requestProcessorIdentifier zu erzeugen
            IRequestConsumerProcessor requestConsumerProcessor = createProcessor(IRequestConsumerProcessor.class,
                    requestProcessorIdentifier, null, Collections.singletonList(beanParameterMap));
            //IRequestProcessor requestProcessor = createProcessor(IRequestProcessor.class,
                    //requestProcessorIdentifier, null, Collections.singletonList(beanParameterMap));
            //falls keiner gefunden wurde schauen wir nach dem typ requestProcessor
            if (requestConsumerProcessor == null) {
                requestConsumerProcessor = createProcessor(IRequestConsumerProcessor.class,
                        null, "requestProcessor", Collections.singletonList(beanParameterMap));
            }

            //TODO: soll der requestProcessor in den Kontext gehangen werden (zumindest fÃ¼r die Zeit, die er gebraucht wird (also innerhalb dieser Methode)?
            //Wie kann dieser Prozessor in der Dokumentation erfasst werden? Muss er als Parameter hinterlegt werden? Oder scannt die Doku auch den Kontext?
            //LÃ¶sung: die Instanz bekommt als Parameter die Menge aller mÃ¶glichen RequestProcessor Prototypeidentifier
            log.info("found request processor {}", requestConsumerProcessor != null ? requestConsumerProcessor.getIdentifier() : null);

            //TODO: Exception handling
            return requestConsumerProcessor != null ? requestConsumerProcessor.processRequest(req) : null;
        }
        log.warn("request is not responsible for request");
        return Map.of();
    }
}