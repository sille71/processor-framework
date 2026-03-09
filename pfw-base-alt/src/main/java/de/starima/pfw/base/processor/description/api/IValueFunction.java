package de.starima.pfw.base.processor.description.api;

import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.transformator.api.ISubjectFunction;
import java.util.Collections;
import java.util.List;

/**
 * Ersetzt die IParameterFunctionProcessor.
 * ReprÃ¤sentiert die Kernlogik fÃ¼r die Transformation und Validierung eines Wertes.
 * Sie ist die "Engine" hinter einem IValueDescriptor.
 * @param <I> Input-Typ (z.B. String aus der Konfiguration)
 * @param <O> Output-Typ (z.B. ein IProcessor-Objekt)
 */
public interface IValueFunction<S extends ITransformationContext, I, O> extends ISubjectFunction<S, I, O>, IDescriptorProcessor {
    // Wir Ã¼bernehmen die Methoden aus der alten IParameterFunctionProcessor
    I reverseTransformValue(O value);
    //I reverseTransformValue(Pair<Object, Field> subject, O value);
    I reverseTransformValue(S transformationContext, O value);

    /**
     * Liefert die sprachunabhÃ¤ngige Typsignatur des Wertes, den diese Funktion reprÃ¤sentiert.
     * z.B. "string", "processor", "list<valueObject>".
     * Diese Methode ist die EINZIGE Quelle der Wahrheit fÃ¼r die Typsignatur.
     */
    String getTypeSignature();

    /**
     * Gibt an, ob diese Funktion einen generischen Typ (z.B. ein Interface) reprÃ¤sentiert.
     * @return true, wenn der Typ generisch ist und Implementierungen gesucht werden mÃ¼ssen.
     */
    boolean isGeneric();

    /**
     * Liefert den sprachunabhÃ¤ngigen Identifier des generischen Typs (des Zielwertes O).
     * Ist nur relevant, wenn isGeneric() true ist.
     */
    //String getGenericTypeIdentifier(IParameterContext context);
    String getTypeIdentifier();
    void setTypeIdentifier(String genericTypeIdentifier);

    /**
     * PrÃ¼ft, ob der Wert ein IProcessor ist, indem es die assoziierte ValueFunction fragt.
     */
    default boolean isProcessor() {
        return false;
    }

    /**
     * PrÃ¼ft, ob der Wert ein ValueObject ist, indem es die assoziierte ValueFunction fragt.
     */
    default boolean isValueObject() {
        return false;
    }

    /**
     * Gibt an, ob der Wert ein numerischer Typ ist (z.B. integer, long, double).
     * Die Default-Implementierung nutzt die typeSignature.
     * @return true, wenn der Wert ein numerischer Typ ist.
     */
    default boolean isNumeric() {
        String signature = getTypeSignature();
        return "integer".equals(signature) || "long".equals(signature) || "double".equals(signature) || "float".equals(signature) || "decimal".equals(signature) || "bigdecimal".equals(signature);
    }

    /**
     * Gibt an, ob der Wert ein Boolean ist.
     * @return true, wenn der Wert ein boolean oder Boolean ist.
     */
    default boolean isBoolean() {
        return "boolean".equals(getTypeSignature());
    }

    /**
     * Gibt an, ob der Wert ein Datumstyp ist.
     */
    default boolean isDate() {
        return "datetime".equals(getTypeSignature()) || "date".equals(getTypeSignature());
    }

    /**
     * Gibt an, ob der Wert ein Zeittyp ist.
     */
    default boolean isTime() {
        return "time".equals(getTypeSignature());
    }

    /**
     * Gibt an, ob der Wert ein String ist.
     */
    default boolean isString() {
        return "string".equals(getTypeSignature());
    }

    /**
     * Gibt an, ob der Wert ein Enum ist.
     * (Annahme: Wir definieren eine 'enum' typeSignature)
     */
    default boolean isEnum() {
        return "enum".equals(getTypeSignature());
    }

    /**
     * Liefert eine Liste der mÃ¶glichen Werte, die diese Funktion verarbeiten kann (Parameterwerte).
     * Besonders relevant fÃ¼r Enums oder andere feste Definitionsbereiche (I).
     * @return Eine Liste der mÃ¶glichen Werte des Definitionsbereiches als Strings.
     * Wir liefern hier nicht Object zurÃ¼ck - evtl. hier noch eine andere Methode anbieten.
     * Der Ursprung liegt in der Domain Menge. FÃ¼r einfache FÃ¤lle wird das aber in der Regel Ã¼ber die Parameter Annotation gesteuert. Oder Ã¼ber enums.
     */
    default List<String> getPossibleInputValues() {
        return Collections.emptyList();
    }

    /**
     * Liefert eine Liste der mÃ¶glichen Werte, die diese Funktion erzeugen kann (aufgelÃ¶ste Werte).
     * Besonders relevant fÃ¼r Enums oder andere feste Definitionsbereiche (O).
     * @return Eine Liste der mÃ¶glichen Werte des Wertebereiches als Strings.
     */
    default List<String> getPossibleOutputValues() {
        return Collections.emptyList();
    }
}