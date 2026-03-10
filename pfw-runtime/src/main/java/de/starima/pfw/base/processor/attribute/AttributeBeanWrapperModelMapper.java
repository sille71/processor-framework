package de.starima.pfw.base.processor.attribute;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.attribute.api.IAttribute;
import de.starima.pfw.base.processor.attribute.api.IAttributeModelMapperProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.InvalidPropertyException;

@Slf4j
@Getter @Setter
@Processor
public class AttributeBeanWrapperModelMapper extends AbstractProcessor implements IAttributeModelMapperProcessor {
    @Override
    public boolean isResponsibleForSubject(IAttribute subject) {
        return true;
    }

    @Override
    public Object transformValue(IAttribute attribute, Object model) {
        if (model == null) return null;
        if (!isResponsibleForSubject(attribute)) return model;
        try {
            BeanWrapper wrapper = new BeanWrapperImpl(model);
            return wrapper.getPropertyValue(attribute.getName());
        } catch (InvalidPropertyException ipe) {
            log.trace("{}: Can not get property {} from model.", getIdentifier(), attribute);
        } catch (BeansException be) {
            log.trace("{}: Can not get property {} from model.", getIdentifier(), attribute);
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