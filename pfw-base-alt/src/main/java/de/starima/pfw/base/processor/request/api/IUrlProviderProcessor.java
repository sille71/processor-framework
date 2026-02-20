package de.starima.pfw.base.processor.request.api;

import de.starima.pfw.base.processor.api.IProcessor;

public interface IUrlProviderProcessor extends IProcessor {
    public String getUrl();
    public String getDescription();

}