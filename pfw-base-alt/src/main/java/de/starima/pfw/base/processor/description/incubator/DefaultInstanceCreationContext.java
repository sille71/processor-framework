package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.domain.DefaultTaskContext;

import de.starima.pfw.base.processor.description.config.api.IDescriptorConfigProvider;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceCreationContext;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceProvider;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DefaultInstanceCreationContext extends DefaultTaskContext implements IInstanceCreationContext {
    private Type typeToResolve;
    private Object objectToResolve;
    private Field fieldToResolve;
    private ProcessorParameter processorParameter;
    private IInstanceProvider rootProvider;
    private IDescriptorConfigProvider descriptorConfigRootProvider;

    public DefaultInstanceCreationContext(IInstanceCreationContext parent) {
        super(parent);
        this.rootProvider = parent.getRootProvider();
        this.descriptorConfigRootProvider = parent.getDescriptorConfigRootProvider();
    }
}