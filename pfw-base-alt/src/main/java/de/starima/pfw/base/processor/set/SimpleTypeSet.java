package de.starima.pfw.base.processor.set;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.context.domain.DefaultTransformationContext;
import de.starima.pfw.base.processor.description.BooleanValueFunction;
import de.starima.pfw.base.processor.description.DoubleValueFunction;
import de.starima.pfw.base.processor.description.IntegerValueFunction;
import de.starima.pfw.base.processor.set.api.ISetProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Slf4j
@Processor
public class SimpleTypeSet extends AbstractProcessor implements ISetProcessor<Object> {
    public static boolean isResponsibleFor(Field field) {
        DefaultTransformationContext context = new DefaultTransformationContext();
        context.setTargetField(field);
        return IntegerValueFunction.isResponsibleFor(context)
                || DoubleValueFunction.isResponsibleFor(context)
                || BooleanValueFunction.isResponsibleFor(context);
    }
    @ProcessorParameter
    private String domainName;
    @ProcessorParameter
    private String type;

    @Override
    public double getMemberShip(Object element) {
        if (type == null) return 0;
        try {
            return Class.forName(type).isAssignableFrom(element.getClass()) ? 1 : 0;
        } catch (ClassNotFoundException e) {
            log.error("{} can not find class for name {}",getIdentifier(), type);
        }

        return 0;
    }

    @Override
    public boolean isMember(Object element) {
        return getMemberShip(element) == 1;
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public List<Object> getMembers() {
        return new ArrayList<Object>();
    }

    @Override
    public String getDomainName() {
        if (domainName == null) domainName = type;
        return domainName;
    }
}