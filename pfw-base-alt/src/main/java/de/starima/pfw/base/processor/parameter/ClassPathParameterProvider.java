package de.starima.pfw.base.processor.parameter;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.migration.GlobalConfigurationMigrationProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Processor
public class ClassPathParameterProvider extends AbstractParameterProviderProcessor {
    @Override
    public Map<String, Map<String, Object>> getBeanParameterMap() {
        try {
            InputStream input = GlobalConfigurationMigrationProcessor.class.getResourceAsStream(getIdentifier() + ".json");
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