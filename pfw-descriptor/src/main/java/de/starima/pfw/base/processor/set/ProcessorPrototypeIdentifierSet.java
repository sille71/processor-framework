package de.starima.pfw.base.processor.set;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.context.domain.DefaultTransformationContext;
import de.starima.pfw.base.processor.description.ProcessorValueFunction;
import de.starima.pfw.base.processor.set.api.ISetProcessor;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@Processor
public class ProcessorPrototypeIdentifierSet extends AbstractProcessor implements ISetProcessor<Object> {
    public static boolean isResponsibleFor(Field field) {
        DefaultTransformationContext context = new DefaultTransformationContext();
        context.setFieldToResolve(field);
        return ProcessorValueFunction.isResponsibleFor(context);
    }

    @ProcessorParameter
    private String domainName;
    @ProcessorParameter
    private String type;

    @Override
    public double getMemberShip(Object element) {
        if (element instanceof String && getMembers() != null) return getMembers().contains(ProcessorUtils.getPrototypeId(element.toString())) ? 1 : 0;
        return 0;
    }

    @Override
    public boolean isMember(Object element) {
        return getMemberShip(element) == 1;
    }

    @Override
    public boolean isCollection() {
        return true;
    }

    @Override
    public List<Object> getMembers() {
        return new ArrayList<Object>(Arrays.asList(getRuntimeContext().getProcessorProvider().getBeanIdentifiers(type)));
    }

    @Override
    public String getDomainName() {
        if (domainName == null) domainName = type;
        return domainName;
    }
}