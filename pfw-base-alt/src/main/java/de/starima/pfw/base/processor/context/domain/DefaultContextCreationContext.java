package de.starima.pfw.base.processor.context.domain;

import de.starima.pfw.base.processor.context.api.IContextCreationContext;
import de.starima.pfw.base.processor.context.api.ITaskContext;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceCreationContext;
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

    /**
     * Brücken-Konstruktor: Baut einen ContextCreationContext aus einem IInstanceCreationContext.
     *
     * <p>Beide Kontexte tragen typeToResolve und objectToResolve —
     * der IInstanceCreationContext hat zusätzlich Field- und Provider-Infos,
     * die für die Context-Erstellung nicht relevant sind.
     *
     * <p>Der runtimeContext (= Parent-IProcessorContext) wird vom TaskContext geerbt.
     */
    public DefaultContextCreationContext(IInstanceCreationContext instanceContext) {
        super(instanceContext); // erbt runtimeContext, parentContext, ownBeanParameterMap
        this.typeToResolve = instanceContext.getTypeToResolve();
        this.objectToResolve = instanceContext.getObjectToResolve();
    }

    /**
     * Basis-Konstruktor für Builder-Pattern oder manuelles Setup.
     */
    public DefaultContextCreationContext(ITaskContext parentContext, Type typeToResolve, Object objectToResolve) {
        super(parentContext);
        this.typeToResolve = typeToResolve;
        this.objectToResolve = objectToResolve;
    }
}