package de.starima.pfw.base.processor.transformator.api;

import java.lang.reflect.Field;

//hat die gleiche Aufgabe wie IReconItemTransformatorProcessor. Ist aber fÃ¼r die FÃ¤lle gedacht, bei denen die Typen erst zur Laufzeit bekannt sind (siehe ProcessorParameter)
public interface IValueTransformerProcessor extends ISubjectTransformerProcessor<Field,Object,Object> {
}