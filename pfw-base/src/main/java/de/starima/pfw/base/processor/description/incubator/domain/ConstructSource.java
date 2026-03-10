package de.starima.pfw.base.processor.description.incubator.domain;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.Map;

@Getter @Setter
public class ConstructSource implements IConstructSource, IDescribeSource {
    /** Optionales Feld — für feld-basierte Beschreibung */
    private Field sourceField;
    /** Runtime-Context des Incubators oder des aufrufenden Prozessors */
    private IProcessorContext runtimeContext;
    /** Optionaler Draft-Prozessor für Template-basierte Konstruktion */
    private IProcessor draftProcessor;
    /** Das Quell-Objekt für die DESCRIBE-Operation */
    private Object sourceObject;
    /** beanParameterMap für die CONSTRUCT-Operation */
    private Map<String, Map<String, Object>> beanParameterMap;
    /** Root-Eintrag in der beanParameterMap (Startpunkt der CONSTRUCT-Operation) */
    private String rootBeanId;

    /** Implementiert IDescribeSource — liefert das Quell-Objekt für describe() */
    @Override
    public Object getObject() {
        return sourceObject;
    }
}