package de.starima.pfw.base.domain.api;

public interface IArtifact<T> {
	T getIdentifier();
	void setIdentifier(T identifier);
}