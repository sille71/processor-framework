package de.starima.pfw.base.processor.transformator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.migration.domain.configuration.ReconConfiguration;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Getter
@Setter
@Slf4j
@Processor
public class ConfigTransformerProcessor extends AbstractProcessor implements IItemTransformatorProcessor<String, ReconConfiguration> {

    //cache
    private ReconConfiguration config;

    @Override
    public ReconConfiguration process(String item) throws Exception {
        if (!isResponsibleFor(item)) return null;
        return config;
    }

    @Override
    public boolean isResponsibleFor(String item) {
        try {
            config = ProcessorUtils.createReconConfigurationFromString(item);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}