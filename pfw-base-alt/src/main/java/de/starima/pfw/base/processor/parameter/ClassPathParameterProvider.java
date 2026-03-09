package de.starima.pfw.base.processor.parameter;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.starima.pfw.base.annotation.Processor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Processor
public class ClassPathParameterProvider extends AbstractParameterProviderProcessor {
    @Override
    public Map<String, Map<String, Object>> getBeanParameterMap() {
        //TODO: ist noch flexibler zu implementieren (evtl. mit Class als ProcessorParameter
        try {
            InputStream input = this.getClass().getResourceAsStream(getIdentifier() + ".json");
            if (input == null) return null;
            ObjectMapper mapper = new ObjectMapper();
            Map<String,Map<String, Object>> map = mapper.readValue(input, mapper.getTypeFactory().constructMapType(HashMap.class, String.class, HashMap.class));
            return map;
        } catch (Exception e) {
            log.warn("Can not get bean parameter map for {}", getIdentifier(), e);
        }
        return null;
    }
}