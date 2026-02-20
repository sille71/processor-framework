package de.starima.pfw.base.processor.description.config.api;

import de.starima.pfw.base.annotation.Processor;

@Processor(description = "Beschreibt eine Map von Werten.")
public interface IMapValueConfig extends IValueConfig {
    /**
     * Gibt den Key Typ der Map zurÃ¼ck
     * @return
     */
    IValueConfig getKeyElementConfig();
    void setKeyElementConfig(IValueConfig elementConfig);

    /**
     * Gibt den Value Typ der Map zurÃ¼ck
     * @return
     */
    IValueConfig getValueElementConfig();
    void setValueElementConfig(IValueConfig elementConfig);
}