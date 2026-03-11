package de.starima.pfw.base.processor.request;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.request.api.IRequestDispatcherProcessor;
import de.starima.pfw.base.processor.request.api.IRequestGatewayProcessor;
import de.starima.pfw.base.processor.request.api.IResponseDispatcherProcessor;
import de.starima.pfw.base.processor.set.api.ISetProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard-Gateway mit Dispatcher-Chains, dynamischer Registrierung und Policy.
 *
 * <p>Request-Chain-of-Responsibility: Der erste zuständige Request-Dispatcher gewinnt.
 * Response-Chain-of-Responsibility: Der erste zuständige Response-Dispatcher gewinnt.
 *
 * <p>Services können Dispatcher zur Laufzeit via
 * {@link #registerRequestDispatcher} / {@link #registerResponseDispatcher} hinzufügen.
 * Die allowedDispatcherSet-Policies bestimmen, welche Dispatcher akzeptiert werden.
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Standard-Gateway mit Request- und Response-Dispatcher-Chain, " +
                "dynamischer Registrierung und Zulassungs-Policy.",
        categories = {"request", "gateway"},
        tags = {"communication", "gateway", "request", "response", "policy"}
)
public class DefaultRequestGatewayProcessor extends AbstractProcessor
        implements IRequestGatewayProcessor {

    @ProcessorParameter(description = "Die statisch konfigurierten Request-Dispatcher. " +
            "Zur Laufzeit können weitere über registerRequestDispatcher() hinzukommen.")
    private List<IRequestDispatcherProcessor> requestDispatchers;

    @ProcessorParameter(description = "Die statisch konfigurierten Response-Dispatcher. " +
            "Zur Laufzeit können weitere über registerResponseDispatcher() hinzukommen.")
    private List<IResponseDispatcherProcessor> responseDispatchers;

    @ProcessorParameter(description = "Erlaubte Request-Dispatcher-PrototypIDs. " +
            "Wenn gesetzt, werden nur Dispatcher akzeptiert, deren PrototypID " +
            "in dieser Menge enthalten ist. Wenn null, werden alle akzeptiert.")
    private ISetProcessor<String> allowedRequestDispatcherSet;

    @ProcessorParameter(description = "Erlaubte Response-Dispatcher-PrototypIDs. " +
            "Wenn gesetzt, werden nur Dispatcher akzeptiert, deren PrototypID " +
            "in dieser Menge enthalten ist. Wenn null, werden alle akzeptiert.")
    private ISetProcessor<String> allowedResponseDispatcherSet;

    @Override
    public Object processRequest(Object request) {
        if (requestDispatchers == null || requestDispatchers.isEmpty()) {
            log.warn("Gateway: keine Request-Dispatcher konfiguriert");
            return null;
        }

        // Request-Chain-of-Responsibility: erster zuständiger Dispatcher gewinnt
        for (IRequestDispatcherProcessor dispatcher : requestDispatchers) {
            if (dispatcher.isResponsibleForRequest(request)) {
                log.debug("Gateway: Request-Dispatcher '{}' übernimmt Request",
                        dispatcher.getFullBeanId());
                Object response = dispatcher.dispatchRequest(request);

                // Response-Chain-of-Responsibility: erster zuständiger Dispatcher gewinnt
                if (response != null && responseDispatchers != null) {
                    for (IResponseDispatcherProcessor responseDispatcher : responseDispatchers) {
                        if (responseDispatcher.isResponsibleForResponse(response)) {
                            log.debug("Gateway: Response-Dispatcher '{}' übernimmt Response",
                                    responseDispatcher.getFullBeanId());
                            response = responseDispatcher.dispatchResponse(response);
                            break;
                        }
                    }
                }
                return response;
            }
        }

        log.info("Gateway: kein Request-Dispatcher zuständig für Request vom Typ {}",
                request != null ? request.getClass().getSimpleName() : "null");
        return null;
    }

    @Override
    public boolean registerRequestDispatcher(IRequestDispatcherProcessor dispatcher) {
        if (dispatcher == null) return false;

        // Policy-Prüfung
        if (allowedRequestDispatcherSet != null) {
            String prototypeId = dispatcher.getProtoTypeIdentifier();
            if (!allowedRequestDispatcherSet.isMember(prototypeId)) {
                log.warn("Gateway: Request-Dispatcher '{}' (prototypeId='{}') nicht in " +
                        "der erlaubten Menge — abgelehnt",
                        dispatcher.getFullBeanId(), prototypeId);
                return false;
            }
        }

        // Duplikat-Prüfung
        if (requestDispatchers == null) {
            requestDispatchers = new ArrayList<>();
        }
        boolean alreadyRegistered = requestDispatchers.stream()
                .anyMatch(d -> d.getFullBeanId() != null
                        && d.getFullBeanId().equals(dispatcher.getFullBeanId()));
        if (alreadyRegistered) {
            log.debug("Gateway: Request-Dispatcher '{}' bereits registriert — übersprungen",
                    dispatcher.getFullBeanId());
            return true;
        }

        requestDispatchers.add(dispatcher);
        log.info("Gateway: Request-Dispatcher '{}' registriert (gesamt: {})",
                dispatcher.getFullBeanId(), requestDispatchers.size());
        return true;
    }

    @Override
    public boolean registerResponseDispatcher(IResponseDispatcherProcessor dispatcher) {
        if (dispatcher == null) return false;

        // Policy-Prüfung
        if (allowedResponseDispatcherSet != null) {
            String prototypeId = dispatcher.getProtoTypeIdentifier();
            if (!allowedResponseDispatcherSet.isMember(prototypeId)) {
                log.warn("Gateway: Response-Dispatcher '{}' (prototypeId='{}') nicht in " +
                        "der erlaubten Menge — abgelehnt",
                        dispatcher.getFullBeanId(), prototypeId);
                return false;
            }
        }

        // Duplikat-Prüfung
        if (responseDispatchers == null) {
            responseDispatchers = new ArrayList<>();
        }
        boolean alreadyRegistered = responseDispatchers.stream()
                .anyMatch(d -> d.getFullBeanId() != null
                        && d.getFullBeanId().equals(dispatcher.getFullBeanId()));
        if (alreadyRegistered) {
            log.debug("Gateway: Response-Dispatcher '{}' bereits registriert — übersprungen",
                    dispatcher.getFullBeanId());
            return true;
        }

        responseDispatchers.add(dispatcher);
        log.info("Gateway: Response-Dispatcher '{}' registriert (gesamt: {})",
                dispatcher.getFullBeanId(), responseDispatchers.size());
        return true;
    }
}