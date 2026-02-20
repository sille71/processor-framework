package de.starima.core.typeref;

import java.util.stream.Collectors;

final class TypeRefFormat {

  static String signature(TypeRef t) {
    return switch (t.kind()) {
      case CLASS -> t.rawTypeName();
      case PARAMETERIZED -> t.rawTypeName() + "<" +
          t.typeArguments().stream().map(TypeRefFormat::signature).collect(Collectors.joining(",")) + ">";
      case ARRAY -> signature(t.componentType().orElseThrow()) + "[]";
      case TYPE_VAR -> "T(" + t.typeVar().map(TypeRef.TypeVar::name).orElse("?") + ")";
      case WILDCARD -> {
        var w = t.wildcard().orElseThrow();
        yield switch (w.variance()) {
          case UNBOUNDED -> "?";
          case EXTENDS -> "? extends " + w.bounds().stream().map(TypeRefFormat::signature).collect(Collectors.joining("&"));
          case SUPER -> "? super " + w.bounds().stream().map(TypeRefFormat::signature).collect(Collectors.joining("&"));
        };
      }
    };
  }

  private TypeRefFormat() {}
}

