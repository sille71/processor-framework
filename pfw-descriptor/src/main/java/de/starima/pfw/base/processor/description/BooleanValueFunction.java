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
public class BooleanValueFunction extends AbstractValueFunction<Object, Boolean> {
    public static boolean isResponsibleFor(ITransformationContext transformationContext) {
        if (transformationContext == null) return false;
        Class<?> clazz = transformationContext.getRawType();
        Field field = transformationContext.getFieldToResolve();
        Class<?> valueType = field != null ? field.getType() : clazz;
        return valueType != null && (boolean.class.isAssignableFrom(valueType) || Boolean.class.isAssignableFrom(valueType));
    }

    //TODO: hier defaultwerte angeben! Die set Processoren benÃ¶tigen wir im ReconLight
    @ProcessorParameter(description = "Definitionsbereich der Parameter. Dies kÃ¶nnen Integer oder auch Strings sein. ")
    private ISetProcessor<Object> domainProcessor;
    @ProcessorParameter
    private ISetProcessor<Boolean> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<Boolean> imageProcessor;

    @Override
    public ISetProcessor<Object> getDomainProcessor() {
        return domainProcessor;
    }

    @Override
    public ISetProcessor<Boolean> getCoDomainProcessor() {
        return coDomainProcessor;
    }

    @Override
    public ISetProcessor<Boolean> getImageProcessor() {
        return imageProcessor;
    }

    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return isResponsibleFor(transformationContext);
    }

    @Override
    public Boolean transformValue(Object input) {
        if (!isResponsibleForInput(input)) {
            log.warn("{}.transformValue: Can not transform value {}! Value is not in domain.", this.getIdentifier(), input);
            return null;
        }

        if (input instanceof Boolean) {
            return (Boolean)input;
        } else if (input != null && !"".equals(input))
            return "TRUE".equalsIgnoreCase(input.toString());

        log.warn("{}.transformValue: Can not transform value {}! Value is properly not in domain.", this.getIdentifier(), input);
        return false;
    }

    public Object transformObjectToParameter(Boolean value) {
        if (value == null) return "";
        return value ? "TRUE" : "FALSE";
    }

    @Override
    public String getTypeSignature() {
        return "boolean";
    }

    @Override
    public boolean isGeneric() {
        return false;
    }
}