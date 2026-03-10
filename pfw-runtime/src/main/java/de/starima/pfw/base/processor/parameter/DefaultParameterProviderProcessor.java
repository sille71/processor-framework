package de.starima.pfw.base.processor.parameter;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.parameter.api.IParameterChangeListener;
import de.starima.pfw.base.processor.variable.api.IVariableProcessor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Kann beliebige Parameter Konfigurationen aus der Liste der beanParameters lesen. Der Ã¼bergebene identifier der getter Methoden 
 * entspricht dabei einer BeanId (im Sinne der <beanParameterMap><beanParameters>) oder einem Pfad von beanId's, getrennt durch /.
 * Er berÃ¼cksichtigt dabei die Parameter der globalen Konfig, Cluster Konfig und der Recon spezifischen Konfig.
 * Die Parameterdefinitionen dieser Konfigurationen werden verschmolzen. Gleiche Definitionen werden in der genannten Reihenfolge Ã¼berschrieben.
 * Die Methoden ohne Identifier verwenden den Identifier dieses Prozessors und rufen die entsprechenden Methoden auf.
 * @author xn06312
 *
 */
@Getter @Setter
@Processor @Component(value = "defaultParameterProviderProcessor")
public class DefaultParameterProviderProcessor extends AbstractParameterProviderProcessor {

	@ProcessorParameter
	protected IVariableProcessor variableProcessor;

	protected Map<String, Object> getVariableParameters(Map<String, String> param, IProcessorContext ctx) {
		//lautet der Key mit SYSTEM:<systemVariablenName>, so handelt es sich um eine Systemvariable, deren Wert bestimmt wird und unter dem Parameternamen, der im Wert der param Map gespeichert ist, gespeichert wird
		//analoges gilt fÃ¼r USER:<systemVariablenName>
		Object value = null;
		HashMap<String, Object> varParam = new HashMap<>();
		for (String key : param.keySet()) {
			value = IVariableProcessor.getVariableValue(key, getVariableProcessor());
			if (value != null) {
				varParam.put(param.get(key), value);
			}
		}

		return varParam;
	}

	@Override
	public void addParameterChangeListener(IParameterChangeListener listener) {

	}

	@Override
	public void removeParameterChangeListener(IParameterChangeListener listener) {

	}

	@Override
	public Map<String, Map<String, Object>> getBeanParameterMap() {
		return runtimeContext.getContextMergedBeanParameterMap();
	}
}