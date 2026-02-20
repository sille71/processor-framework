package de.starima.pfw.base.processor.description.api;

/**
 * Beschreibt einen Wert, der eine Liste von Elementen ist.
 * FÃ¼r den Moment wird von homogenen Listen ausgegangen (alle Elemente haben den gleichen Typ).
 */
public interface ICollectionValueDescriptor extends IValueDescriptor {
    /**
     * Liefert den "Template"-Deskriptor, der fÃ¼r jedes Element in der Liste gilt.
     * Wenn die Liste z.B. eine {@code List<String>} ist, wÃ¼rde diese Methode einen
     * {@code IScalarValueDescriptor} zurÃ¼ckgeben. Wenn es eine {@code List<IReconProcessor>} ist,
     * wÃ¼rde sie einen {@code IStructureValueDescriptor} zurÃ¼ckgeben.
     *
     * @return Der IValueDescriptor, der den Typ der Listenelemente beschreibt.
     */
    IValueDescriptor getElementValueDescriptor();

    /**
     * Gibt an, ob Duplikate in der Liste erlaubt sind.
     * @return true, wenn Duplikate erlaubt sind.
     */
    boolean allowDuplicates();
}