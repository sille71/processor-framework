package de.starima.pfw.base.processor.description.api;

import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.transformator.api.ISubjectFunction;
import java.util.Collections;
import java.util.List;

/**
 * Ersetzt die IParameterFunctionProcessor.
 * Repräsentiert die Kernlogik für die Transformation und Validierung eines Wertes.
 * Sie ist die "Engine" hinter einem IValueDescriptor.
 *
 * <p>Erweitert {@link ITypeDescriptor} für Interface-Segregation:
 * Konsumenten, die nur Typ-Metadaten brauchen (UI, Proposals), können
 * gegen {@code ITypeDescriptor} programmieren ohne Transformationslogik.
 *
 * @param <S> Subject-Typ (ITransformationContext-Subtyp)
 * @param <I> Input-Typ (z.B. String aus der Konfiguration)
 * @param <O> Output-Typ (z.B. ein IProcessor-Objekt)
 */
public interface IValueFunction<S extends ITransformationContext, I, O>
        extends ISubjectFunction<S, I, O>, ITypeDescriptor, IDescriptorProcessor {

    // Von ITypeDescriptor GEERBT (nicht nochmal deklarieren!):
    // String getTypeSignature()         ← geerbt (abstract)
    // boolean isGeneric()               ← geerbt (abstract)
    // String getTypeIdentifier()        ← geerbt (abstract)
    // void setTypeIdentifier(String)    ← geerbt (abstract)
    // default boolean isPolymorphic()   ← geerbt (default: false)
    // boolean isProcessor()             ← geerbt — Default-Impl unten
    // boolean isValueObject()           ← geerbt — Default-Impl unten
    // boolean isNumeric() / isBoolean() / isDate() / isString() / isEnum() ← geerbt — Default-Impl unten
    // default boolean isScalar()        ← geerbt (default)
    // default boolean isStructure()     ← geerbt (default)

    // =========================================================================
    // Transformations-spezifische Methoden
    // =========================================================================

    I reverseTransformValue(O value);
    I reverseTransformValue(S transformationContext, O value);

    /**
     * Liefert eine Liste der möglichen Werte, die diese Funktion verarbeiten kann (Parameterwerte).
     * Besonders relevant für Enums oder andere feste Definitionsbereiche (I).
     */
    default List<String> getPossibleInputValues() {
        return Collections.emptyList();
    }

    /**
     * Liefert eine Liste der möglichen Werte, die diese Funktion erzeugen kann (aufgelöste Werte).
     * Besonders relevant für Enums oder andere feste Definitionsbereiche (O).
     */
    default List<String> getPossibleOutputValues() {
        return Collections.emptyList();
    }

    // =========================================================================
    // ITypeDescriptor Default-Implementierungen
    // =========================================================================

    /**
     * Prüft, ob der Wert ein IProcessor ist.
     */
    @Override
    default boolean isProcessor() {
        return false;
    }

    /**
     * Prüft, ob der Wert ein ValueObject ist.
     */
    @Override
    default boolean isValueObject() {
        return false;
    }

    /**
     * Gibt an, ob der Wert ein numerischer Typ ist (z.B. integer, long, double).
     */
    @Override
    default boolean isNumeric() {
        String signature = getTypeSignature();
        return "integer".equals(signature) || "long".equals(signature) || "double".equals(signature)
                || "float".equals(signature) || "decimal".equals(signature) || "bigdecimal".equals(signature);
    }

    /**
     * Gibt an, ob der Wert ein Boolean ist.
     */
    @Override
    default boolean isBoolean() {
        return "boolean".equals(getTypeSignature());
    }

    /**
     * Gibt an, ob der Wert ein Datumstyp ist.
     */
    @Override
    default boolean isDate() {
        return "datetime".equals(getTypeSignature()) || "date".equals(getTypeSignature());
    }

    /**
     * Gibt an, ob der Wert ein Zeittyp ist.
     */
    @Override
    default boolean isTime() {
        return "time".equals(getTypeSignature());
    }

    /**
     * Gibt an, ob der Wert ein String ist.
     */
    @Override
    default boolean isString() {
        return "string".equals(getTypeSignature());
    }

    /**
     * Gibt an, ob der Wert ein Enum ist.
     */
    @Override
    default boolean isEnum() {
        return "enum".equals(getTypeSignature());
    }
}