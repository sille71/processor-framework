package de.starima.pfw.base.processor.parameter.api;

import de.starima.pfw.base.processor.api.IProcessor;

/**
 * Ãœbernimmt die Validierung von Prozessorparametern.
 */
public interface IParameterValidatorProcessor extends IProcessor {
    public void validateParameterValue(String parameterName, String parameterValue);
}