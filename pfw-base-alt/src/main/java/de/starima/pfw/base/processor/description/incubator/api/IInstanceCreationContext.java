package de.starima.pfw.base.processor.description.incubator.api;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.ITaskContext;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfigProvider;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public interface IInstanceCreationContext extends ITaskContext {
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

    IInstanceProvider getRootProvider();
    void setRootProvider(IInstanceProvider rootProvider);

    /**

    /**
     * Der Descriptor-Config Root-Provider (die Kette), der fÃ¼r rekursive Aufrufe verwendet wird.
     * @return
     */
    IDescriptorConfigProvider getDescriptorConfigRootProvider();

    void setDescriptorConfigRootProvider(IDescriptorConfigProvider rootProvider);
}