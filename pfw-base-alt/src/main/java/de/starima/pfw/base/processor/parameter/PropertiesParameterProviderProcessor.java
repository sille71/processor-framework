package de.starima.pfw.base.processor.parameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Processor
public class PropertiesParameterProviderProcessor extends AbstractParameterProviderProcessor {

    private static final Pattern pattern = Pattern.compile("\\$\\{([^\\}]+)\\}|@(\\w[\\w\\.-]*)@");

    @Value("${projectDirectory}")
    private String projectBasedir;

    @ProcessorParameter(value = "application.properties")
    private String fileName;
    @ProcessorParameter(description = "Delimited string containing the properties to load. The structure of a property is \"{propertyType}:{propertyName}\". " +
                                    "Defined propertyTypes can be found in the PropertyType enumeration class")
    private String properties;
    @ProcessorParameter(value = ";")
    private String delimiter;

    @Override
    public Map<String, Map<String, Object>> getBeanParameterMap() {
        Map<String, Map<String, Object>> parameterMap = new HashMap<>();
        String filePath = applicationCfgDir + File.separator + fileName;
        
        try {
            Properties loadedProperties = loadProperties(filePath);
            loadedProperties.put("project.basedir", projectBasedir);
            Map<String, Object> parameters = new HashMap<>();

            if (properties == null || properties.isBlank())
                return null;
            else {
                // load specified properties
                List<String> values = Arrays.asList(properties.split(delimiter));

                for(String value : values) {
                    if (!value.contains(":"))
                        throw new IllegalArgumentException("Value \"" + value + "\" is not a valid property definition. A valid property definition is \"{propertyType}:{propertyName}\"");

                    String[] split = value.split(":");
                    if (split.length != 2)
                        throw new IllegalArgumentException("Value \"" + value + "\" is not a valid property definition. A valid property definition is \"{propertyType}:{propertyName}\"");

                    PropertyType propertyType = PropertyType.valueOf(split[0]);
                    String propertyName = split[1];

                    if (loadedProperties.containsKey(propertyName)) {
                        parameters.put(propertyType.name(), resolvePlaceholders(loadedProperties.get(propertyName).toString(), loadedProperties));
                    } else {
                        log.warn("Specified property \"{}\" is not defined in \"{}\"", propertyName, fileName);
                    }
                }
            }

            parameterMap.put(this.getIdentifier(), parameters);
            return parameterMap;
        } catch (IllegalArgumentException | IOException e) {
            log.error("Failed to parse \"{}\". Exception: ", filePath, e);
            return null;
        }
    }

    private Properties loadProperties(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            Properties applicationProperties = new Properties();
            applicationProperties.load(fis);

            return applicationProperties;
        }
    }

    private String resolvePlaceholders(String input, Properties properties) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            if (placeholder != null) {
                // check for $-placeholder
                String replacement = properties.get(placeholder).toString();
                if (replacement != null) {
                    matcher.appendReplacement(buffer, Matcher.quoteReplacement(resolvePlaceholders(replacement, properties)));
                }
            } else {
                // check for @-placeholder
                String variable = matcher.group(2);
                String replacement = properties.get(variable).toString();
                if (replacement != null) {
                    matcher.appendReplacement(buffer, Matcher.quoteReplacement(resolvePlaceholders(replacement, properties)));
                }
            }
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }
}