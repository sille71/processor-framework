package de.starima.pfw.base.processor.variable.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.attribute.api.IAttribute;

public interface IVariableProcessor extends IProcessor {
	public static Object getSystemVariableValue(String identifier, IVariableProcessor variableProcessor) {
		if (variableProcessor != null) {
			IAttribute c;
			try {
				c = variableProcessor.getSystemVariable(identifier);
				return c.getValue();
			} catch (Exception e) {

			}
		}
		return null;
	}

	public static Object getUserVariableValue(String identifier, IVariableProcessor variableProcessor) {
		if (variableProcessor != null) {
			IAttribute c;
			try {
				c = variableProcessor.getUserVariable(identifier);
				return c.getValue();
			} catch (Exception e) {
			}
		}
		return null;
	}

	/**
	 * Liest Variablen aus dem Ã¼bergebenen Variablen Prozessor. Dabei kann der name die folgende Struktur haben:
	 * SYSTEM:<variablename>  in diesem Fall werden Systemvariablen gelesen
	 * USER:<variablename>  in diesem Fall werden Benutzervariablen gelesen
	 * EnthÃ¤lt der name kein :, so wird standardmÃ¤ÃŸig die Systemvariable gelesen.
	 * @param name
	 * @param variableProcessor
	 * @return
	 */
	public static Object getVariableValue(String name, IVariableProcessor variableProcessor) {
		if (name != null && name.contains(":")) {
			switch (name.split(":")[0]) {
				case "SYSTEM":
					return getSystemVariableValue(name.split(":")[1], variableProcessor);
				case "USER":
					return getUserVariableValue(name.split(":")[1], variableProcessor);
			}
		}
		//the default
		return getSystemVariableValue(name, variableProcessor);
	}

	public IAttribute getSystemVariable(String name) throws Exception;
	public IAttribute getUserVariable(String name) throws Exception;
}