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
public class DoubleValueFunction extends AbstractValueFunction<Object,Double> {
    public static boolean isResponsibleFor(ITransformationContext transformationContext) {
        if (transformationContext == null) return false;
        Class<?> clazz = transformationContext.getTargetType();
        Field field = transformationContext.getTargetField();
        Class<?> valueType = field != null ? field.getType() : clazz;
        return valueType != null && (double.class.isAssignableFrom(valueType) || Double.class.isAssignableFrom(valueType));
    }

    //TODO: hier defaultwerte angeben! Die set Processoren benÃ¶tigen wir im ReconLight
    @ProcessorParameter(description = "Definitionsbereich der Parameter. Dies kÃ¶nnen Double oder auch Strings sein. ")
    private ISetProcessor<Object> domainProcessor;
    @ProcessorParameter
    private ISetProcessor<Double> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<Double> imageProcessor;

    @Override
    public ISetProcessor<Object> getDomainProcessor() {
        return domainProcessor;
    }

    @Override
    public ISetProcessor<Double> getCoDomainProcessor() {
        return coDomainProcessor;
    }

    @Override
    public ISetProcessor<Double> getImageProcessor() {
        return imageProcessor;
    }

    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return isResponsibleFor(transformationContext);
    }

    @Override
    public Double transformValue(Object input) {
        if (!isResponsibleForInput(input)) {
            log.warn("{}.transformValue: Can not transform value {}! Value is not in domain.", this.getIdentifier(), input);
            return null;
        }
        try {
            if (input instanceof Number) {
                return ((Number)input).doubleValue();
            } else if (input != null && !"".equals(input))
                return Double.parseDouble(input.toString());
        } catch (NumberFormatException e) {
            log.warn("{}.transformValue: Can not transform value {}!", this.getIdentifier(), input, e);
        }
        log.warn("{}.transformValue: Can not transform value {}! Value is properly not in domain.", this.getIdentifier(), input);
        return null;
    }

    public Object transformObjectToParameter(Double value) {
        if (value == null) return "";
        return value.toString();
    }

    @Override
    public String getTypeSignature() {
        return "double";
    }

    @Override
    public boolean isGeneric() {
        return false;
    }
}