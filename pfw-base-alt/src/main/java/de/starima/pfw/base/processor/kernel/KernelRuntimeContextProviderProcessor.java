package de.starima.pfw.base.processor.kernel;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.DefaultProcessorProvider;
import de.starima.pfw.base.processor.api.IProcessorProvider;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.IContextCreationContext;
import de.starima.pfw.base.processor.context.api.IRuntimeContextProviderProcessor;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.context.domain.DefaultProcessorContext;
import de.starima.pfw.base.processor.parameter.DefaultBeanTypeMapProcessor;
import de.starima.pfw.base.processor.parameter.FileSystemParameterProviderProcessor;
import de.starima.pfw.base.processor.parameter.api.IBeanTypeMapProcessor;
import de.starima.pfw.base.processor.parameter.api.IParameterProviderProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Slf4j
@Processor(description = "Der Kernel ContextProvider. Wird durch Spring instanziiert!")
public class KernelRuntimeContextProviderProcessor extends AbstractProcessor implements IRuntimeContextProviderProcessor {
    //Wir annotieren trotzdem obwohl diese Attribute im Konstruktor gesetzt werden!
    //Dadurch kÃ¶nnen wir den Prozessor spÃ¤ter beschreiben.
    @ProcessorParameter(description = "Der ParameterProvider fÃ¼r diesen ContextProvider. Er wird explizit Ã¼ber den Konstruktor gesetzt. Per default schaut er auf das beanParameter File unter <APPL_CFG>/beanParameters_FileSystemParameterProviderProcessor.json", ignoreInitialization = true)
    private IParameterProviderProcessor parameterProviderProcessor;
    @ProcessorParameter(description = "Der ProcessorProvider fÃ¼r diesen ContextProvider. Er wird explizit Ã¼ber den Konstruktor gesetzt.", ignoreInitialization = true)
    private IProcessorProvider processorProvider;
    //brauchen wir den hier schon?
    @ProcessorParameter(description = "Der BeanTypMap Prozessor, der benutzt wird, um aus der beanParameterMap die beanIdTypeMap zu extrahieren und dem hier erzeugten Context zu setzen. Wird im Konstructor gesetzt!", ignoreInitialization = true)
    private IBeanTypeMapProcessor beanTypeMapProcessor;

    public KernelRuntimeContextProviderProcessor(DefaultProcessorProvider processorProvider, FileSystemParameterProviderProcessor parameterProviderProcessor, DefaultBeanTypeMapProcessor beanTypeMapProcessor) {
        this.processorProvider = processorProvider;
        this.parameterProviderProcessor = parameterProviderProcessor;
        this.beanTypeMapProcessor = beanTypeMapProcessor;
    }

    protected IProcessorContext createNewContext() {
        IProcessorContext newCtx = new DefaultProcessorContext();
        newCtx.setName("KernelContext");
        newCtx.setContextProviderProcessor(this);
        return newCtx;
    }

    @Override
    public IProcessorContext createContext(IContextCreationContext parentCtx) {
        IProcessorContext newCtx = createNewContext();
        log.info("{}: set parameters from parameter provider parameter provider {}", getIdentifier(), this.getParameterProviderProcessor().getIdentifier());
        newCtx.setBeanParameterMap(this.getParameterProviderProcessor().getBeanParameterMap());
        log.info("{}: get beanIdTypeMap from beanTypeMap processor {}", getIdentifier(), this.getBeanTypeMapProcessor().getIdentifier());
        newCtx.setBeanIdTypeMap(this.getBeanTypeMapProcessor().getBeanIdTypeMap());
        return newCtx;
    }

    @Override
    public IProcessorContext createInitialzerContext(IProcessor processor, IProcessorContext parentCtx) {
        throw new UnsupportedOperationException("Not supported in kernel context provider.");
    }

    @Override
    public IParameterProviderProcessor getParameterProviderProcessor() {
        return null;
    }

    @Override
    public IBeanTypeMapProcessor getBeanTypeMapProcessor() {
        return null;
    }

    @Override
    public IProcessorProvider getProcessorProvider() {
        return null;
    }

    @Override
    public IProcessorContext createContext(IProcessorContext parentCtx, List<Map<String, Map<String, Object>>> parameterMaps) {
        throw new UnsupportedOperationException("Not supported in kernel context provider.");
    }

    @Override
    public IProcessorContext createContext(IProcessorContext parentCtx, Map<String, Map<String, Object>> parameterMap) {
        throw new UnsupportedOperationException("Not supported in kernel context provider.");
    }

    @Override
    public boolean isUseDefaultBeanParameterMap() {
        return false;
    }
}