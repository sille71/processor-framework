package de.starima.pfw.base.processor.parameter;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.parameter.api.IParameterChangeListener;
import de.starima.pfw.base.processor.parameter.api.IParameterProviderProcessor;
import de.starima.pfw.base.util.MapUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Processor(description = "VerknÃ¼pft mehrere ParameterProvider. Die Parametermaps der einzelnen Provider werden zu einer Map verschmolzen. Dabei Ã¼berschreiben " +
        "Parameter der nachfolgenden Prozessoren die Parameter der vorher definierten Prozessoren.")
public class DefaultParameterProviderChainProcessor extends AbstractParameterProviderProcessor {
    @ProcessorParameter(value = "processorProviderParameterProcessor", description = "Komma separierte Liste von IParameterProviderProcessor. " +
            "Die Parameter der zuerst definierten werden durch die Parameter der folgenden Prozessoren Ã¼berschrieben.")
    private List<IParameterProviderProcessor> parameterProviderProcessors;

    public List<IParameterProviderProcessor> getParameterProviderProcessors() {
        return parameterProviderProcessors;
    }

    public void setParameterProviderProcessors(List<IParameterProviderProcessor> parameterProviderProcessors) {
        this.parameterProviderProcessors = parameterProviderProcessors;
    }

    @Override
    public void addParameterChangeListener(IParameterChangeListener listener) {
        if (getParameterProviderProcessors() == null) return;
        getParameterProviderProcessors().forEach(processor -> processor.addParameterChangeListener(listener));
    }

    @Override
    public void removeParameterChangeListener(IParameterChangeListener listener) {
        if (getParameterProviderProcessors() == null) return;
        getParameterProviderProcessors().forEach(processor -> processor.removeParameterChangeListener(listener));
    }

    @Override
    public Map<String, Map<String, Object>> getBeanParameterMap() {
        Map<String, Map<String, Object>> beanIdParameterMap = new HashMap<>();
        if (getParameterProviderProcessors() == null) return beanIdParameterMap;
        getParameterProviderProcessors().forEach(processor ->
            MapUtils.mergeBeanIdParameterMap(beanIdParameterMap, processor.getBeanParameterMap()));
        return beanIdParameterMap;
    }

    @Override
    public Map<String, String> getStringParameters(String identifier) {
        HashMap<String, String> parameters = new HashMap<>();
        if (getParameterProviderProcessors() == null) return parameters;
        getParameterProviderProcessors().forEach(processor -> MapUtils.mergeMaps(parameters, processor.getStringParameters(identifier)));
        return parameters;
    }

    @Override
    public void setStringParameters(Map<String, String> parameters, String identifier) {
        if (getParameterProviderProcessors() == null) return;
        getParameterProviderProcessors().forEach(processor -> processor.setStringParameters(parameters, identifier));
    }

    @Override
    public void setStringParameter(String parameterName, String value, String identifier) {
        if (getParameterProviderProcessors() == null) return;
        getParameterProviderProcessors().forEach(processor -> processor.setStringParameter(parameterName, value, identifier));
    }

    @Override
    public Map<String, Object> getParameters(String identifier) {
        HashMap<String, Object> parameters = new HashMap<>();
        if (getParameterProviderProcessors() == null) return parameters;
        getParameterProviderProcessors().forEach(processor -> MapUtils.mergeMaps(parameters, processor.getParameters(identifier)));
        return parameters;
    }

    @Override
    public void setParameters(Map<String, Object> parameters, String identifier) {
        if (getParameterProviderProcessors() == null) return;
        getParameterProviderProcessors().forEach(processor -> processor.setParameters(parameters, identifier));
    }

    @Override
    public void setParameter(String parameterName, Object value, String identifier) {
        if (getParameterProviderProcessors() == null) return;
        getParameterProviderProcessors().forEach(processor -> processor.setParameter(parameterName, value, identifier));
    }
}