package de.starima.pfw.base.processor.description.config;

import de.starima.pfw.base.processor.description.config.api.ICollectionValueConfig;
import de.starima.pfw.base.processor.description.config.api.IValueConfig;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CollectionValueConfig extends ValueConfig implements ICollectionValueConfig {
    private IValueConfig elementConfig;
}