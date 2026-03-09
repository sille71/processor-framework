package de.starima.pfw.base.processor.description.config.api;

import java.util.List;
import java.util.Optional;

/**
 * Eine unverÃ¤nderliche ReprÃ¤sentation eines Java-Typs, inklusive Generics.
 * Diese Klasse entkoppelt das Framework von der komplexen java.lang.reflect.Type-Hierarchie
 * und zentralisiert alle Abfragen zur Natur eines Typs.
 */
public interface ITypeRef {

    // --- Kernstruktur ---

    /**
     * Gibt die grobe Kategorie des Typs zurÃ¼ck.
     */
    Kind getKind();

    /**
     * Liefert den vollqualifizierten Namen des "rohen" Typs, z.B. "java.util.List".
     */
    String getRawTypeName();

    /**
     * Liefert die Typ-Argumente bei generischen Typen (z.B. fÃ¼r List<String> -> [TypeRef fÃ¼r String]).
     * Ansonsten eine leere Liste.
     */
    List<ITypeRef> getTypeArguments();

    // --- Von IValueDescriptor Ã¼bernommene Methoden ---

    /**
     * Liefert eine fÃ¼r Menschen und die UI lesbare Signatur des Typs, z.B. "List<String>" oder "Integer".
     */
    String getTypeSignature();

    /**
     * Gibt an, ob dieser Typ als IProcessor betrachtet wird.
     * Diese Information wird vom ShapeResolver wÃ¤hrend der Analyse gesetzt.
     */
    boolean isProcessor();

    /**
     * Gibt an, ob dieser Typ als ValueObject betrachtet wird.
     * Diese Information wird vom ShapeResolver wÃ¤hrend der Analyse gesetzt.
     */
    boolean isValueObject();

    /**
     * Gibt an, ob dieser Typ ein numerischer Typ ist (z.B. Integer, Long, Double).
     */
    boolean isNumeric();

    /**
     * Gibt an, ob dieser Typ ein Boolean ist.
     */
    boolean isBoolean();

    /**
     * Gibt an, ob dieser Typ ein Datumstyp ist.
     */
    boolean isDate();

    /**
     * Gibt an, ob dieser Typ ein Zeittyp ist.
     */
    boolean isTime();

    /**
     * Gibt an, ob dieser Typ ein String ist.
     */
    boolean isString();

    /**
     * Gibt an, ob dieser Typ ein Enum ist.
     */
    boolean isEnum();

    /**
     * Gibt an, ob dieser Typ polymorph ist (d.h. ein Interface oder eine abstrakte Klasse,
     * fÃ¼r die zur Laufzeit eine konkrete Implementierung gewÃ¤hlt werden muss).
     */
    boolean isPolymorphic();

    // --- Convenience / Helpers ---
    /**
     * Gibt an, ob dieser Typ ein "Skalar" ist, d.h. ein atomarer Wert,
     * der nicht weiter in seine Bestandteile zerlegt wird.
     * Dies schlieÃŸt komplexe Strukturen wie Collections, Maps, Arrays,
     * Prozessoren oder ValueObjects aus.
     *
     * @return true, wenn der Typ als Skalar betrachtet wird.
     */
    default boolean isScalar() {
        // Ein Typ ist ein Skalar, wenn er einer der grundlegenden, atomaren
        // Kategorien angehÃ¶rt UND keine komplexe, zusammengesetzte Struktur ist.
        boolean isAtomic = isString() || isNumeric() || isBoolean() || isDate() || isTime() || isEnum();
        boolean isComposite = isProcessor() || isValueObject() || isArray() || isListLike() || isMapLike();

        return isAtomic && !isComposite;
    }

    default boolean isRawType(Class<?> raw) {
        return getRawTypeName().equals(raw.getName());
    }

    default boolean isParameterized() {
        return getKind() == Kind.PARAMETERIZED;
    }

    default boolean isArray() {
        return getKind() == Kind.ARRAY;
    }

    default boolean isListLike() {
        return isRawType(java.util.List.class) || isRawType(java.util.Set.class) || isRawType(java.util.Collection.class);
    }

    default boolean isMapLike() {
        return isRawType(java.util.Map.class);
    }

    default Optional<ITypeRef> getComponentType() {
        if (!isArray()) return Optional.empty();
        // Bei einem Array ist das erste (und einzige) Typ-Argument der Komponententyp.
        return getTypeArguments().isEmpty() ? Optional.empty() : Optional.of(getTypeArguments().get(0));
    }

    default Optional<ITypeRef> getListItemType() {
        if (!isListLike()) return Optional.empty();
        return getTypeArguments().isEmpty() ? Optional.empty() : Optional.of(getTypeArguments().get(0));
    }

    default Optional<ITypeRef> getMapKeyType() {
        if (!isMapLike() || getTypeArguments().size() < 1) return Optional.empty();
        return Optional.of(getTypeArguments().get(0));
    }

    default Optional<ITypeRef> getMapValueType() {
        if (!isMapLike() || getTypeArguments().size() < 2) return Optional.empty();
        return Optional.of(getTypeArguments().get(1));
    }

    enum Kind {
        /** Ein einfacher, nicht-generischer Klassentyp (z.B. String). */
        CLASS,
        /** Ein parametrisierter Typ (z.B. List<String>). */
        PARAMETERIZED,
        /** Ein Array-Typ (z.B. String[]). */
        ARRAY,
        /** Ein Typ-Variable (z.B. T in List<T>). FÃ¼r zukÃ¼nftige Erweiterungen. */
        TYPE_VAR,
        /** Eine Wildcard (z.B. ? extends Number). FÃ¼r zukÃ¼nftige Erweiterungen. */
        WILDCARD
    }
}