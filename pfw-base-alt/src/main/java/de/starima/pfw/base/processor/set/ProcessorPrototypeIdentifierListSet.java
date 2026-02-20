package de.starima.pfw.base.processor.set;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.description.ProcessorListValueFunction;
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
public class ProcessorPrototypeIdentifierListSet extends SimpleTypeSet implements ISetProcessor<Object> {
    public static boolean isResponsibleFor(Field field) {
        return ProcessorListValueFunction.isResponsibleFor(field);
    }
    @ProcessorParameter(description = "Trenner der Identifier", value = ",")
    private String delimiter = ",";

    @Override
    public double getMemberShip(Object element) {
        if (element instanceof String && element.toString().split(delimiter).length > 1) {
            return getMemberShip(Arrays.asList(element.toString().split(delimiter)));
        }
        if (element instanceof String && getMembers() != null) return getMembers().contains(ProcessorUtils.getPrototypeId(element.toString())) ? 1 : 0;
        if (element instanceof List<?>) {
            double membership = 0;
            double min = 1;
            for (Object e : (List<?>)element) {
                membership = getMemberShip(e);
                min = membership < min ? membership : min;
            }
            return min;
        }
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

    //TODO: noch zu Ã¼berarbeiten, hier wird genau genommen nur ein Element zurÃ¼ckgegeben, da es eine Menge von Teilmengen ist.
    //Es sit noch eine geeignete Methode zu definieren, die dem ReconLight mitteilt, dass eine Mehrfachauswahl getroffen werden kann.
    @Override
    public List<Object> getMembers() {
        return new ArrayList<Object>(Arrays.asList(getRuntimeContext().getProcessorProvider().getBeanIdentifiers(getType())));
    }
}