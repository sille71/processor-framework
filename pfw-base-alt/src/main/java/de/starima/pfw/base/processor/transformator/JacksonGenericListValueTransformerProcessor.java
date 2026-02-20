package de.starima.pfw.base.processor.transformator;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.transformator.api.IValueTransformerProcessor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;

@Slf4j
@Processor(description = "Transformiert Werte vom Typ Map<String, Object> oder JSON Strings in konkrete Objekte. Dies ist der Defaulttransformator, er sollte am Ende einer Kette eingesetzt werden.")
public class JacksonGenericListValueTransformerProcessor extends AbstractProcessor implements IValueTransformerProcessor {
    @Override
    public Object transformValue(Field field, Object value) {
        if (value == null) return null;

        if (!this.isResponsibleForSubject(field)) {
            log.warn("{} is not responsible for field {}! Can not transform value {}", getIdentifier(), field.getName(), value);
            return null;
        }

        String jsonString;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
            log.trace("transform list value {}", jsonString);
            ParameterizedType type = (ParameterizedType)field.getGenericType();
            return objectMapper.readValue(jsonString, objectMapper.getTypeFactory().constructCollectionType(List.class, objectMapper.getTypeFactory().constructType(type.getActualTypeArguments()[0])));
        } catch (Exception e) {
            log.error("Can not transform value {}", value, e);
        }

        return null;
    }

    @Override
    public boolean isResponsibleForSubject(Field field) {
        return field != null && List.class.isAssignableFrom(field.getType()) && field.getGenericType() instanceof ParameterizedType;
    }

    @Override
    public boolean isResponsibleForInput(Object input) {
        return true;
    }

    @Override
    public Object transformValue(Object input) {
        return null;
    }
}