package de.starima.pfw.base.processor.transformator;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.transformator.api.IValueTransformerProcessor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

@Slf4j
@Processor(description = "Transformiert Werte vom Typ Map<String, Object> oder JSON Strings in konkrete Objekte. Dies ist der Defaulttransformator, er sollte am Ende einer Kette eingesetzt werden.")
public class JacksonValueTransformerProcessor extends AbstractProcessor implements IValueTransformerProcessor {
    @Override
    public Object transformValue(Field field,Object value) {
        if (value == null) return null;
        String jsonString;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
            log.trace("transform value {}", jsonString);

            return objectMapper.readValue(jsonString, field.getType());
        } catch (Exception e) {
            log.trace("Can not transform value {}", value, e);
        }

        return null;
    }

    @Override
    public boolean isResponsibleForSubject(Field field) {
        return true;
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