package de.starima.pfw.base.processor.parameter.api;

import java.util.Map;

public interface IParameterChangeListener {
    public void parameterChanged(String parameterName, Object value, String identifier);
    public void parametersChanged(Map<String, Object> parameters, String identifier);
    public void parametersChanged();
}