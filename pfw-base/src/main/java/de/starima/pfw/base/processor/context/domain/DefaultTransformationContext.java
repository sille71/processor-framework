package de.starima.pfw.base.processor.context.domain;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.context.api.LoadStrategy;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.api.IValueDescriptor;
import de.starima.pfw.base.processor.description.api.IValueFunction;
import lombok.*;

import java.lang.reflect.Field;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
public class DefaultTransformationContext extends  DefaultTaskContext implements ITransformationContext {
    private Field  targetField;
    private Class<?> targetType;
    private Object targetObject;
    private LoadStrategy loadStrategy = LoadStrategy.DEEP;
    private ProcessorParameter processorParameterAnnotation;
    private Optional<IDescriptorProcessor> parentDescriptor;
    private Optional<IValueDescriptor> valueDescriptor;
    private Optional<IValueFunction<ITransformationContext, Object, Object>> valueFunction;

    public DefaultTransformationContext(Object targetObject, IProcessorContext runtimeContext) {
        this.targetObject = targetObject;
        this.setRuntimeContext(runtimeContext);
    }

    @Override
    public String toString() {
        return "DefaultTransformationContext{" +
               "targetField=" + targetField +
               ", targetType=" + targetType +
               ", targetObject=" + (targetObject != null ? targetObject.getClass().getSimpleName() : "null") +
                ",loadStrategy=" + loadStrategy +
               ", processorParameterAnnotation=" + processorParameterAnnotation +
               ", parentDescriptor=" + parentDescriptor +
               ", valueDescriptor=" + valueDescriptor +
               ", valueFunction=" + valueFunction +
               "} " + super.toString();
    }

}