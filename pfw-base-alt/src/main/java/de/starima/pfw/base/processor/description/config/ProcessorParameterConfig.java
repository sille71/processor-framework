package de.starima.pfw.base.processor.description.config;

import de.starima.pfw.base.processor.description.config.api.IProcessorParameterConfig;
import de.starima.pfw.base.processor.description.config.api.IValueConfig;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class ProcessorParameterConfig extends DefaultDescriptorConfig implements IProcessorParameterConfig {
    private String propertyName;
    private String parameterName;
    private Object parameterDefaultValue;
    private boolean key;
    private List<String> aliases;
    private boolean ignoreInitialization;
    private boolean ignoreRefresh;
    private boolean ignoreExtractParameter;
    private boolean required;
    private boolean input;
    private boolean output;
    private IValueConfig valueConfig;
}