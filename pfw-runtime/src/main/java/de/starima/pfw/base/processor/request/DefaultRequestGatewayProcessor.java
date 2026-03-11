package de.starima.pfw.base.processor.request;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.request.api.IRequestDispatcherProcessor;
import de.starima.pfw.base.processor.request.api.IRequestGatewayProcessor;
import de.starima.pfw.base.processor.request.api.IResponseDispatcherProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Standard-Gateway mit Dispatcher-Chains.
 *
 * <p>Empfängt Requests, dispatcht sie über den RequestDispatcher
 * und leitet die Response über den ResponseDispatcher weiter.
 *
 * <p>Konfigurierbar über beanParameterMap — kein Hardcode.
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Standard-Gateway mit Dispatcher-Chains. " +
                "Empfängt Requests und routet sie an den passenden Dispatcher.",
        categories = {"request", "gateway"},
        tags = {"gateway", "dispatcher", "request", "response"}
)
public class DefaultRequestGatewayProcessor extends AbstractProcessor
        implements IRequestGatewayProcessor {

    @ProcessorParameter(value = "requestDispatcherProcessorChain",
            description = "Eingehende Requests werden an den passenden Dispatcher geleitet.")
    private IRequestDispatcherProcessor requestDispatcher;

    @ProcessorParameter(
            description = "Ausgehende Responses werden an den passenden Dispatcher geleitet.")
    private IResponseDispatcherProcessor responseDispatcher;

    @Override
    public Object processRequest(Object request) {
        if (requestDispatcher == null) {
            log.warn("DefaultRequestGatewayProcessor: kein requestDispatcher konfiguriert");
            return null;
        }
        log.debug("DefaultRequestGatewayProcessor: processing request {}", request != null ? request.getClass().getSimpleName() : "null");
        Object response = requestDispatcher.dispatchRequest(request);
        if (responseDispatcher != null && response != null) {
            response = responseDispatcher.dispatchResponse(response);
        }
        return response;
    }
}