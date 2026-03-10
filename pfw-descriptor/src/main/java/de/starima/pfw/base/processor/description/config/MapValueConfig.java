package de.starima.pfw.base.processor.description.config;

import de.starima.pfw.base.processor.description.config.api.IMapValueConfig;
import de.starima.pfw.base.processor.description.config.api.IValueConfig;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MapValueConfig extends ValueConfig implements IMapValueConfig {
    private IValueConfig keyElementConfig;
    private IValueConfig valueElementConfig;
}