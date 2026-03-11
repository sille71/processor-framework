package de.starima.pfw.base.processor.request.api;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.api.IProcessor;

/**
 * Zentrale Kommunikationsschnittstelle des Systems.
 *
 * <p>Empfängt alle Requests, dispatcht über RequestDispatcherChain,
 * leitet Responses über ResponseDispatcherChain weiter.
 *
 * <p>Services registrieren ihre Dispatcher zur Laufzeit über
 * {@link #registerDispatcher(IRequestDispatcherProcessor)}.
 * Die allowedDispatcherSet-Policy schränkt die zulässigen Dispatcher ein.
 *
 * <p>Biologisch: Die Zellmembran — kontrolliert, was ein- und ausgeht.
 * Rezeptoren (Dispatcher) werden bei Bedarf exprimiert, aber die Membran
 * entscheidet, was zugelassen wird.
 */
@Processor(
        description = "Zentrale Kommunikationsschnittstelle des Systems. " +
                "Empfängt alle Requests, dispatcht über RequestDispatcherChain, " +
                "leitet Responses über ResponseDispatcherChain weiter. " +
                "Services registrieren Dispatcher zur Laufzeit.",
        categories = {"request", "gateway"},
        tags = {"communication", "gateway", "request", "response", "policy"}
)
public interface IRequestGatewayProcessor extends IProcessor {

    /** Verarbeitet einen Request über die Dispatcher-Chain. */
    Object processRequest(Object request);

    /** Der konfigurierte Request-Dispatcher (erster in der Chain). */
    IRequestDispatcherProcessor getRequestDispatcher();

    /** Der konfigurierte Response-Dispatcher. */
    IResponseDispatcherProcessor getResponseDispatcher();

    /**
     * Registriert einen Dispatcher zur Laufzeit.
     * Wird gegen die allowedDispatcherSet-Policy geprüft.
     *
     * @return true wenn erfolgreich registriert, false wenn abgelehnt
     */
    boolean registerDispatcher(IRequestDispatcherProcessor dispatcher);
}