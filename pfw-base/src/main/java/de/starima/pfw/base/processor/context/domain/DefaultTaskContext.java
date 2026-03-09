package de.starima.pfw.base.processor.context.domain;

import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.context.api.ITaskContext;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaultTaskContext implements ITaskContext {
    private ITaskContext parentContext;
    private IProcessorContext runtimeContext;
    private Map<String, Map<String, Object>> ownBeanParameterMap;
    private List<Map<String,Map<String, Object>>> ownBeanParameterMaps;

    /**
     * Erstellt einen neuen Kontext, der den Ã¼bergebenen Kontext als seinen Elternteil hat.
     * Wichtige Eigenschaften wie der runtimeContext werden vom Elternteil geerbt.
     *
     * @param parentContext Der Kontext, der zum Elternteil dieses neuen Kontexts wird.
     */
    public DefaultTaskContext(ITaskContext parentContext) {
        this.parentContext = parentContext;
        if (parentContext != null) {
            this.runtimeContext = parentContext.getRuntimeContext();
            this.ownBeanParameterMap = parentContext.getOwnBeanParameterMap();
            this.ownBeanParameterMaps = parentContext.getOwnBeanParameterMaps();
        }
    }
}