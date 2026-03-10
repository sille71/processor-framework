package de.starima.pfw.base.processor.parameter;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.parameter.api.IParameterChangeListener;
import de.starima.pfw.base.processor.parameter.api.IParameterProviderProcessor;
import de.starima.pfw.base.util.MapUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

@Getter
@Setter
@Processor
public abstract class AbstractParameterProviderProcessor extends AbstractProcessor implements IParameterProviderProcessor {

    @Value("${APPL_CFG}")
    protected String applicationCfgDir;

    @Override
    public void addParameterChangeListener(IParameterChangeListener listener) {
        //TODO macht das hier Sinn? Nur, wenn auf DB Ã„nderungen reagiert werden kann
    }

    @Override
    public void removeParameterChangeListener(IParameterChangeListener listener) {

    }

    @Override
    public Map<String, Object> getParameters(String identifier) {
        return getBeanParameterMap() != null ? getBeanParameterMap().get(identifier) : null;
    }

    @Override
    public void setParameters(Map<String, Object> parameters, String identifier) {

    }

    @Override
    public void setParameter(String parameterName, Object value, String identifier) {

    }

    @Override
    public Map<String, String> getStringParameters(String identifier) {
        return getBeanParameterMap() != null ? MapUtils.convertObjectMapToStringMap(getBeanParameterMap().get(identifier)) : null;
    }


    @Override
    public void setStringParameters(Map<String, String> parameters, String identifier) {

    }

    @Override
    public void setStringParameter(String parameterName, String value, String identifier) {

    }
}