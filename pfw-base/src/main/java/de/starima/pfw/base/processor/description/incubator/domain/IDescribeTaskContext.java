package de.starima.pfw.base.processor.description.incubator.domain;

import java.lang.reflect.Field;

/**
 * TaskContext für DESCRIBE (Java-Objekt/Typ → Descriptor-Graph).
 *
 * <p>Ersetzt {@code IDescribeSource} + {@code IDescribePolicy}.
 * Enthält alle Eingaben, die der Incubator für eine Beschreibung benötigt.
 */
public interface IDescribeTaskContext extends IBuildTaskContext {

    @Override
    default BuildMode getMode() { return BuildMode.DESCRIBE; }

    /**
     * Das Java-Objekt, das beschrieben werden soll (Instance-Describe).
     * <p>Null bei reinem Schema/Blueprint-Describe.
     */
    Object getSourceObject();

    /**
     * Der Java-Typ, der beschrieben werden soll.
     * <ul>
     *   <li>Bei Instance-Describe: {@code sourceObject.getClass()}</li>
     *   <li>Bei Blueprint-Describe: die Klasse direkt</li>
     * </ul>
     */
    Class<?> getSourceType();

    /**
     * Das Quell-Feld (optional, wenn ein einzelnes Feld beschrieben wird).
     */
    default Field getSourceField() { return null; }
}