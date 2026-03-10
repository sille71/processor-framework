package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.set.api.ISetProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;

@Slf4j
@Getter
@Setter
@Processor
public class StringValueFunction extends AbstractValueFunction<Object, String> {
    public static boolean isResponsibleFor(ITransformationContext transformationContext) {
        if (transformationContext == null) return false;
        Class<?> clazz = transformationContext.getRawType();
        Field field = transformationContext.getFieldToResolve();
        Class<?> valueType = field != null ? field.getType() : clazz;
        return valueType != null && String.class.isAssignableFrom(valueType);
    }

    //TODO: hier defaultwerte angeben! Die set Processoren benÃ¶tigen wir im ReconLight
    @ProcessorParameter(description = "Definitionsbereich der Parameter. Dies kÃ¶nnen Integer oder auch Strings sein. ")
    private ISetProcessor<Object> domainProcessor;
    @ProcessorParameter
    private ISetProcessor<String> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<String> imageProcessor;


    @Override
    public ISetProcessor<Object> getDomainProcessor() {
        return domainProcessor;
    }

    @Override
    public ISetProcessor<String> getCoDomainProcessor() {
        return coDomainProcessor;
    }

    @Override
    public ISetProcessor<String> getImageProcessor() {
        return imageProcessor;
    }

    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return isResponsibleFor(transformationContext);
    }

    @Override
    public String transformValue(Object input) {
        if (!isResponsibleForInput(input)) {
            log.warn("{}.transformValue: Can not transform value {}! Value is not in domain.", this.getIdentifier(), input);
            return null;
        }
        return input != null ? StringUtils.defaultIfBlank(input.toString(),null) : "";
    }

    public Object transformObjectToParameter(String value) {
        if (value == null) return null;
        return value;
    }

    @Override
    public String getTypeSignature() {
        return "string";
    }

    @Override
    public boolean isGeneric() {
        return false; // Ein String ist niemals generisch
    }
}