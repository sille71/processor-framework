package de.starima.pfw.base.processor.description.incubator.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.incubator.domain.IConstructTaskContext;
import de.starima.pfw.base.processor.description.incubator.domain.IDescribeTaskContext;
import de.starima.pfw.base.processor.description.incubator.domain.IEditTaskContext;

/**
 * Zentrales Einstiegspunkt für describe, construct und edit.
 *
 * <p>Alle Operationen erhalten einen {@code IBuildTaskContext} statt separater
 * Source/Policy-Objekte — der Context trägt sowohl Eingabedaten als auch
 * Steuerungsparameter.
 */
public interface IIncubator extends IProcessor {

    /**
     * Beschreibt ein Java-Objekt oder einen Typ als Descriptor-Graph (extract-Pfad).
     *
     * @param context Enthält sourceObject/sourceType, LoadStrategy, ConstructionManager
     * @return Session mit dem Root-Descriptor und dem serialisierten extractionResult
     */
    IDescribeSession startDescribe(IDescribeTaskContext context);

    /**
     * Erzeugt einen lebendigen Objektgraph aus einer beanParameterMap (provide-Pfad).
     *
     * @param context Enthält rootBeanId, targetType, RuntimeContext mit beanParameterMap
     * @return Session mit dem erzeugten Root-Objekt
     */
    <T> IConstructSession<T> startConstruct(IConstructTaskContext context);

    /**
     * Startet eine langlebige Edit-Session (Workspace + Lazy Loading + Patches).
     *
     * @param context Enthält editTarget, maxDepth, pageSize, ConstructionManager
     * @return Session mit dem partiellen Descriptor-Workspace
     */
    IEditSession startEdit(IEditTaskContext context);

    // =========================================================================
    // Convenience-Methoden
    // =========================================================================

    default IDescriptorProcessor describe(IDescribeTaskContext context) {
        var session = startDescribe(context);
        return session != null ? session.getRoot() : null;
    }

    default Object construct(IConstructTaskContext context) {
        var session = startConstruct(context);
        return session != null ? session.getRoot() : null;
    }
}