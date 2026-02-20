package de.starima.pfw.base.processor.request;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.request.api.IRequestConsumerProcessor;
import de.starima.pfw.base.processor.request.api.IRequestDispatcherProcessor;
import de.starima.pfw.base.processor.request.domain.AssetHttpRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Processor
public class AssetRequestDispatcherProcessor extends AbstractProcessor implements IRequestDispatcherProcessor {

    @Override
    public boolean isResponsibleForRequest(Object request) {
        return request instanceof AssetHttpRequest;
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
            AssetHttpRequest assetHttpRequest = (AssetHttpRequest) request;
            log.info("{}: dispatch asset request with asset processor {} and assetName {}",getFullBeanId() ,assetHttpRequest.getAssetProcessorId(), assetHttpRequest.getAssetName());

            log.info("{}: 1. create  processor", assetHttpRequest.getAssetProcessorId());
            IRequestConsumerProcessor requestProcessor = createProcessor(IRequestConsumerProcessor.class,
                    assetHttpRequest.getAssetProcessorId(), null, null);
            //TODO: soll der requestProcessor in den Kontext gehangen werden (zumindest fÃ¼r die Zeit, die er gebraucht wird (also innerhalb dieser Methode)?

            log.info("found asset request processor {}", requestProcessor != null ? requestProcessor.getFullBeanId() : null);

            //log.info("{}: 2. validate / logging ", getFullBeanId());

            //TODO: Exception handling
            log.info("{}: 3. process request ", getFullBeanId());
            return requestProcessor != null ? requestProcessor.processRequest(assetHttpRequest) : null;
        }
        log.warn("request is not responsible for request");
        return Map.of();
    }
}