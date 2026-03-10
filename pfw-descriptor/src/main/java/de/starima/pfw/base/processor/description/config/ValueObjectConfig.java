package de.starima.pfw.base.processor.description.config;

import de.starima.pfw.base.processor.description.config.api.IProcessorParameterConfig;
import de.starima.pfw.base.processor.description.config.api.IValueObjectConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter @Setter
public class ValueObjectConfig extends ValueConfig implements IValueObjectConfig {
    private String prototypeIdentifier;
    private String identifier;
    private Map<String, Object> defaultValues;
    private List<String> categories;
    private List<String> subCategories;
    private List<String> tags;
    private Map<String, IProcessorParameterConfig> parameterConfigs;
    private String defaultBeanParameterMapFileName;
}