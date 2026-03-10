package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.set.api.ISetProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

@Slf4j
@Getter
@Setter
@Processor
public class ClassValueFunction extends AbstractValueFunction<Object, Class<?>> {
    public static boolean isResponsibleFor(ITransformationContext transformationContext) {
        if (transformationContext == null) return false;
        Class<?> clazz = transformationContext.getTargetType();
        Field field = transformationContext.getTargetField();
        Class<?> valueType = field != null ? field.getType() : clazz;
        return valueType != null && Class.class.isAssignableFrom(valueType);
    }

    //TODO: hier defaultwerte angeben! Die set Processoren benÃ¶tigen wir im ReconLight
    @ProcessorParameter(description = "Definitionsbereich der Parameter. Dies kÃ¶nnen Integer oder auch Strings sein. ")
    private ISetProcessor<Object> domainProcessor;
    @ProcessorParameter
    private ISetProcessor<Class<?>> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<Class<?>> imageProcessor;

    @Override
    public ISetProcessor<Object> getDomainProcessor() {
        return domainProcessor;
    }

    @Override
    public ISetProcessor<Class<?>> getCoDomainProcessor() {
        return coDomainProcessor;
    }

    @Override
    public ISetProcessor<Class<?>> getImageProcessor() {
        return imageProcessor;
    }

    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return isResponsibleFor(transformationContext);
    }

    @Override
    public Class<?> transformValue(Object input) {
        if (!isResponsibleForInput(input)) {
            log.warn("{}.transformValue: Can not transform value {}! Value is not in domain.", this.getIdentifier(), input);
            return null;
        }
        try {
            if (input instanceof Class<?>) {
                return (Class<?>)input;
            } else if (input != null && !"".equals(input))
                return Class.forName(input.toString());
        } catch (ClassNotFoundException e) {
            log.warn("{}.transformValue: Can not transform value {}!", this.getIdentifier(), input, e);
        }
        log.warn("{}.transformValue: Can not transform value {}! Value is properly not in domain.", this.getIdentifier(), input);
        return null;
    }

    public Object transformObjectToParameter(Class<?> value) {
        if (value == null) return "";
        return value.getName();
    }

    @Override
    public String getTypeSignature() {
        return "class";
    }

    @Override
    public boolean isGeneric() {
        return false;
    }
}