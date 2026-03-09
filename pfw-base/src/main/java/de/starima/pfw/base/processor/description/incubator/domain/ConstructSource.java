package de.starima.pfw.base.processor.description.incubator.domain;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;

@Getter @Setter
public class ConstructSource implements  IConstructSource {
    private Field sourceField;
    private IProcessorContext runtimeContext;
    private IProcessor draftProcessor;
}