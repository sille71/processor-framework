package de.starima.pfw.base.processor.description.api;

/**
 * Stellt die Beschreibung/Dokumentation fÃ¼r Prozessoren zur VerfÃ¼gung. Diese Beschreibung dient als Ausgangspunkt fÃ¼r:
 *   1. die Processor Initialisierung
 * 	 2. die Processor Dokumentation - Hier handelt es sich um eine Erweiterung der Beschreibung mittels @Processor, da die konkrete Beschreibung
 * 	    vom jeweiligen Einsatz abhÃ¤ngen kann.
 *   3. Bauplan im ReconLight
 *   4. Inspektion: Der Prozessor kann mit Hilfe des Descriptors seinen aktuellen, konkreten Aufbau anzeigen.
 *
 *  Rekursionen bei der Erzeugung des Descriptors selbst mÃ¼ssen gesondert abgefangen werden!
 */
public interface IProcessorDescriptor extends IStructureValueDescriptor<IProcessorDescriptor> {
    public IParameterGroupDescriptor getParameterGroupDescriptor();
    public void setParameterGroupDescriptor(IParameterGroupDescriptor groupDescriptor);

    // In Zukunft:
    // List<IMethodDescriptor> getMethodDescriptors();
}