package de.starima.pfw.base.processor.description.incubator.domain;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.IProcessorContext;

import java.lang.reflect.Field;

public interface ISeed {
    Field getSourceField();
    IProcessorContext getRuntimeContext();
    IProcessor getDraftProcessor();
}