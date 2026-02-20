package de.starima.pfw.base.processor.condition;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.attribute.api.IAttribute;
import de.starima.pfw.base.processor.attribute.api.IAttributeHelperProcessor;
import de.starima.pfw.base.processor.condition.api.ICondition;
import de.starima.pfw.base.processor.condition.api.IConditionProcessor;
import de.starima.pfw.base.processor.condition.domain.ConditionConfig;
import de.starima.pfw.base.processor.formatter.api.IFormatterProviderProcessor;
import de.starima.pfw.base.processor.locale.api.ILocalProviderProcessor;
import de.starima.pfw.base.processor.variable.api.IVariableProcessor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.text.ParseException;

@Setter
@Getter
@Processor
public class DefaultAttributeConditionProcessor extends AbstractProcessor implements IConditionProcessor<IAttribute> {
    @ProcessorParameter
    private ICondition<ConditionConfig> condition;
    @ProcessorParameter
    private IAttributeHelperProcessor<IAttribute, ConditionConfig> attributeHelperProcessor;
    @ProcessorParameter
    private IFormatterProviderProcessor formatterProviderProcessor;
    @ProcessorParameter
    private ILocalProviderProcessor localProviderProcessor;
    @ProcessorParameter
    private String description;
    @ProcessorParameter
    private IVariableProcessor variableProcessor;

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public IAttribute process(IAttribute rcnAttribute) throws Exception {
        if (this.condition == null) return rcnAttribute;
        if (this.condition.getConditionConfigs() == null || this.condition.getConditionConfigs().isEmpty()) return rcnAttribute;
        if (rcnAttribute == null) return null;
        // assume logical AND between the conditions
        boolean check = false;

        for (ConditionConfig cfg : this.condition.getConditionConfigs()) {
            if (!checkConditionConfig(cfg, rcnAttribute)) return null;
        }
        return rcnAttribute;
    }

    private boolean checkConditionConfig(ConditionConfig cfg, IAttribute rcnAttribute) {
        Assert.notNull(this.attributeHelperProcessor, "No attributeHelperProcessor defined!");
        if (cfg.isSystemVariable() && this.variableProcessor != null) {
                IAttribute sysvariable = null;
                try {
                    sysvariable = this.variableProcessor.getSystemVariable(cfg.getSystemVariableName());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return this.attributeHelperProcessor.compareAttributes(rcnAttribute, sysvariable, cfg);
        }


        if (cfg.getColumnName() != null && cfg.getValue() != null && this.formatterProviderProcessor != null && this.formatterProviderProcessor.getFormatterForSubject(rcnAttribute) != null && cfg.getColumnName().equalsIgnoreCase(rcnAttribute.getName())) {
                try {
                    return this.attributeHelperProcessor.compareAttribute(rcnAttribute, this.formatterProviderProcessor.getFormatterForSubject(rcnAttribute).parse(cfg.getValue(), this.localProviderProcessor.getLocaleForSubject(rcnAttribute)), cfg);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
        }

        return false;
    }
}