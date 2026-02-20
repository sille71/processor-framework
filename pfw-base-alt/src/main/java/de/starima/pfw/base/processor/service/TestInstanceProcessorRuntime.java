package de.starima.pfw.base.processor.service;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.DefaultProcessorProvider;
import de.starima.pfw.base.processor.api.IProcessorProvider;
import de.starima.pfw.base.processor.parameter.FileSystemParameterProviderProcessor;
import de.starima.pfw.base.processor.parameter.api.IParameterProviderProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Slf4j
@Setter @Getter
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Service
@Processor
public class TestInstanceProcessorRuntime extends AbstractInstanceProcessorRuntime implements ApplicationContextAware {
    private ApplicationContext applicationContext;
    @Autowired
    private DefaultProcessorProvider processorProvider;
    @Autowired private FileSystemParameterProviderProcessor parameterProviderProcessor;

    @ProcessorParameter
    private String testParameter;

    @Override
    public IParameterProviderProcessor getParameterProviderProcessor() {
        return this.parameterProviderProcessor;
    }

    @Override
    public IProcessorProvider getProcessorProvider() {
        return this.processorProvider;
    }
}