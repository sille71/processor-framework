# CollectionInstanceProvider

## Überblick
Provider für `List`, `Set` und Arrays. Bestimmt den Element-Typ via Java-Generics und delegiert die Erzeugung jedes Elements rekursiv an die `InstanceProviderChain`.

## Verantwortlichkeit
`isResponsibleFor()` gibt `true` wenn der Rohtyp eine `Collection` ist oder ein Array.

## provide() — Hydration
1. Element-Typ bestimmen: via `ProcessorUtils.getElementType()` aus dem Feld-Generics oder direkt aus dem `ParameterizedType`
2. `parameterValue` als `Collection<?>` interpretieren (Fallback: komma-separierter String)
3. Pro Element:
   - Kind-Context bauen (`typeToResolve = elementType`, `parameterValue = element`)
   - `rootProvider.provide(childCtx)` — **REKURSION** (kann Prozessoren, ValueObjects oder Skalare erzeugen)
4. `ArrayList` der Ergebnisse zurückgeben; bei Array-Zieltyp → `toArray()`

## extract() — Dehydration
1. `objectToResolve` als `Collection<?>` oder Array interpretieren
2. Pro Element:
   - Kind-Context bauen (`objectToResolve = element`, `typeToResolve = element.getClass()`)
   - `rootProvider.extract(childCtx)` — **REKURSION**
3. `List<Object>` der Raw-Werte zurückgeben

## Parameter
| Parameter | Typ | Default | Beschreibung |
|---|---|---|---|
| — | — | — | Keine eigenen Parameter. |

## Zusammenspiel
- Element-Typ-Auflösung via `ProcessorUtils.getElementType(field.getGenericType())`
- Jedes Element wird vollständig über die Chain verarbeitet — auch verschachtelte Prozessoren

## Beispiel
```java
@ProcessorParameter
private List<ITransformProcessor> transformProcessors;

// beanParameterMap:
//   myProcessor:
//     transformProcessors: ["csvTransform:t1@context", "jsonTransform:t2@context"]
//
// → CollectionInstanceProvider iteriert, für jedes Element ruft ProcessorInstanceProvider auf
```
