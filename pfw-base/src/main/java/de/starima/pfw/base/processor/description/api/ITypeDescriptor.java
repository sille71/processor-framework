package de.starima.pfw.base.processor.description.api;

/**
 * Sprachunabhängige Typ-Beschreibung — reine Metadaten, keine Transformation.
 *
 * <p>Korrespondiert mit {@link de.starima.pfw.base.processor.description.config.api.ITypeRef}
 * im Config-System. Beide beschreiben denselben Aspekt (den Typ einer Abbildung),
 * aber auf unterschiedlichen Abstraktionsebenen:
 * <ul>
 *   <li>{@code ITypeRef} (Config-System): Wird aus dem Java-Typ erzeugt (via TypeRefProviderChain),
 *       enthält zusätzlich {@code Kind}, {@code typeArguments}, {@code rawTypeName}</li>
 *   <li>{@code ITypeDescriptor} (Descriptor-System): Wird von der IValueFunction implementiert,
 *       enthält die abgeleiteten Klassifikatoren</li>
 * </ul>
 *
 * <p><b>Interface-Segregation:</b> Dieses Interface trennt die <b>Beschreibung</b>
 * einer Abbildung (was bildet f ab?) von der <b>Transformation</b> (wie bildet f ab?).
 * Die IValueFunction implementiert beides, aber Konsumenten, die nur die
 * Typ-Beschreibung brauchen (UI, Proposals, Descriptor-System), können
 * gegen dieses schmalere Interface programmieren.
 *
 * <p><b>Mathematisch:</b> Für eine Abbildung {@code f: A → B} beschreibt
 * ITypeDescriptor die Signatur von B (den Bildraum), nicht die Abbildungsvorschrift f.
 */
public interface ITypeDescriptor {

    /**
     * Liefert die sprachunabhängige Typsignatur des Wertes.
     *
     * <p>Beispiele: {@code "string"}, {@code "integer"}, {@code "boolean"},
     * {@code "processor"}, {@code "valueObject"}, {@code "enum"},
     * {@code "list<processor>"}, {@code "map<string, integer>"}.
     *
     * <p>Diese Signatur ist die EINZIGE kanonische Repräsentation des Typs
     * über Sprachgrenzen hinweg (Java ↔ TypeScript ↔ UI).
     */
    String getTypeSignature();

    /**
     * Liefert den sprachunabhängigen Identifier des konkreten Typs.
     *
     * <p>Relevant bei generischen/polymorphen Typen: Die typeSignature ist
     * {@code "processor"}, aber der typeIdentifier ist {@code "ITransformatorProcessor"}
     * oder {@code "csvReaderProcessor"}.
     *
     * <p>Nur relevant wenn {@link #isGeneric()} {@code true} ist.
     */
    String getTypeIdentifier();
    void setTypeIdentifier(String typeIdentifier);

    /**
     * Gibt an, ob dieser Typ generisch ist (z.B. ein Interface oder eine
     * abstrakte Klasse, für die zur Laufzeit mehrere Implementierungen
     * in Frage kommen).
     *
     * <p>Wenn {@code true}: Das UI muss eine Auswahl aus
     * {@code getPossibleValueDescriptors()} anbieten.
     */
    boolean isGeneric();

    /**
     * Gibt an, ob dieser Typ polymorph ist — ein Platzhalter, der zur
     * Laufzeit durch eine konkrete Implementierung ersetzt wird.
     *
     * <p>Unterschied zu {@code isGeneric()}: Polymorphie bedeutet,
     * dass der Typ bewusst als Interface/Abstraktion modelliert ist.
     * Generizität bedeutet, dass der Typ MUSS aufgelöst werden.
     */
    default boolean isPolymorphic() {
        return false;
    }

    // =========================================================================
    // Typ-Klassifikatoren
    // =========================================================================

    boolean isProcessor();

    boolean isValueObject();

    boolean isNumeric();

    boolean isBoolean();

    boolean isDate();

    default boolean isTime() {
        return false;
    }

    boolean isString();

    boolean isEnum();

    /**
     * Gibt an, ob der Typ ein Skalar ist — ein atomarer Wert, der nicht
     * weiter in Bestandteile zerlegt wird.
     *
     * <p>Skalare sind die Blätter des Typ-Baums: String, Integer, Boolean,
     * Double, Enum, Class, Date, etc.
     *
     * <p>NICHT skalar: Processor, ValueObject, Collection, Map.
     */
    default boolean isScalar() {
        return (isString() || isNumeric() || isBoolean() || isDate() || isTime() || isEnum())
                && !isProcessor() && !isValueObject();
    }

    /**
     * Gibt an, ob der Typ eine Struktur ist — ein zusammengesetztes Objekt
     * mit eigenen Parametern.
     *
     * <p>Strukturen: Processor, ValueObject.
     * Strukturen haben einen eigenen IStructureValueDescriptor mit
     * IParameterDescriptoren.
     */
    default boolean isStructure() {
        return isProcessor() || isValueObject();
    }
}
