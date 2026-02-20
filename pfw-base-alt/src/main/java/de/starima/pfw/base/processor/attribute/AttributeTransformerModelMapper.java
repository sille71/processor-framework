package de.starima.pfw.base.processor.attribute;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.attribute.api.IAttribute;
import de.starima.pfw.base.processor.attribute.api.IAttributeModelMapperProcessor;
import de.starima.pfw.base.processor.transformator.IItemTransformatorProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Getter @Setter
@Processor
public class AttributeTransformerModelMapper extends AbstractProcessor implements IAttributeModelMapperProcessor {
    @ProcessorParameter()
    private Map<String , IItemTransformatorProcessor<Object,Object>> attributeTransformerMap;

    @Override
    public boolean isResponsibleForSubject(IAttribute subject) {
        return true;
    }

    @Override
    public Object transformValue(IAttribute attribute, Object model) {
        if (!isResponsibleForSubject(attribute)) return model;
        if (model == null) return null;
        if (attributeTransformerMap == null) {
            log.warn("{}: Can not get value for attribute {} from model. No transformer map defined!", getIdentifier(), attribute);
            return model;
        }
        IItemTransformatorProcessor<Object,Object> transformer = attributeTransformerMap.get(attribute.getName());
        if (transformer == null) {
            log.trace("{}: Can not get value for attribute {} from model. No transformer defined in transformer map!", getIdentifier(), attribute);
            return model;
        }

        try {
            return transformer.process(model);
        } catch (Exception e) {
            log.warn("{}: Can not transform value with transformer {} for attribute {} from model.", getIdentifier(), transformer.getIdentifier(), attribute, e);
        }

        return model;
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