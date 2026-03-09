package de.starima.pfw.base.processor.transformator.api;
/**
 * hat die gleiche Aufgabe wie ITransformerProcessor. Ist aber fÃ¼r die FÃ¤lle gedacht, bei denen die Transformation von einem gegebenen Subjekt
 * abhÃ¤ngt (z.B. einem java.lang.reflect.Field)
 * @param <S> - Subject (z.B. Field)
 * @param <I> - Input Typ
 * @param <O> - Output Typ
 */
public interface ISubjectTransformerProcessor<S,I,O> extends ITransformerProcessor<I,O> {
    public boolean isResponsibleForSubject(S subject);
    default boolean isResponsibleForSubjectAndInput(S subject, I input) {
        return isResponsibleForSubject(subject) && isResponsibleForInput(input);
    }
    public O transformValue(S subject, I input);
}