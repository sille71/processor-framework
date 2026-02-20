package de.starima.pfw.base.processor.context.api;

import de.starima.pfw.base.util.MapUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basis-Interface fÃ¼r alle aufgabenspezifischen Kontexte.
 * Diese Kontexte sind bewuÃŸt leichtgewichtig und tragen Funktions spezifische Parameter in Methodenaufrufe.
 * Der IProcessorContxt verfolgt hierbei einen anderen Ansatz. Er initialisiert beans mit den Parametern und ruft dann die entsprechenden fachlichen Mehoden auf.
 */
public interface ITaskContext {
    /**
     * Liefert den Ã¼bergeordneten Kontext, falls vorhanden.
     * ErmÃ¶glicht den Aufbau von Hierarchien.
     */
    ITaskContext getParentContext();

    /**
     * Liefert den Laufzeit-Kontext, in dem die Task ausgefÃ¼hrt wird.
     * * Dies gibt Zugriff auf den ProcessorProvider, Parameter etc.
     *
     * @return Der aktuelle IProcessorContext.
     */
    IProcessorContext getRuntimeContext();

    /**
     * Liefert die beanParameterMap des Anwendungskontextes.
     * @return
     */
    default Map<String, Map<String, Object>> getRuntimeBeanParameterMap() {
        return getRuntimeContext() != null ? getRuntimeContext().getBeanParameterMap() : new HashMap<>();
    }

    /**
     * Liefert die vereinte beanParameterMap des Anwendungskontextes.
     * @return
     */
    default Map<String, Map<String, Object>> getRuntimeContextMergedBeanParameterMap() {
        return getRuntimeContext() != null ? getRuntimeContext().getContextMergedBeanParameterMap() : new HashMap<>();
    }

    /**
     * Liefert die beanParameterMap dieses Kontextes. Sie ist zunÃ¤chst unabhÃ¤ngig vom RuntimeContext.
     * @return
     */
    Map<String, Map<String, Object>> getOwnBeanParameterMap();

    default Map<String, Map<String, Object>> getOwnContextMergedBeanParameterMap() {
        return MapUtils.mergeBeanIdParameterMap(getOwnBeanParameterMap(), getRuntimeContextMergedBeanParameterMap());
    }

    /**
     *
     * @return
     */
    List<Map<String,Map<String, Object>>> getOwnBeanParameterMaps();
}