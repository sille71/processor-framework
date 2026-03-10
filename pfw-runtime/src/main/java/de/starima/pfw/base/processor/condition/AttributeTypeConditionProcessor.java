package de.starima.pfw.base.processor.condition;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.attribute.api.IAttribute;
import de.starima.pfw.base.processor.condition.api.IConditionProcessor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Processor
public class AttributeTypeConditionProcessor extends AbstractProcessor implements IConditionProcessor<IAttribute> {
    @ProcessorParameter
    private String description;
    @ProcessorParameter(value = "char")
    private String type = "char";

    @Override
    public IAttribute process(IAttribute attribute) throws Exception {
        if (attribute == null) return null;

        return type.equalsIgnoreCase(attribute.getType()) ? attribute : null;
    }
}