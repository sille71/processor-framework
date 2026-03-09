package de.starima.pfw.base.processor.description.api;

/**
 * Erweitert {@link IValueDescriptor} fÃ¼r Werte, die eine Map sind.
 * Definiert den Zugriff auf die Deskriptoren, die den Typ des SchlÃ¼ssels
 * und den Typ des Wertes in der Map beschreiben.
 */
public interface IMapValueDescriptor extends IValueDescriptor {

    /**
     * Liefert den Deskriptor, der den Typ der SchlÃ¼ssel in der Map beschreibt.
     * @return Der IValueDescriptor fÃ¼r die SchlÃ¼ssel.
     */
    IValueDescriptor getKeyValueDescriptor();

    /**
     * Liefert den Deskriptor, der den Typ der Werte in der Map beschreibt.
     * @return Der IValueDescriptor fÃ¼r die Werte.
     */
    IValueDescriptor getValueValueDescriptor();
}