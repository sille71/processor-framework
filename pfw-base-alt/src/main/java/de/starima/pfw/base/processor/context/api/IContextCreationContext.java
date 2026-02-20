package de.starima.pfw.base.processor.context.api;

import java.lang.reflect.Type;

public interface IContextCreationContext extends ITaskContext {
    /**
     * Der Java-Typ, der aufgelÃ¶st werden soll.
     */
    Type getTypeToResolve();

    /**
     * Das Java Object, welches untersucht werden soll.
     * @return
     */
    Object getObjectToResolve();
}