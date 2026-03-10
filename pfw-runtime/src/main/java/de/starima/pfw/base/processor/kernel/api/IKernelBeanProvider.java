package de.starima.pfw.base.processor.kernel.api;

import de.starima.pfw.base.processor.api.IProcessor;

public interface IKernelBeanProvider extends IProcessor {
    Object getBeanForId(String beanId);

    <T> T getBeanForId(Class<T> clazz, String beanId);
}