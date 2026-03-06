# ScalarInstanceProvider

## Überblick
Provider für skalare Typen (String, Integer, Boolean, Double, Enum, Class, etc.). Delegiert intern an die bestehenden `IValueFunction`-Implementierungen und bildet damit die **Blätter des rekursiven Erzeugungsbaums**.

## Verantwortlichkeit
`isResponsibleFor()` gibt `true` wenn der Rohtyp:
- kein `IProcessor` ist
- kein `@ValueObject` ist
- keine `Collection` ist
- keine `Map` ist
- kein Array ist

D.h. alles was nicht strukturell ist, landet hier.

## provide() — Hydration
1. `parameterValue` als Eingabe nehmen
2. `DefaultTransformationContext` als Brücke zur ValueFunction-Welt bauen
3. `ProcessorUtils.createValueFunctionForContext()` für die passende `IValueFunction` aufrufen
4. `valueFunction.transformValue(txCtx, parameterValue)` delegieren
5. Bei fehlender ValueFunction: `null` + Warnung

## extract() — Dehydration
1. `objectToResolve` als Eingabe nehmen
2. `DefaultTransformationContext` bauen
3. Passende `IValueFunction` finden
4. `valueFunction.reverseTransformValue(txCtx, value)` delegieren
5. Fallback: `value.toString()` wenn keine ValueFunction gefunden

**Keine Rekursion** — Skalare haben keine eigenen Parameter.

## Parameter
| Parameter | Typ | Default | Beschreibung |
|---|---|---|---|
| — | — | — | Keine. ValueFunctions werden statisch via ProcessorUtils aufgelöst. |

## Zusammenspiel
- Nutzt `ProcessorUtils.createValueFunctionForContext()` zur ValueFunction-Auswahl
- Baut `DefaultTransformationContext` als Adapter zwischen `IInstanceCreationContext` und `ITransformationContext`
- Schreibt **nicht** in `extractionResult` — Skalare sind Blätter

## Beispiel
```
@ProcessorParameter String myParam;
// beanParameterMap: { "myParam": "hello world" }
// → ScalarInstanceProvider ruft StringValueFunction auf → "hello world"

@ProcessorParameter Integer count;
// beanParameterMap: { "count": "42" }
// → IntegerValueFunction → 42
```
