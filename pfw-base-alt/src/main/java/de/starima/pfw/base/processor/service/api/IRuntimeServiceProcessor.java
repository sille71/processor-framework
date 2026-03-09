package de.starima.pfw.base.processor.service.api;

import de.starima.pfw.base.processor.context.api.IRuntimeContextProviderProcessor;

public interface IRuntimeServiceProcessor {
    Object processRequest(Object request);
}