package de.starima.pfw.base.processor.description.incubator.domain;

import de.starima.pfw.base.processor.context.api.ITaskContext;
import de.starima.pfw.base.processor.context.api.LoadStrategy;
import de.starima.pfw.base.processor.description.incubator.api.IConstructionManager;

/**
 * Basis-Context für alle Build-Läufe (describe, construct, edit).
 *
 * <p>Fasst gemeinsame Steuerungsparameter zusammen: Modus, ConstructionManager,
 * Rekursionstiefe und LoadStrategy.
 */
public interface IBuildTaskContext extends ITaskContext {

    /**
     * Modus dieses Build-Laufs.
     */
    enum BuildMode {
        /** Java-Objekt/Typ → Descriptor-Graph */
        DESCRIBE,
        /** beanParameterMap → lebendiger Objektgraph */
        CONSTRUCT,
        /** Langlebiger Workspace + Lazy Loading + Patches */
        EDIT
    }

    BuildMode getMode();

    /**
     * Der ConstructionManager für diesen Build-Lauf.
     * Wird vom Incubator erzeugt und in den Context eingehängt.
     */
    IConstructionManager getConstructionManager();
    void setConstructionManager(IConstructionManager constructionManager);

    /**
     * Maximale Rekursionstiefe für den Build.
     * Default: unbegrenzt.
     */
    default int getMaxDepth() { return Integer.MAX_VALUE; }

    /**
     * LoadStrategy (DEEP, SHALLOW, LAZY) für den Build.
     * Default: DEEP.
     */
    default LoadStrategy getLoadStrategy() { return LoadStrategy.DEEP; }
}
