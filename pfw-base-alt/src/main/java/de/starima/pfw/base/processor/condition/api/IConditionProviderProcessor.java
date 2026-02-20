package de.starima.pfw.base.processor.condition.api;

import de.starima.pfw.base.processor.api.IProcessor;

import java.util.List;

public interface IConditionProviderProcessor<C extends ICondition> extends IProcessor {
    /*
     * Liefert nur eine Bedingung, die durch diesen Provider beschrieben wird. Siehe alte Welt DefaultConditionProvider, hier werden die Bedingungen als
     * Prozessoren konfiguriert. Das ging nicht anders, da die beanParameterMap dort keine Objektwerte zulÃ¤sst.
     * In der neuen Welt kÃ¶nnen wir auch ganze Objekte konfigurieren. Damit auch Conditions!
     * @return
     * @param <C>
     */
    public C getCondition(String conditionReference);
    public List<C> getConditions(List<String> conditionReferences);

    /*
     * In diesem Fall wurde der Provider mit einer Liste von Bedingungen konfiguriert. Geht nur in der neuen Welt mit der neuen BeanParameterMap.
     * @param identifier
     * @return
     * @param <C>
     */
    public List<C> getConditions();
    public List<String> getConditionReferences();
}