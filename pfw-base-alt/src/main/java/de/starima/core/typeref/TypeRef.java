package de.starima.core.typeref;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TypeRef {

  /** Grobe Kategorie des Typs. */
  Kind kind();

  /** Vollqualifizierter Name des Raw-Typs, z.B. "java.util.List" oder "com.foo.MyClass". */
  String rawTypeName();

  /** Typ-Argumente bei generischen Typen (z.B. List<String> -> [String]). Sonst leer. */
  List<TypeRef> typeArguments();

  /** Komponenten-Typ bei Arrays (z.B. String[] -> String). Nur fÃ¼r ARRAY sinnvoll. */
  Optional<TypeRef> componentType();

  /** Optional: TypeVar/Wildcard Informationen (fÃ¼r spÃ¤tere Erweiterung). */
  Optional<TypeVar> typeVar();

  Optional<Wildcard> wildcard();

  /* -------------------------
   * Convenience / Helpers
   * ------------------------- */

  default boolean isRaw(Class<?> raw) {
    return rawTypeName().equals(raw.getName());
  }

  default boolean isParameterized() {
    return kind() == Kind.PARAMETERIZED;
  }

  default boolean isArray() {
    return kind() == Kind.ARRAY;
  }

  default boolean isListLike() {
    return isRaw(java.util.List.class) || isRaw(java.util.Set.class) || isRaw(java.util.Collection.class);
  }

  default boolean isMapLike() {
    return isRaw(java.util.Map.class);
  }

  default Optional<TypeRef> listItemType() {
    if (!isListLike()) return Optional.empty();
    return typeArguments().isEmpty() ? Optional.empty() : Optional.of(typeArguments().get(0));
  }

  default Optional<TypeRef> mapKeyType() {
    if (!isMapLike()) return Optional.empty();
    return typeArguments().size() < 1 ? Optional.empty() : Optional.of(typeArguments().get(0));
  }

  default Optional<TypeRef> mapValueType() {
    if (!isMapLike()) return Optional.empty();
    return typeArguments().size() < 2 ? Optional.empty() : Optional.of(typeArguments().get(1));
  }

  /**
   * Kanonische Signatur, die sich gut als Cache-Key eignet.
   * Beispiel: java.util.List<java.lang.String>
   */
  default String signature() {
    return TypeRefFormat.signature(this);
  }

  /* -------------------------
   * Serialization (Map/JSON)
   * ------------------------- */

  /**
   * Map-ReprÃ¤sentation fÃ¼r beanParameterMap/JSON.
   * Stabil, ohne Reflection-Objekte.
   */
  Map<String, Object> toMap();

  /** Rekonstruktion aus Map. */
  static TypeRef fromMap(Map<String, Object> map) {
    return TypeRefSerde.fromMap(map);
  }

  /* -------------------------
   * Factory Methods
   * ------------------------- */

  static TypeRef of(Class<?> raw) {
    return TypeRefs.classRef(raw.getName());
  }

  static TypeRef of(Class<?> raw, TypeRef... args) {
    return TypeRefs.parameterized(raw.getName(), List.of(args));
  }

  static TypeRef arrayOf(TypeRef component) {
    return TypeRefs.arrayOf(component);
  }

  enum Kind {
    CLASS,
    PARAMETERIZED,
    ARRAY,
    TYPE_VAR,
    WILDCARD
  }

  /** Optional fÃ¼r spÃ¤tere Generics-AuflÃ¶sung (T, E, etc.). */
  record TypeVar(String name, List<TypeRef> bounds) {}

  /** Optional fÃ¼r ? extends / ? super. */
  record Wildcard(Variance variance, List<TypeRef> bounds) {
    enum Variance { EXTENDS, SUPER, UNBOUNDED }
  }
}