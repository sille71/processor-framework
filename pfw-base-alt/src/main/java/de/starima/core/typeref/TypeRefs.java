package de.starima.core.typeref;

import java.util.*;

final class TypeRefs {

  static TypeRef classRef(String rawTypeName) {
    return new ClassTypeRef(rawTypeName);
  }

  static TypeRef parameterized(String rawTypeName, List<TypeRef> args) {
    return new ParameterizedTypeRef(rawTypeName, List.copyOf(args));
  }

  static TypeRef arrayOf(TypeRef component) {
    return new ArrayTypeRef(component);
  }

  private record ClassTypeRef(String rawTypeName) implements TypeRef {
    @Override public Kind kind() { return Kind.CLASS; }
    @Override public List<TypeRef> typeArguments() { return List.of(); }
    @Override public Optional<TypeRef> componentType() { return Optional.empty(); }
    @Override public Optional<TypeVar> typeVar() { return Optional.empty(); }
    @Override public Optional<Wildcard> wildcard() { return Optional.empty(); }
    @Override public Map<String, Object> toMap() {
      return Map.of("k", "C", "raw", rawTypeName);
    }
  }

  private record ParameterizedTypeRef(String rawTypeName, List<TypeRef> typeArguments) implements TypeRef {
    @Override public Kind kind() { return Kind.PARAMETERIZED; }
    @Override public Optional<TypeRef> componentType() { return Optional.empty(); }
    @Override public Optional<TypeVar> typeVar() { return Optional.empty(); }
    @Override public Optional<Wildcard> wildcard() { return Optional.empty(); }
    @Override public Map<String, Object> toMap() {
      List<Object> args = typeArguments.stream().map(TypeRef::toMap).map(m -> (Object)m).toList();
      return Map.of("k", "P", "raw", rawTypeName, "args", args);
    }
  }

  private record ArrayTypeRef(TypeRef component) implements TypeRef {
    @Override public Kind kind() { return Kind.ARRAY; }
    @Override public String rawTypeName() { return component.rawTypeName() + "[]"; }
    @Override public List<TypeRef> typeArguments() { return List.of(); }
    @Override public Optional<TypeRef> componentType() { return Optional.of(component); }
    @Override public Optional<TypeVar> typeVar() { return Optional.empty(); }
    @Override public Optional<Wildcard> wildcard() { return Optional.empty(); }
    @Override public Map<String, Object> toMap() {
      return Map.of("k", "A", "component", component.toMap());
    }
  }
}