# MapInstanceProvider

## Überblick
Provider für `Map<K, V>`-Typen. Bestimmt Key- und Value-Typ via Generics und delegiert Erzeugung und Extraktion jedes Eintrags rekursiv an die `InstanceProviderChain`.

## Verantwortlichkeit
`isResponsibleFor()` gibt `true` wenn der Rohtyp eine `Map` ist.

## provide() — Hydration
1. `parameterValue` als `Map<?, ?>` interpretieren
2. Key- und Value-Typ aus `ParameterizedType` extrahieren (`Map<KeyType, ValueType>`)
3. Pro Entry:
   - `keyCtx` bauen (`typeToResolve = keyType`, `parameterValue = entry.key`)
   - `valueCtx` bauen (`typeToResolve = valueType`, `parameterValue = entry.value`)
   - `rootProvider.provide(keyCtx)` und `rootProvider.provide(valueCtx)` — **REKURSION**
4. `LinkedHashMap` der Ergebnisse zurückgeben (geordnet für deterministische Ausgabe)

## extract() — Dehydration
1. `objectToResolve` als `Map<?, ?>` interpretieren
2. Pro Entry:
   - Key und Value je in Kind-Context verpacken
   - `rootProvider.extract()` für Key und Value — **REKURSION**
3. `LinkedHashMap<Object, Object>` der Raw-Paare zurückgeben

## Parameter
| Parameter | Typ | Default | Beschreibung |
|---|---|---|---|
| — | — | — | Keine eigenen Parameter. |

## Zusammenspiel
- Typ-Auflösung über `ParameterizedType` aus dem Feld-Generics (`Map<String, IProcessor>` etc.)
- Keys und Values werden unabhängig voneinander durch die Chain geleitet

## Beispiel
```java
@ProcessorParameter
private Map<String, IParameterProviderProcessor> paramProviders;

// beanParameterMap:
//   myProcessor:
//     paramProviders:
//       "cluster1": "fileSystemParamProvider:cluster1@context"
//       "cluster2": "propertiesParamProvider:cluster2@context"
//
// → MapInstanceProvider: Key=ScalarInstanceProvider (String), Value=ProcessorInstanceProvider
```
