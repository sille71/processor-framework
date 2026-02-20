package de.starima.pfw.base.processor.context;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.IContextCreationContext;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.context.domain.DefaultProcessorContext;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.api.IProcessorProvider;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.IRuntimeContextProviderProcessor;
import de.starima.pfw.base.processor.parameter.api.IBeanTypeMapProcessor;
import de.starima.pfw.base.processor.parameter.api.IParameterProviderProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter @Setter
@Slf4j
@Processor
public class DefaultRuntimeContextProviderProcessor extends AbstractProcessor implements IRuntimeContextProviderProcessor {
    @ProcessorParameter(description = "Name des zu erstellenden Kontextes. Wird keiner angegeben, so wird der Identifier dieses Prozessors verwendet.")
    private String name;
    @ProcessorParameter
    private IParameterProviderProcessor parameterProviderProcessor;
    @ProcessorParameter(value = "defaultProcessorProvider")
    private IProcessorProvider processorProvider;
    @ProcessorParameter(value = "defaultBeanTypeMapProcessor")
    private IBeanTypeMapProcessor beanTypeMapProcessor;
    @ProcessorParameter(value = "true")
    private boolean useDefaultBeanParameterMap = true;

    private String getContextName() {
        return this.name != null ? this.name + UUID.randomUUID() : getIdentifier() + UUID.randomUUID();
    }

    @Override
    public IProcessorContext createContext(IContextCreationContext creationContext) {
        if (creationContext == null)
            return createNewContext(null);



        IProcessorContext newCtx = createNewContext(parentCtx);
        if (this.parameterProviderProcessor != null) {
            newCtx.setBeanParameterMap(this.parameterProviderProcessor.getBeanParameterMap());
        }
        return newCtx;
    }

    protected IRuntimeContextProviderProcessor createContextProvider(IContextCreationContext creationContext) {

    }

    protected IProcessorContext createNewContext(IProcessorContext parentCtx) {
        IProcessorContext newCtx = new DefaultProcessorContext();
        newCtx.setName(getContextName());
        if (parentCtx != null) parentCtx.addReconContext(newCtx);
        newCtx.setContextProviderProcessor(this);
        return newCtx;
    }

    @Override
    public IParameterProviderProcessor getParameterProviderProcessor() {
        return this.parameterProviderProcessor;
    }

    @Override
    public IProcessorProvider getProcessorProvider() {
        return this.processorProvider;
    }

    @Override
    public IBeanTypeMapProcessor getBeanTypeMapProcessor() {
        return this.beanTypeMapProcessor;
    }

    @Override
    public IProcessorContext createContext(IProcessorContext parentCtx, List<Map<String, Map<String, Object>>> parameterMaps) {
        if (parameterMaps == null || parameterMaps.isEmpty()) {
            return parentCtx != null ? parentCtx : this.createContext(null);
        }
        IProcessorContext newCtx = parentCtx;
        for (Map<String, Map<String, Object>> pm : parameterMaps) {
            newCtx = createContext(newCtx, pm);
        }

        return newCtx;
    }

    @Override
    public IProcessorContext createContext(IProcessorContext parentCtx, Map<String, Map<String, Object>> parameterMap) {
        if (parameterMap == null || parameterMap.isEmpty()) {
            return parentCtx != null ? parentCtx : this.createContext(null);
        }

        IProcessorContext newCtx = createNewContext(parentCtx);
        //wir benutzen hier nicht den ParameterProvider, sondern die Ã¼bergebenen Parameter, um den neuen Kontext mit der ParameterMap
        // auszustatten
        newCtx.setBeanParameterMap(parameterMap);
        //falls ein BeanTypeMap Processor vorhanden ist, benutzen wir die Methode getBeanIdTypeMapFromParameterMap, um die BeanIdType Map des neuen Prozessors zu setzen
        //Das benÃ¶tigen wir beispielsweise beim Deployment von Recon Konfigurationen, dort steckt der Deployment Prozessor in der Konfig
        if (this.getBeanTypeMapProcessor() != null) {
            log.info("{}: get beanIdTypeMap from parameterMap with beanTypeMap processor {}", getIdentifier(), this.getBeanTypeMapProcessor().getIdentifier());
            newCtx.setBeanIdTypeMap(this.getBeanTypeMapProcessor().getBeanIdTypeMapFromParameterMap(parameterMap));
        }
        return newCtx;
    }

    public IProcessorContext createInitialzerContext(IProcessor processor, IProcessorContext parentCtx) {
        DefaultProcessorContext iCtx = new DefaultProcessorContext();
        iCtx.setName("init-" + getContextName());
        iCtx.setInitializedProcessor(processor);
        if (parentCtx != null) parentCtx.addReconContext(iCtx);
        iCtx.setContextProviderProcessor(this);

        return iCtx;
    }
}