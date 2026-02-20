package de.starima.pfw.base.processor.description.api;

/**
 * Beschreibt keinen direkten Wert, sondern einen Verweis auf einen Wert, der an
 * anderer Stelle definiert ist (z.B. in einem Ã¼bergeordneten Kontext oder einer
 * zentralen Bibliothek). Dieser Deskriptor-Typ wird nicht durch Reflection eines
 * Feld-Typs bestimmt, sondern ist eine "Meta-Option", die von einem
 * IParameterDescriptor angeboten wird, um Vererbung oder Wiederverwendung zu ermÃ¶glichen.
 */
public interface IReferenceValueDescriptor<T extends IStructureValueDescriptor<T>> extends IValueDescriptor {
    /**
     * Gibt den logischen Pfad zum referenzierten Wert zurÃ¼ck.
     * z.B. "parentContext.defaultDifferenceProcessor"
     * @return Der Pfad zur Referenz.
     */
    String getReferencePath();

    /**
     * Gibt die Version der referenzierten Konfiguration zurÃ¼ck, falls vorhanden.
     * @return Die Versionsnummer als String.
     */
    String getVersion();

    /**
     * EnthÃ¤lt einen Struktur-Deskriptor, der NUR die lokalen Ãœberschreibungen
     * der referenzierten Konfiguration beschreibt.
     * @return Ein IStructureValueDescriptor mit den Ã¼berschriebenen Parametern.
     */
    T getOverrides();
}