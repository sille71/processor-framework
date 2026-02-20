package de.starima.pfw.base.processor.attribute.domain;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.attribute.api.IAttribute;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Ein Beispiel fÃ¼r eine Attribut Definition als Prozessor. Das hat den Vorteil, dass die Initiierung nach dem Prozessorkonzept erfolgt.
 */

@Getter
@Setter
@Processor
public class RcnAttributeProcessor extends AbstractProcessor implements IAttribute {
    @ProcessorParameter
    private String name;
    @ProcessorParameter
    private String type;
    @ProcessorParameter
    private String sortDirection;
    @ProcessorParameter
    private Integer sortOrder;
    @ProcessorParameter
    private Boolean sortable;
    @ProcessorParameter
    private String defaultLabel;
    @ProcessorParameter
    private String pattern;
    @ProcessorParameter
    private Integer sqlType;
    @ProcessorParameter(parameterFunctionProcessorPrototypeIdentifier = "jacksonParameterFunctionProcessor")
    private Map<String,String> labelMap;
    @ProcessorParameter(parameterFunctionProcessorPrototypeIdentifier = "jacksonParameterFunctionProcessor")
    private Object value;
    @ProcessorParameter
    private Boolean numeric;
    @ProcessorParameter
    private Boolean date;
    @ProcessorParameter
    private Boolean string;
    @ProcessorParameter
    private Boolean timestamp;
    @ProcessorParameter
    private String language;


    @Override
    public String getDefaultLabel() {
        return defaultLabel != null ? defaultLabel : getName();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public Boolean isSortable() {
        return sortable;
    }

    @Override
    public Boolean isNumeric() {
        return this.numeric;
    }

    @Override
    public Boolean isDate() {
        return this.date;
    }

    @Override
    public Boolean isTimestamp() {
        return this.timestamp;
    }

    @Override
    public Boolean isString() {
        return this.string;
    }
}