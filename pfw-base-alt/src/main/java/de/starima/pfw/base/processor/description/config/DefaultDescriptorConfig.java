package de.starima.pfw.base.processor.description.config;

import de.starima.pfw.base.domain.ProcessorScope;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfig;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter @Setter
public class DefaultDescriptorConfig implements IDescriptorConfig {
    private String descriptorPrototypeIdentifier;
    private String descriptorIdentifier;
    private ProcessorScope descriptorScope;

    private String description;
    private String descriptionAssetName;
    private String descriptionAssetMimetype;
    private String assetProviderIdentifier;
    private Map<String, Object> parameters;
    private Map<String, Map<String, Object>> descriptorBeanParameterMap;

}