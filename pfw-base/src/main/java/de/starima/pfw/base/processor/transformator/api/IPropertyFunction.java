package de.starima.pfw.base.processor.transformator.api;

import de.starima.pfw.base.processor.set.api.ISetProcessor;

public interface IPropertyFunction<P,I,O> extends ISubjectTransformerProcessor<P,I,O> {
    /**
     * Beschreibt den Definitionsbereich der Funktion.
     * @return
     */
    public ISetProcessor getPropertyDomainProcessor(P property);

    /**
     * Beschreibt die Zielmenge der Funktion.
     * @return
     */
    public ISetProcessor getPropertyCoDomainProcessor(P property);

    /**
     * Beschreibt die Bildmenge (Wertemenge, Wertebereich) der Funktion.
     * @return
     */
    public ISetProcessor getPropertyImageProcessor(P property);
}