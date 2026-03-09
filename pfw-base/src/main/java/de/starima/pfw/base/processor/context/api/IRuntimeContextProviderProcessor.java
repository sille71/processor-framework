package de.starima.pfw.base.processor.context.api;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.api.IProcessorProvider;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.parameter.api.IBeanTypeMapProcessor;
import de.starima.pfw.base.processor.parameter.api.IParameterProviderProcessor;

import java.util.List;
import java.util.Map;

@Processor
public interface IRuntimeContextProviderProcessor extends IProcessor {

    public IProcessorContext createContext(IContextCreationContext creationContext);

    /**
     * @deprecated
     * @param processor
     * @param parentCtx
     * @return
     */
    public IProcessorContext createInitialzerContext(IProcessor processor, IProcessorContext parentCtx);

    public IParameterProviderProcessor getParameterProviderProcessor();
    public IBeanTypeMapProcessor getBeanTypeMapProcessor();
    public IProcessorProvider getProcessorProvider();

    /**
     * @deprecated
     * @param parentCtx
     * @param parameterMaps
     * @return
     */
    public IProcessorContext createContext(IProcessorContext parentCtx, List<Map<String, Map<String, Object>>> parameterMaps);

    /**
     * @deprecated
     * @param parentCtx
     * @param parameterMap
     * @return
     */
    public IProcessorContext createContext(IProcessorContext parentCtx, Map<String, Map<String, Object>> parameterMap);

    public boolean isUseDefaultBeanParameterMap();
}