package de.starima.pfw.base.processor.attribute.api;

import de.starima.pfw.base.processor.api.IProcessor;

public interface IAttributeProviderSelectorProcessor<A extends IAttribute> extends IProcessor {
	public IAttributeProviderProcessor<A> getAttributeProviderForSubjectReference(String subjectReference);
	public IAttributeProviderProcessor<A> getAttributeProviderForSubject(Object subject);

	public boolean isResponsibleForSubject(Object subject);
}