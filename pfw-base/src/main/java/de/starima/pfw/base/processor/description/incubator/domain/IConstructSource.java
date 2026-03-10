package de.starima.pfw.base.processor.description.incubator.domain;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.IProcessorContext;

import java.lang.reflect.Field;
import java.util.Map;

public interface IConstructSource {
    Field getSourceField();
    IProcessorContext getRuntimeContext();
    IProcessor getDraftProcessor();

    /** beanParameterMap für die CONSTRUCT-Operation (rootBeanId → prototypeId:identifier@scope) */
    Map<String, Map<String, Object>> getBeanParameterMap();

    /** Der Root-Eintrag in der beanParameterMap — welcher Bean-Eintrag ist der Startpunkt? */
    String getRootBeanId();
}