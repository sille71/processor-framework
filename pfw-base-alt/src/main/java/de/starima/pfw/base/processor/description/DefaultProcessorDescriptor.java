package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.description.api.IProcessorDescriptor;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;


@Setter
@Getter
@Slf4j
@Processor()
public class DefaultProcessorDescriptor extends DefaultStructureValueDescriptor<IProcessorDescriptor> implements IProcessorDescriptor {

    public boolean isResponsibleFor(Object sourceProcessor) {
        return sourceProcessor != null && ProcessorUtils.isConsideredProcessor(sourceProcessor.getClass());
    }
}