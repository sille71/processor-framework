package de.starima.pfw.base.processor.condition.api;

import java.util.List;

public interface ICondition<C> {
    public void setIdentifier(String identifier);
    public String getIdentifier();
    public void setDescription(String description);
    public String getDescription();
    public List<C> getConditionConfigs();
}