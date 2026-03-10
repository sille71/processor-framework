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
import java.lang.reflect.ParameterizedType;
import java.util.List;

@Slf4j
@Getter
@Setter
@Processor
public class JacksonGenericListValueFunction extends AbstractValueFunction<Object,List<?>>  {
    public static boolean isResponsibleFor(ITransformationContext transformationContext) {
        if (transformationContext == null) return false;
        Field field = transformationContext.getFieldToResolve();
        ProcessorParameter p = transformationContext.getProcessorParameter();
        if (p == null && field != null) p = field.getAnnotation(ProcessorParameter.class);
        if (p == null) return false;
        return StringUtils.hasLength(p.valueFunctionPrototypeIdentifier()) && p.valueFunctionPrototypeIdentifier().equalsIgnoreCase(ProcessorUtils.getPrototypeIdentifierFromClass(JacksonGenericListValueFunction.class));
    }

    //TODO: hier defaultwerte angeben! Die set Processoren benÃ¶tigen wir im ReconLight
    @ProcessorParameter(description = "Definitionsbereich der Parameter. Dies kÃ¶nnen Integer oder auch Strings sein. ")
    private ISetProcessor<Object> domainProcessor;
    @ProcessorParameter
    private ISetProcessor<List<?>> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<List<?>> imageProcessor;

    @Override
    public ISetProcessor<Object> getDomainProcessor() {
        return domainProcessor;
    }

    @Override
    public ISetProcessor<List<?>> getCoDomainProcessor() {
        return coDomainProcessor;
    }

    @Override
    public ISetProcessor<List<?>> getImageProcessor() {
        return imageProcessor;
    }
    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return isResponsibleFor(transformationContext);
    }

    @Override
    public List<?> transformValue(ITransformationContext transformationContext, Object input) {
        if (!isResponsibleForSubject(transformationContext)) {
            log.warn("{}.transformValue: Can not transform value {}! Processor is not responsible for subject {}.", this.getIdentifier(), input, transformationContext.getFieldToResolve());
            return null;
        }
        if (!isResponsibleForInput(input)) {
            log.warn("{}.transformValue: Can not transform value {}! Value is not in domain.", this.getIdentifier(), input);
            return null;
        }
        if (input == null || "".equals(input)) return null;

        //wir Ã¼berprÃ¼fen zunÃ¤chst, ob der input schon vom richtigen List Typ ist
        try {
            Object backUp = transformationContext.getFieldToResolve().get(input);
            transformationContext.getFieldToResolve().set(transformationContext.getObjectToResolve(), input);
            transformationContext.getFieldToResolve().set(backUp, transformationContext.getObjectToResolve());
            return (List<?>) input;
        } catch (Exception e) {
            log.debug("input is not of destination type for field {}", transformationContext.getFieldToResolve());
        }

        //wir transformieren
        String jsonString;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(input);
            log.trace("transform list value {}", jsonString);
            ParameterizedType type = (ParameterizedType) transformationContext.getFieldToResolve().getGenericType();
            return objectMapper.readValue(jsonString, objectMapper.getTypeFactory().constructCollectionType(List.class, objectMapper.getTypeFactory().constructType(type.getActualTypeArguments()[0])));
        } catch (Exception e) {
            log.error("Can not transform value {}", transformationContext, e);
        }

        return null;
    }

    @Override
    public List<?> transformValue(Object input) {
        log.warn("{}: transformValue(input) is not defined.");
        return null;
    }

    public Object transformObjectToParameter(List<?> value) {
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
        return "list<object>";
    }

    @Override
    public boolean isGeneric() {
        return true;
    }
}