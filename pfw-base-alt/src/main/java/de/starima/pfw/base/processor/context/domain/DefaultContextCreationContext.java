package de.starima.pfw.base.processor.context.domain;

import de.starima.pfw.base.processor.context.api.IContextCreationContext;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.lang.reflect.Type;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DefaultContextCreationContext extends DefaultTaskContext implements IContextCreationContext {
    private Type typeToResolve;
    private Object objectToResolve;

    public DefaultContextCreationContext(IContextCreationContext parent) {
        super(parent);
        this.typeToResolve = parent.getTypeToResolve();
        this.objectToResolve = parent.getObjectToResolve();
    }
}