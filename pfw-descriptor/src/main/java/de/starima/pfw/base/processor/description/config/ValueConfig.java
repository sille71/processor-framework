package de.starima.pfw.base.processor.description.config;

import de.starima.pfw.base.processor.description.config.api.ITypeRef;
import de.starima.pfw.base.processor.description.config.api.IValueConfig;
import de.starima.pfw.base.processor.description.config.api.IValueFunctionConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ValueConfig extends DefaultDescriptorConfig implements IValueConfig {
    private List<String> requiredCategories;
    private List<String> requiredSubCategories;
    private List<String> requiredTags;
    private ITypeRef typeRef;
    private Class<?> targetType;
    private IValueFunctionConfig functionConfig;
}