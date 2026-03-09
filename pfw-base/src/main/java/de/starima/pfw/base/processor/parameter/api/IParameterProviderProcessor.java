package de.starima.pfw.base.processor.parameter.api;

import java.util.Map;

import de.starima.pfw.base.processor.api.IProcessor;

public interface IParameterProviderProcessor extends IProcessor {
	void addParameterChangeListener(IParameterChangeListener listener);
	void removeParameterChangeListener(IParameterChangeListener listener);

	/**
	 * Die Methode liefert die gesamte Bean Parameter Map,
	 * @return
	 */
	Map<String, Map<String, Object>> getBeanParameterMap();

	public Map<String, Object> getParameters(String identifier);
	public void setParameters(Map<String, Object> parameters, String identifier);
	public void setParameter(String parameterName, Object value, String identifier);

	/**
	 * Liefert die Parameter zu einer definierten Bean Id.
	 * Das kann auch eine Bean Id Map sein, beispielsweise als Parameter Map fÃ¼r den IBeanTypeMapProcessor.
	 * @return
	 */
	Map<String, String> getStringParameters(String identifier);

	void setStringParameters(Map<String, String> parameters, String identifier);

	void setStringParameter(String parameterName, String value, String identifier);
}