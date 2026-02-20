package de.starima.pfw.base.processor.transformator.api;

import de.starima.pfw.base.processor.set.api.ISetProcessor;

public interface IFunction<I,O> extends ITransformerProcessor<I,O> {
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

    public IFunction<O,I> getReverseFunction();
}