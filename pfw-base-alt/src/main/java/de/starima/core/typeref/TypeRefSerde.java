package de.starima.core.typeref;

import java.util.*;

final class TypeRefSerde {

  @SuppressWarnings("unchecked")
  static TypeRef fromMap(Map<String, Object> map) {
    String k = (String) map.get("k");
    if ("C".equals(k)) {
      return TypeRefs.classRef((String) map.get("raw"));
    }
    if ("P".equals(k)) {
      String raw = (String) map.get("raw");
      List<Map<String, Object>> argMaps = (List<Map<String, Object>>) map.getOrDefault("args", List.of());
      List<TypeRef> args = argMaps.stream().map(TypeRefSerde::fromMap).toList();
      return TypeRefs.parameterized(raw, args);
    }
    if ("A".equals(k)) {
      Map<String, Object> comp = (Map<String, Object>) map.get("component");
      return TypeRefs.arrayOf(fromMap(comp));
    }
    throw new IllegalArgumentException("Unknown TypeRef kind: " + k);
  }

  private TypeRefSerde() {}
}