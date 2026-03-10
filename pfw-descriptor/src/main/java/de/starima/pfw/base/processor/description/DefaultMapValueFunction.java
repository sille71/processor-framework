package de.starima.pfw.base.processor.description;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.api.IMapValueDescriptor;
import de.starima.pfw.base.processor.description.api.IValueDescriptor;
import de.starima.pfw.base.processor.description.api.IValueFunction;
import de.starima.pfw.base.processor.set.api.ISetProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

@Getter
@Setter
@Slf4j
@Processor(
        description = "Transformiert eine Map von rohen Key-Value-Paaren in eine Map von aufgelösten Objekten.",
        categories = {"function/value/map"}
)
public class DefaultMapValueFunction extends AbstractValueFunction<Object, Map<String, Object>> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ProcessorParameter(description = "Die ValueFunction, mit der die Map-Werte transformiert werden.")
    private IValueFunction<ITransformationContext, Object, Object> valueFunction;

    @ProcessorParameter(description = "Definitionsbereich der Parameter (Prozessor-Identifier).")
    private ISetProcessor<Object> domainProcessor;
    @ProcessorParameter
    private ISetProcessor<Map<String, Object>> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<Map<String, Object>> imageProcessor;

    public static boolean isResponsibleFor(ITransformationContext transformationContext) {
        if (transformationContext == null) return false;
        Class<?> clazz = transformationContext.getRawType();
        Field field = transformationContext.getFieldToResolve();
        Class<?> valueType = field != null ? field.getType() : clazz;
        if (valueType == null || !Map.class.isAssignableFrom(valueType)) return false;

        Type genericType = field != null ? field.getGenericType() : null;
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type[] args = pt.getActualTypeArguments();
            if (args.length == 2 && args[1] == String.class) {
                return false; // SimpleMapValueFunction is responsible for Map<*, String>
            }
        }
        return true;
    }

    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return isResponsibleFor(transformationContext);
    }

    @Override
    public boolean isResponsibleForInput(Object input) {
        return input instanceof Map;
    }

    @Override
    public Map<String, Object> transformValue(Object input) {
        return Map.of();
    }

    @Override
    public Map<String, Object> transformValue(ITransformationContext transformationContext, Object input) {
        //TODO: noch zu implementieren! Benutzt MapInstanceProvider
        return null;
    }

    @Override
    public String getTypeSignature() {
        IDescriptorProcessor parent = getParentDescriptor();
        if (parent instanceof IMapValueDescriptor) {
            IMapValueDescriptor mapDescriptor = (IMapValueDescriptor) parent;
            IValueDescriptor valueDescriptor = mapDescriptor.getValueValueDescriptor();
            if (valueDescriptor != null) {
                return "map<string," + valueDescriptor.getTypeSignature() + ">";
            }
        }
        return "map<string,object>";
    }

    @Override
    public boolean isGeneric() {
        return false;
    }
}
