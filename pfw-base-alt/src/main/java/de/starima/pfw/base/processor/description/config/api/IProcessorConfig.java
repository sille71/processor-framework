package de.starima.pfw.base.processor.description.config.api;

public interface IProcessorConfig extends IValueObjectConfig {
    @Override
    default MetaType getMetaType() {return MetaType.PROCESSOR;}
}