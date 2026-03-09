package de.starima.pfw.base.processor.request;

import de.starima.pfw.base.util.LogOutputHelper;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.request.api.IRequestDispatcherProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter @Setter
@Processor
public class RequestDispatcherProcessorChain extends AbstractProcessor implements IRequestDispatcherProcessor {
    @ProcessorParameter(value = "jsonRequestDispatcherProcessor, defaultMultipartHttpServletRequestDispatcherProcessor, assetRequestDispatcherProcessor")
    private List<IRequestDispatcherProcessor> dispatcherProcessors;
    @Override
    public boolean isResponsibleForRequest(Object request) {
        return true;
    }

    @Override
    public Object dispatchRequest(Object request) {
        log.info("{}: dispatchRequest ...", getFullBeanId());
        if (!dispatcherProcessors.isEmpty()) {
            for (IRequestDispatcherProcessor dispatcherProcessor : dispatcherProcessors) {
                if (dispatcherProcessor.isResponsibleForRequest(request)) {
                    return dispatcherProcessor.dispatchRequest(request);
                }
            }
        }
        return null;
    }
}