package de.starima.pfw.base.processor.attribute.api;

import de.starima.pfw.base.processor.api.IProcessor;

import java.util.List;

//TODO: evtl. noch 4 analoge Methoden mit dem subject als Argument hinzufÃ¼gen
public interface IAttributeProviderProcessor<A extends IAttribute> extends IProcessor {
	public A getAttribute(String attributeReference);
	public List<A> getAttributes(List<String> attributeReferences);
	public List<A> getAttributes();
	public List<String> getAttributeReferences();

	public boolean isResponsibleForSubject(Object subject);
}