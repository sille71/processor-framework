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
import java.lang.reflect.Type;

@Getter
@Setter
@NoArgsConstructor
public class DefaultTransformationContext extends DefaultTaskContext implements ITransformationContext {

    private Type typeToResolve;
    private Object objectToResolve;
    private Field fieldToResolve;
    private LoadStrategy loadStrategy = LoadStrategy.DEEP;
    private ProcessorParameter processorParameter;
    private IDescriptorProcessor parentDescriptor;
    private IValueDescriptor valueDescriptor;
    @SuppressWarnings("rawtypes")
    private IValueFunction valueFunction;

    public DefaultTransformationContext(Object objectToResolve, IProcessorContext runtimeContext) {
        this.objectToResolve = objectToResolve;
        this.setRuntimeContext(runtimeContext);
    }

    @Override
    public String toString() {
        return "DefaultTransformationContext{" +
               "typeToResolve=" + typeToResolve +
               ", fieldToResolve=" + fieldToResolve +
               ", objectToResolve=" + (objectToResolve != null ? objectToResolve.getClass().getSimpleName() : "null") +
               ", loadStrategy=" + loadStrategy +
               ", processorParameter=" + processorParameter +
               ", parentDescriptor=" + parentDescriptor +
               ", valueDescriptor=" + valueDescriptor +
               ", valueFunction=" + valueFunction +
               "} " + super.toString();
    }
}