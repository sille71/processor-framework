package de.starima.pfw.base.processor.description.incubator.domain;

import de.starima.pfw.base.processor.api.IProcessor;

import java.util.Map;

/**
 * TaskContext für CONSTRUCT (beanParameterMap → lebendiger Objektgraph).
 *
 * <p>Ersetzt {@code IConstructSource} + {@code IConstructPolicy}.
 * Enthält alle Eingaben, die der Incubator für eine Konstruktion benötigt.
 */
public interface IConstructTaskContext extends IBuildTaskContext {

    @Override
    default BuildMode getMode() { return BuildMode.CONSTRUCT; }

    /**
     * Die Root-BeanId des zu erzeugenden Prozessors.
     * <p>Beispiel: {@code "csvReconService:instance1@instance"}
     */
    String getRootBeanId();

    /**
     * Der aufzulösende Ziel-Typ (optional, für typisierte Erzeugung).
     * <p>Default: Object.class (untypisiert).
     */
    Class<?> getTargetType();

    /**
     * Die beanParameterMap für die Konstruktion.
     * <p>Kommt standardmäßig aus dem RuntimeContext — kann aber auch direkt
     * gesetzt werden (z.B. beim Test oder bei direkter Konfiguration).
     */
    default Map<String, Map<String, Object>> getBeanParameterMap() {
        return getRuntimeContext() != null
                ? getRuntimeContext().getContextMergedBeanParameterMap()
                : null;
    }

    /**
     * Ein vorab erzeugter Draft-Prozessor (optional).
     * <p>Wird verwendet, wenn die Bean bereits existiert und nur
     * initialisiert werden muss.
     */
    IProcessor getDraftProcessor();
}