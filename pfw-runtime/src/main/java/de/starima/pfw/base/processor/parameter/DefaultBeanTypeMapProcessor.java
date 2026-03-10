package de.starima.pfw.base.processor.parameter;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.parameter.api.IBeanTypeMapProcessor;
import de.starima.pfw.base.util.MapUtils;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Processor
@Getter
@Setter
public class DefaultBeanTypeMapProcessor extends AbstractProcessor implements IBeanTypeMapProcessor {
    @ProcessorParameter
    private Map<String, String> beanIdTypeMap;

    @Override
    public String getBeanIdForType(String type) {
        return this.beanIdTypeMap != null ? this.beanIdTypeMap.get(type) : null;
    }

    @Override
    public Map<String, String> getBeanIdTypeMap() {
        return this.beanIdTypeMap;
    }

    @Override
    public Map<String, String> getContextMergedBeanIdTypeMap() {
        if (getRuntimeContext() != null) {
            return MapUtils.mergeMaps(MapUtils.mergeMaps(new HashMap<>(), getRuntimeContext().getContextMergedBeanIdTypeMap()), getBeanIdTypeMap());
        }
        return getBeanIdTypeMap();
    }

    @Override
    public Map<String, String> getBeanIdTypeMapFromParameterMap(Map<String, Map<String, Object>> parameterMap) {
        if (parameterMap == null) return null;
        //TODO: refreshBeanParameters sollte eine Eigenschaft des Prozessors sein (wie init) und entweder von ProcessorUtils oder dem ProcessorDescriptor Ã¼bernommen werden.
        ProcessorUtils.refreshBeanParameters(this, parameterMap.get(getIdentifier()), getRuntimeContext());
        return getBeanIdTypeMap();
    }

    @Override
    public Map<String, String> getContextMergedBeanIdTypeMapFromParameterMap(Map<String, Map<String, Object>> parameterMap) {
        if (getRuntimeContext() != null) {
            return MapUtils.mergeMaps(getContextMergedBeanIdTypeMap(), getBeanIdTypeMapFromParameterMap(parameterMap));
        }
        return getBeanIdTypeMapFromParameterMap(parameterMap);
    }


}