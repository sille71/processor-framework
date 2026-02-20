package de.starima.pfw.base.processor.description.config.api;

//"Beschreibt eine Liste/Array von Werten."
public interface ICollectionValueConfig extends IValueConfig {
    /**
     * Gibt den Element Typ der heterogenen Liste zurÃ¼ck
     * @return
     */
    IValueConfig getElementConfig();
    void setElementConfig(IValueConfig elementConfig);
}