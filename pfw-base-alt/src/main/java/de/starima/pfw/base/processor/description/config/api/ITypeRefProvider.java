package de.starima.pfw.base.processor.description.config.api;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.api.IProcessor;

@Processor
/**
 * Ein spezialisierter Prozessor, der den gesamten IDescriptorConstructorContext
 * analysieren und in eine unverÃ¤nderliche, reichhaltige ITypeRef-Instanz
 * umwandeln kann.
 */
public interface ITypeRefProvider extends IProcessor {

    /**
     * PrÃ¼ft, ob dieser Resolver fÃ¼r den gegebenen Konstruktionskontext zustÃ¤ndig ist.
     * Die Entscheidung basiert typischerweise auf dem Typ, der aus dem Kontext
     * extrahiert wird (z.B. context.getSourceField().getGenericType()).
     *
     * @param context Der spezifische Task Context als TrÃ¤ger der nÃ¶tigen Info, wie z.b. Type.
     * @return true, wenn dieser Resolver den Kontext verarbeiten kann.
     */
    boolean isResponsibleFor(ITypeResolutionContext context);

    /**
     * Analysiert den Kontext und erzeugt die entsprechende ITypeRef-Instanz.
     * Diese Methode sollte nur aufgerufen werden, wenn isResponsibleFor() true zurÃ¼ckgibt.
     *
     * @param context Der spezifische Task Context als TrÃ¤ger der nÃ¶tigen Info, wie z.b. Type oder rootProvider Der Haupt-Orchestrator (die Kette), der fÃ¼r rekursive Aufrufe verwendet wird.
     *                     Kann fÃ¼r den initialen Aufruf an der Kette selbst null sein.
     * @return Eine ITypeRef-Instanz, die den Typ und seine Metadaten vollstÃ¤ndig beschreibt.
     */
    ITypeRef provide(ITypeResolutionContext context);
}