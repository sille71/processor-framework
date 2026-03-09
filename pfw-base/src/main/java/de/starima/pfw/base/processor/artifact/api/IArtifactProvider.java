package de.starima.pfw.base.processor.artifact.api;

import de.starima.pfw.base.processor.api.IProcessor;

public interface IArtifactProvider extends IProcessor {
    <T> T getArtifact(Class<T> clazz);
    <T> void setArtifact(Class<T> clazz, T artifact);
}