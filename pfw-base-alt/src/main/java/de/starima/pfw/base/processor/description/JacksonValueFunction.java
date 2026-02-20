package de.starima.pfw.base.processor.description;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.set.api.ISetProcessor;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;

@Slf4j
@Getter
@Setter
@Processor
public class JacksonValueFunction extends AbstractValueFunction<Object,Object> {
    public static boolean isResponsibleFor(ITransformationContext transformationContext) {
        if (transformationContext == null) return false;
        Field field = transformationContext.getTargetField();
        ProcessorParameter p = transformationContext.getProcessorParameterAnnotation();
        if (p == null && field != null) p = field.getAnnotation(ProcessorParameter.class);
        if (p == null) return false;
        return StringUtils.hasLength(p.valueFunctionPrototypeIdentifier()) && p.valueFunctionPrototypeIdentifier().equalsIgnoreCase(ProcessorUtils.getPrototypeIdentifierFromClass(JacksonValueFunction.class));
    }

    //TODO: hier defaultwerte angeben! Die set Processoren benÃ¶tigen wir im ReconLight
    @ProcessorParameter(description = "Definitionsbereich der Parameter. Dies kÃ¶nnen Integer oder auch Strings sein. ")
    private ISetProcessor<Object> domainProcessor;
    @ProcessorParameter
    private ISetProcessor<Object> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<Object> imageProcessor;

    @Override
    public ISetProcessor<Object> getDomainProcessor() {
        return domainProcessor;
    }

    @Override
    public ISetProcessor<Object> getCoDomainProcessor() {
        return coDomainProcessor;
    }

    @Override
    public ISetProcessor<Object> getImageProcessor() {
        return imageProcessor;
    }
    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return isResponsibleFor(transformationContext);
    }

    @Override
    public Object transformValue(ITransformationContext transformationContext, Object input) {
        if (!isResponsibleForSubject(transformationContext)) {
            log.warn("{}.transformValue: Can not transform value {}! Processor is not responsible for subject {}.", this.getIdentifier(), input, transformationContext.getTargetField());
            return null;
        }
        if (!isResponsibleForInput(input)) {
            log.warn("{}.transformValue: Can not transform value {}! Value is not in domain.", this.getIdentifier(), input);
            return null;
        }
        if (input == null) return null;
        String jsonString;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(input);
            log.trace("transform value {}", jsonString);

            return objectMapper.readValue(jsonString, transformationContext.getTargetField().getType());
        } catch (Exception e) {
            log.trace("Can not transform value {}", input, e);
        }

        return null;
    }

    @Override
    public Object transformValue(Object input) {
        return null;
    }

    public Object transformObjectToParameter(Object value) {
        if (value == null) return "";
        try {
            // ObjectMapper objectMapper = new ObjectMapper();
            // return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
            return value;
        } catch (Exception e) {
            log.trace("Can not transform value {}", value, e);
            return "";
        }
    }

    @Override
    public String getTypeSignature() {
        return "object";
    }

    @Override
    public boolean isGeneric() {
        return false;
    }
}