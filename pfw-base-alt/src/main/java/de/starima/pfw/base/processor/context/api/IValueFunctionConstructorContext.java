package de.starima.pfw.base.processor.context.api;

import java.lang.reflect.Field;

public interface IValueFunctionConstructorContext extends ITaskContext {
    Field getSourceField();
    Object getSourceObject();
    Class<?> getSourceType();
}