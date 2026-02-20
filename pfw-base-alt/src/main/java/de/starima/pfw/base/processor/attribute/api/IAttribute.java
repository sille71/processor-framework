package de.starima.pfw.base.processor.attribute.api;

import java.util.Map;

public interface IAttribute {
    public void setName(String name);
    public void setType(String type);
    public void setValue(Object value);
    public void setSortable(Boolean flag);
    public String getName();
    public String getType();
    public Integer getSqlType();
    public void setSqlType(Integer type);
    public String getSortDirection();
    public void setSortDirection(String direction);
    public Integer getSortOrder();
    public void setSortOrder(Integer order);
    public Boolean isSortable();
    public Map<String,String> getLabelMap();
    public Object getValue();
    public Boolean isNumeric();
    public Boolean isDate();
    public Boolean isTimestamp();
    public Boolean isString();
    public String getLanguage();
    public void setLanguage(String language);
    public String getPattern();
    public void setPattern(String pattern);


    //default Methoden

    public default String getDefaultLabel() {
        return getName();
    }

    public default String getLabel(String language) {
        return getLabelMap() != null ? getLabelMap().get(language) : getDefaultLabel();
    }
}