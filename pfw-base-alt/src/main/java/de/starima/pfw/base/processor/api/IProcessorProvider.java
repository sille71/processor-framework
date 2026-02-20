package de.starima.pfw.base.processor.api;

import de.starima.pfw.base.processor.context.api.IProcessorContext;

/**
 * Beschreibt den Ursprung der Prozessoren. Soll er selbst ein Prozessor sein?
 * Brauchen wir dann noch den Service IProcessorProviderService, ist doch identisch?
 */
public interface IProcessorProvider extends IBeanProvider {
    <T extends IProcessor> T getProcessorForType(Class<T> clazz, String processorType, IProcessorContext ctx, IProcessor parentProcessor);
    <T extends IProcessor> T getProcessorForBeanId(Class<T> clazz, String beanid, IProcessorContext ctx, IProcessor parentProcessor);
    <T extends IProcessor> T getProcessorForBeanIdWithType(Class<T> clazz, String beanid, String type, IProcessorContext ctx, IProcessor parentProcessor);
    IProcessor getProcessorForBeanId(String beanid, IProcessorContext ctx, IProcessor parentProcessor);
    IProcessor getProcessorForBeanIdWithType(String beanid, String type, IProcessorContext ctx, IProcessor parentProcessor);

    IProcessor getProcessorForType(String processorType, IProcessorContext ctx, IProcessor parentProcessor);

    boolean isProcessorCreationPossible(String beanid, IProcessorContext ctx, IProcessor parentProcessor);

    //<T> T createIsolatedProcessor(ITaskContext taskContext);
}