package de.starima.pfw.base.processor.description.config.api;

import de.starima.pfw.base.processor.context.api.ITaskContext;

import java.lang.reflect.Type;

public interface ITypeResolutionContext extends ITaskContext {
    /**
     * Der Java-Typ, der aufgelÃ¶st werden soll.
     */
    Type getTypeToResolve();

    /**
     * Der Root-Provider (die Kette), der fÃ¼r rekursive Aufrufe verwendet wird.
     */
    ITypeRefProvider getRootProvider();

    /**
     * Setzt den Root-Provider. Wichtig fÃ¼r die Bootstrap-Logik in der Kette.
     */
    void setRootProvider(ITypeRefProvider rootProvider);
}