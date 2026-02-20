package de.starima.pfw.base.processor.condition.domain;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import de.starima.pfw.base.processor.condition.api.ICondition;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Condition implements ICondition<ConditionConfig> {
    private String identifier;
    private String description;
    private List<ConditionConfig> conditionConfigs;

    @XmlElementWrapper(name = "conditionConfigs")
	@XmlElement(name = "cfg")
	public List<ConditionConfig> getConditionConfigs() {
		return conditionConfigs;
	}
}