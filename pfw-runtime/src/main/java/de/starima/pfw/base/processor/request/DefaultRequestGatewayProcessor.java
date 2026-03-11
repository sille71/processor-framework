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
 * Standard-Gateway mit Dispatcher-Chain, dynamischer Registrierung und Policy.
 *
 * <p>Chain-of-Responsibility: Der erste zuständige Dispatcher gewinnt.
 * Services können Dispatcher zur Laufzeit via {@link #registerDispatcher} hinzufügen.
 * Die allowedDispatcherSet-Policy bestimmt, welche Dispatcher akzeptiert werden.
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Standard-Gateway mit Dispatcher-Chain, " +
                "dynamischer Registrierung und Zulassungs-Policy.",
        categories = {"request", "gateway"},
        tags = {"communication", "gateway", "request", "response", "policy"}
)
public class DefaultRequestGatewayProcessor extends AbstractProcessor
        implements IRequestGatewayProcessor {

    @ProcessorParameter(description = "Die statisch konfigurierten Dispatcher. " +
            "Zur Laufzeit können weitere über registerDispatcher() hinzukommen.")
    private List<IRequestDispatcherProcessor> dispatchers;

    @ProcessorParameter(description = "Response-Dispatcher-Chain.")
    private IResponseDispatcherProcessor responseDispatcher;

    @ProcessorParameter(description = "Erlaubte Dispatcher-PrototypIDs. " +
            "Wenn gesetzt, werden nur Dispatcher akzeptiert, deren PrototypID " +
            "in dieser Menge enthalten ist. Wenn null, werden alle akzeptiert.")
    private ISetProcessor<String> allowedDispatcherSet;

    @Override
    public Object processRequest(Object request) {
        if (dispatchers == null || dispatchers.isEmpty()) {
            log.warn("Gateway: keine Dispatcher konfiguriert");
            return null;
        }

        // Chain-of-Responsibility: erster zuständiger Dispatcher gewinnt
        for (IRequestDispatcherProcessor dispatcher : dispatchers) {
            if (dispatcher.isResponsibleForRequest(request)) {
                log.debug("Gateway: Dispatcher '{}' übernimmt Request",
                        dispatcher.getFullBeanId());
                Object response = dispatcher.dispatchRequest(request);

                if (responseDispatcher != null && response != null) {
                    response = responseDispatcher.dispatchResponse(response);
                }
                return response;
            }
        }

        log.info("Gateway: kein Dispatcher zuständig für Request vom Typ {}",
                request != null ? request.getClass().getSimpleName() : "null");
        return null;
    }

    @Override
    public IRequestDispatcherProcessor getRequestDispatcher() {
        // Abwärtskompatibilität — liefert den ersten Dispatcher
        return (dispatchers != null && !dispatchers.isEmpty()) ? dispatchers.get(0) : null;
    }

    @Override
    public boolean registerDispatcher(IRequestDispatcherProcessor dispatcher) {
        if (dispatcher == null) return false;

        // Policy-Prüfung
        if (allowedDispatcherSet != null) {
            String prototypeId = dispatcher.getProtoTypeIdentifier();
            if (!allowedDispatcherSet.isMember(prototypeId)) {
                log.warn("Gateway: Dispatcher '{}' (prototypeId='{}') nicht in " +
                        "der erlaubten Menge — abgelehnt",
                        dispatcher.getFullBeanId(), prototypeId);
                return false;
            }
        }

        // Duplikat-Prüfung
        if (dispatchers == null) {
            dispatchers = new ArrayList<>();
        }
        boolean alreadyRegistered = dispatchers.stream()
                .anyMatch(d -> d.getFullBeanId() != null
                        && d.getFullBeanId().equals(dispatcher.getFullBeanId()));
        if (alreadyRegistered) {
            log.debug("Gateway: Dispatcher '{}' bereits registriert — übersprungen",
                    dispatcher.getFullBeanId());
            return true;
        }

        dispatchers.add(dispatcher);
        log.info("Gateway: Dispatcher '{}' registriert (gesamt: {})",
                dispatcher.getFullBeanId(), dispatchers.size());
        return true;
    }
}