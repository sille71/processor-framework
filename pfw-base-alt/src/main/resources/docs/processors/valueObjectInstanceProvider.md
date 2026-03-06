# ValueObjectInstanceProvider

## Überblick
Provider für `@ValueObject`-annotierte Klassen. Analog zum `ProcessorInstanceProvider`, aber ohne IProcessorContext-Erstellung — ValueObjects haben keinen eigenen Kontext.

## Verantwortlichkeit
`isResponsibleFor()` gibt `true` wenn:
- Der aufgelöste Rohtyp mit `@ValueObject` annotiert ist (via `ProcessorUtils.isConsideredValueObject()`)
- Der Typ kein `IProcessor` ist (Prozessoren haben Vorrang)

## provide() — Hydration
1. `parameterValue` als `fullBeanId`-String interpretieren
2. `prototypeId` + `identifier` via `ProcessorUtils` extrahieren
3. Bean-Lookup via `beanProvider.getBeanForId(prototypeId)`
4. Zirkularitätsprüfung via CreationStack (falls vorhanden)
5. **PUSH** fullBeanId
6. Parameter aus `runtimeContext.getContextMergedBeanParameters(identifier)` laden
7. Pro `@ProcessorParameter`-Feld: Kind-Context bauen, `rootProvider.provide(childCtx)` — **REKURSION**
8. **POP** fullBeanId
9. ValueObject zurückgeben

## extract() — Dehydration
1. VisitedSet-Check
2. `markVisited(valueObject)`
3. Identifier via `IdentifierUtils.getIdentifierForValueObject()` ermitteln
4. Pro `@ProcessorParameter`-Feld: `rootProvider.extract(childCtx)` — **REKURSION**
5. `extractionResult.put(identifier, parameters)` (wenn Identifier vorhanden)
6. Identifier (oder `toString()`-Fallback) zurückgeben

## Parameter
| Parameter | Typ | Default | Beschreibung |
|---|---|---|---|
| `beanProvider` | `IBeanProvider` | `defaultValueObjectProviderProcessor` | Bean-Lookup für `@ValueObject`-Prototypen. |

## Zusammenspiel
- Kein `ContextProviderResolver` — ValueObjects haben keinen `IProcessorContext`
- Nutzt `IdentifierUtils` für die Identifier-Ermittlung (IArtifact, @ProcessorParameter(key=true), oder toString-Fallback)
- Parameter-Iteration wie beim `ProcessorInstanceProvider`

## Beispiel
```
@ValueObject
public class FilterConfig {
    @ProcessorParameter String fieldName;
    @ProcessorParameter String operator;
    @ProcessorParameter String value;
}

beanParameterMap:
  filterConfig:myFilter:
    fieldName: "status"
    operator: "equals"
    value: "ACTIVE"
```
