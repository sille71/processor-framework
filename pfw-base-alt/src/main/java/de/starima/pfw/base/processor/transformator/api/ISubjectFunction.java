package de.starima.pfw.base.processor.transformator.api;

import de.starima.pfw.base.processor.set.api.ISetProcessor;

public interface ISubjectFunction<S,I,O> extends ISubjectTransformerProcessor<S,I,O> {
    /**
     * Beschreibt den Definitionsbereich der Funktion.
     * @return
     */
    public ISetProcessor<I> getDomainProcessor();

    /**
     * Beschreibt die Zielmenge der Funktion.
     * @return
     */
    public ISetProcessor<O> getCoDomainProcessor();

    /**
     * Beschreibt die Bildmenge (Wertemenge, Wertebereich) der Funktion. Ist Teilmenge vom coDomain.
     * @return
     */
    public ISetProcessor<O> getImageProcessor();

    public ISubjectFunction<S,O,I> getReverseFunction();
}