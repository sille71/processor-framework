package de.starima.pfw.base.processor.description.config.api;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.ITaskContext;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public interface IDescriptorConfigCreationContext extends ITaskContext {
    /**
     * Der Java-Typ, der aufgelÃ¶st werden soll.
     */
    Type getTypeToResolve();

    /**
     * Das Java Object, welches untersucht werden soll.
     * @return
     */
    Object getObjectToResolve();

    Field getFieldToResolve();

    ProcessorParameter getProcessorParameter();

    /**
     * In diesen Fall suchen wir die zugehÃ¶rige IValueFunctionConfig.
     * @return
     */
    IValueConfig getValueConfigToResolve();

    /**
     * Der Descriptor-Config Root-Provider (die Kette), der fÃ¼r rekursive Aufrufe verwendet wird.
     * @return
     */
    IDescriptorConfigProvider getRootProvider();

    void setRootProvider(IDescriptorConfigProvider rootProvider);

    /**
     * Der TypeRef Root-Provider (die Kette), der fÃ¼r rekursive Aufrufe verwendet wird.
     */
    ITypeRefProvider getTypeRefRootProvider();

    /**
     * Setzt den Root-Provider. Wichtig fÃ¼r die Bootstrap-Logik in der Kette.
     */
    void setTypeRefRootProvider(ITypeRefProvider rootProvider);
}