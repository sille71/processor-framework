package de.starima.pfw.base.processor.service;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.domain.ProcessorScope;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.parameter.api.IBeanTypeMapProcessor;
import de.starima.pfw.base.processor.request.api.IRequestDispatcherProcessor;
import de.starima.pfw.base.processor.request.api.IResponseDispatcherProcessor;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Processor
@Service
public abstract class AbstractInstanceProcessorRuntime extends AbstractProcessor {

    @ProcessorParameter(value = "defaultBeanTypeMapProcessor",
            description = "Der BeanTypeMap-Prozessor, der benutzt wird, um aus der beanParameterMap " +
                    "die beanIdTypeMap zu extrahieren und dem hier erzeugten Context zu setzen.")
    private IBeanTypeMapProcessor beanTypeMapProcessor;

    @ProcessorParameter(value = "requestDispatcherProcessorChain",
            description = "Der RequestDispatcher steuert, ob und wie die Instanz Requests verarbeiten kann.")
    private IRequestDispatcherProcessor requestDispatcherProcessor;

    @ProcessorParameter(description = "Der ResponseDispatcher kann Antworten an unterschiedliche Ziele weiterleiten.")
    private IResponseDispatcherProcessor responseDispatcherProcessor;

    @ProcessorParameter(value = "true")
    private boolean useDefaultBeanParameterMap = true;
}