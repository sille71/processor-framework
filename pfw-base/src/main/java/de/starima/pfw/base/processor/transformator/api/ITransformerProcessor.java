package de.starima.pfw.base.processor.transformator.api;

import de.starima.pfw.base.processor.api.IProcessor;

/**
 * ErfÃ¼llt die analoge Aufgabe wie der IReconItemTransformatorProcessor. Implementiert aber nicht das ItemProcessor Interface von Spring.
 * @param <I>
 * @param <O>
 */
public interface ITransformerProcessor<I,O> extends IProcessor {
    public boolean isResponsibleForInput(I input);
    public O transformValue(I input);
}