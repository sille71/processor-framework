package de.starima.pfw.base.processor.api;

import de.starima.pfw.base.processor.context.api.ITaskContext;
import de.starima.pfw.base.processor.description.api.IStructureValueDescriptor;

import java.util.List;

public interface IBeanProvider extends IProcessor {
    Object getBeanForId(String beanId) throws Exception;

    <T> T getBeanForId(Class<T> clazz, String beanId) throws Exception;

    Object getBeanForId(String beanId, IStructureValueDescriptor<?> descriptor) throws Exception;

    <T> T getBeanForId(Class<T> clazz, String beanId, IStructureValueDescriptor<?> descriptor) throws Exception;

    //<T> T getBeanForId(Class<T> clazz, String beanId);
    String[] getBeanIdentifiers(Object characteristic);
    String getBeanIdentifier(Object characteristic);

    <T> List<T> getBeansOfType(Class<T> clazz);

    <T> T createIsolatedBean(ITaskContext taskContext);
}