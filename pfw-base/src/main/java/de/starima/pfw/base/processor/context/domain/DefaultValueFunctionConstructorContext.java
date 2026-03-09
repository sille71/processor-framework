package de.starima.pfw.base.processor.context.domain;

import de.starima.pfw.base.processor.context.api.IValueFunctionConstructorContext;
import lombok.*;

import java.lang.reflect.Field;


@Getter
@Setter
@NoArgsConstructor
public class DefaultValueFunctionConstructorContext extends DefaultTaskContext implements IValueFunctionConstructorContext {
    private Field sourceField;
    private Object sourceObject;
    private Class<?> sourceType;
}