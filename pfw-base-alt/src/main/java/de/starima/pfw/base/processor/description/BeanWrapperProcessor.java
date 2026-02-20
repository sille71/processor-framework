package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Getter @Setter
@Slf4j
@Processor(description = "Wrapper Prozessor zum Bereitstellen von gewÃ¶hnlichen beans als Parameter. Damit kÃ¶nnen dann auch diese mit einem Descriptor versehen werden (siehe DefaultBeanProvider)")
public class BeanWrapperProcessor extends AbstractProcessor {
    private Object bean;

    @Override
    protected void initParameters(Map<String, Object> parameters) {
        if (this.processorDescriptor != null) {
            log.trace("Use descriptor {} to init parameters for processor {}", this.processorDescriptor.getFullBeanId(), this.getFullBeanId());
            this.processorDescriptor.initBeanParameters(this, parameters);
            this.processorDescriptor.initBeanParameters(this.bean, parameters);
        } else {
            ProcessorUtils.initBeanParameters(this, parameters, this.getRuntimeContext());
            ProcessorUtils.initBeanParameters(this.bean, parameters, this.getRuntimeContext());
        }
    }
}