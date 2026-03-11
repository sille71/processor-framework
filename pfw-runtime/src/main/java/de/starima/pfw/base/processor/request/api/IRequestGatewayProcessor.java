package de.starima.pfw.base.processor.request.api;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.api.IProcessor;

/**
 * Zentrale Kommunikationsschnittstelle des Systems.
 *
 * <p>Empfängt alle Requests, dispatcht über RequestDispatcherChain,
 * leitet Responses über ResponseDispatcherChain weiter.
 *
 * <p>EINE Stelle für alle Requests — nicht verteilt über Kernel und Services.
 */
@Processor(
        description = "Zentrale Kommunikationsschnittstelle des Systems. " +
                "Empfängt alle Requests, dispatcht über RequestDispatcherChain, " +
                "leitet Responses über ResponseDispatcherChain weiter.",
        categories = {"request", "gateway"},
        tags = {"communication", "gateway", "request", "response"}
)
public interface IRequestGatewayProcessor extends IProcessor {

    /** Verarbeitet einen eingehenden Request und liefert die Response. */
    Object processRequest(Object request);

    /** Der konfigurierte Request-Dispatcher. */
    IRequestDispatcherProcessor getRequestDispatcher();

    /** Der konfigurierte Response-Dispatcher. */
    IResponseDispatcherProcessor getResponseDispatcher();
}