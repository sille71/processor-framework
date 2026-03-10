package de.starima.pfw.base.processor.attribute;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.attribute.api.IAttribute;
import de.starima.pfw.base.processor.attribute.api.IAttributeModelMapperProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter @Setter
@Processor
public class AttributeModelMapperChain extends AbstractProcessor implements IAttributeModelMapperProcessor {
    @ProcessorParameter
    private List<IAttributeModelMapperProcessor> attributeModelMapperProcessors;

    @Override
    public boolean isResponsibleForSubject(IAttribute subject) {
        return true;
    }

    @Override
    public Object transformValue(IAttribute property, Object value) {
        if (attributeModelMapperProcessors == null) {
            log.warn("{}: no chain defined! Can not transform value for attribute {}", getIdentifier(),property.getName());
            return value;
        }

        Object mappedValue = value;
        for (IAttributeModelMapperProcessor mapper : attributeModelMapperProcessors) {
            mappedValue = mapper.transformValue(property, mappedValue);
        }
        return mappedValue;
    }

    @Override
    public boolean isResponsibleForInput(Object input) {
        return true;
    }

    @Override
    public Object transformValue(Object input) {
        return null;
    }
}