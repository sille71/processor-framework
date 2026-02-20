package de.starima.pfw.base.processor.transformator.api;


/**
 * hat die gleiche Aufgabe wie IReconItemTransformatorProcessor. Ist aber fÃ¼r die FÃ¤lle gedacht, bei denen die Typen erst zur Laufzeit bekannt sind (siehe ProcessorParameter)
 * @param <P> - Property (z.B. Field)
 * @param <I> - Parameter Typ
 * @param <O> - Property Typ
 */
public interface IBidirectionalPropertyTransformerProcessor<P,I,O> extends ISubjectTransformerProcessor<P,I,O> {
    I reverseTransformValue(O value, P property);
}