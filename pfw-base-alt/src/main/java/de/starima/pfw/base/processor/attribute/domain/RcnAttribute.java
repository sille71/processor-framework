package de.starima.pfw.base.processor.attribute.domain;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.annotation.ValueObject;
import de.starima.pfw.base.processor.attribute.api.IAttribute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Builder
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@ValueObject()
public class RcnAttribute implements IAttribute {
    @ProcessorParameter(key = true)
    private String name;
    @ProcessorParameter()
    private String type;
    @ProcessorParameter()
    private Integer precision;
    @ProcessorParameter(value = "asc")
    private String sortDirection = "asc";
    @ProcessorParameter(value = "0")
    private Integer sortOrder = 0;
    @ProcessorParameter()
    private Boolean sortable;
    @ProcessorParameter()
    private String defaultLabel;
    @ProcessorParameter()
    private Integer sqlType;
    @ProcessorParameter()
    private Map<String,String> labelMap;
    @ProcessorParameter()
    private Object value;
    @ProcessorParameter()
    private Boolean numeric;
    @ProcessorParameter()
    private Boolean date;
    @ProcessorParameter()
    private Boolean string;
    @ProcessorParameter()
    private Boolean timestamp;
    @ProcessorParameter()
    private String language;
    @ProcessorParameter()
    private String pattern;


    @Override
    public Boolean isSortable() {
        return sortable;
    }

    @Override
    public Boolean isNumeric() {
        return numeric;
    }

    @Override
    public Boolean isDate() {
        return date;
    }

    @Override
    public Boolean isTimestamp() {
        return timestamp;
    }

    @Override
    public Boolean isString() {
        return string;
    }

    @Override
    public String getDefaultLabel() {
        return defaultLabel != null ? defaultLabel : getName();
    }

    @Override
    public String toString() {
        return getName();
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection == null ? "asc" : sortDirection;
    }
}