package de.starima.pfw.base.processor.condition.domain;

import de.starima.pfw.base.processor.condition.api.ICondition;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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