package de.starima.pfw.base.processor.description;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.set.api.ISetProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

//TODO: wird spÃ¤ter durch MapValueDescriptor abgelÃ¶st

@Slf4j
@Getter
@Setter
@Processor
public class SimpleMapValueFunction extends AbstractValueFunction<Object, Map<String, String>> {
    public static boolean isResponsibleFor(ITransformationContext transformationContext) {
        if (transformationContext == null) return false;
        Class<?> clazz = transformationContext.getRawType();
        Field field = transformationContext.getFieldToResolve();
        Class<?> valueType = field != null ? field.getType() : clazz;
        if (valueType != null && Map.class.isAssignableFrom(valueType)) {
            // Den generischen Typ des Feldes abrufen
            Type genericType = field.getGenericType();

            // ÃœberprÃ¼fen, ob es sich um einen ParameterizedType handelt (z.B. Map<String, String>)
            if (genericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericType;

                // Die Typ-Parameter (z.B. <String, String>) abrufen
                Type[] typeArguments = parameterizedType.getActualTypeArguments();

                // ÃœberprÃ¼fen, ob es genau zwei Typ-Parameter gibt und ob beide String sind
                return typeArguments.length == 2 &&
                        typeArguments[0] == String.class &&
                        typeArguments[1] == String.class;
            }
        }
        return false;
    }

    //TODO: hier defaultwerte angeben! Die set Processoren benÃ¶tigen wir im ReconLight
    @ProcessorParameter(description = "Definitionsbereich der Parameter. Dies kÃ¶nnen Integer oder auch Strings sein. ")
    private ISetProcessor<Object> domainProcessor;
    @ProcessorParameter
    private ISetProcessor<Map<String, String>> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<Map<String, String>> imageProcessor;


    @Override
    public ISetProcessor<Object> getDomainProcessor() {
        return domainProcessor;
    }

    @Override
    public ISetProcessor<Map<String, String>> getCoDomainProcessor() {
        return coDomainProcessor;
    }

    @Override
    public ISetProcessor<Map<String, String>> getImageProcessor() {
        return imageProcessor;
    }

    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return isResponsibleFor(transformationContext);
    }

    @Override
    public Map<String, String> transformValue(Object input) {
        if (!isResponsibleForInput(input)) {
            log.warn("{}.transformValue: Can not transform value {}! Value is not in domain.", this.getIdentifier(), input);
            return null;
        }
        if (input == null) {
            return new HashMap<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String,String> map = mapper.convertValue(input, Map.class);
            return map;
        } catch (IllegalArgumentException e) {
            log.warn("Can not convert {} to map Msg:", input, e);
        }

        log.warn("{}.transformValue: Can not transform value {}! Value is properly not in domain.", this.getIdentifier(), input);
        return null;
    }

    public Object transformObjectToParameter(Map<String, String> value) {
        if (value == null) return "";
        return value;
    }

    @Override
    public String getTypeSignature() {
        return "map<string,string>";
    }

    @Override
    public boolean isGeneric() {
        return false; // Ein String ist niemals generisch
    }
}