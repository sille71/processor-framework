package de.starima.pfw.base.processor.context.api;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.api.IValueDescriptor;
import de.starima.pfw.base.processor.description.api.IValueFunction;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface ITransformationContext extends ITaskContext {

    // --- Kern-Methoden ---

    /**
     * Der aufzulösende Java-Typ.
     * Kann ein {@code Class<?>}, {@code ParameterizedType} (z.B. {@code List<IProcessor>}),
     * {@code GenericArrayType}, etc. sein.
     */
    Type getTypeToResolve();
    void setTypeToResolve(Type type);

    /**
     * Das Java-Objekt, das untersucht werden soll.
     * Bei provide(): die bereits vorhandene Bean-Instanz (oder null).
     * Bei extract(): das lebendige Objekt.
     */
    Object getObjectToResolve();
    void setObjectToResolve(Object object);

    /**
     * Das Feld, in das der Wert injiziert (provide) bzw. aus dem gelesen wird (extract).
     * Trägt Generics-Information und die @ProcessorParameter-Annotation.
     */
    Field getFieldToResolve();
    void setFieldToResolve(Field field);

    /**
     * Die @ProcessorParameter-Annotation des Feldes.
     */
    ProcessorParameter getProcessorParameter();
    void setProcessorParameter(ProcessorParameter processorParameter);

    // --- Descriptor-Bezug (kein Optional) ---

    IValueDescriptor getValueDescriptor();
    void setValueDescriptor(IValueDescriptor valueDescriptor);

    IDescriptorProcessor getParentDescriptor();

    @SuppressWarnings("rawtypes")
    IValueFunction getValueFunction();

    // --- Laden ---

    LoadStrategy getLoadStrategy();
    void setLoadStrategy(LoadStrategy loadStrategy);

    // --- Convenience ---

    /**
     * Extrahiert die raw {@code Class<?>} aus dem Type.
     * Für ParameterizedType: rawType. Für Class: direkt. Sonst: null.
     */
    default Class<?> getRawType() {
        Type type = getTypeToResolve();
        if (type instanceof Class<?> c) return c;
        if (type instanceof ParameterizedType pt
                && pt.getRawType() instanceof Class<?> raw) return raw;
        return null;
    }
}