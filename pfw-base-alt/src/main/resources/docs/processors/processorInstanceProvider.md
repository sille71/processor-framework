# ProcessorInstanceProvider

## Überblick
Zentraler Provider für `IProcessor`-Instanzen. Orchestriert Context-Erstellung, rekursive Parameter-Auflösung und Zirkularitätserkennung. Ersetzt die bisherige Initialisierungslogik in `AbstractProcessor.init(ctx)`.

## Verantwortlichkeit
`isResponsibleFor()` gibt `true` wenn:
- `typeToResolve` eine `IProcessor`-Klasse ist (provide-Richtung)
- `objectToResolve` ein `IProcessor` ist (extract-Richtung)
- `parameterValue` ein String ist UND das Zielfeld einen `IProcessor`-Typ hat

## provide() — Hydration
1. Bereits ein `IProcessor`? → Durchreichen (kein neues Objekt)
2. Scope-Check via `ProcessorUtils.getProcessorFromScope()` — bereits registriert?
3. Bean-Lookup via `beanProvider.getBeanForId(prototypeId)`
4. Identifier + Scope setzen
5. Zirkularitätsprüfung: `isInCreationPath(fullBeanId)` → Abbruch bei Zyklus
6. **PUSH** fullBeanId auf den CreationStack
7. Context erstellen: `ContextProviderResolver.resolve()` → `provider.createContext()`
8. `processor.setRuntimeContext(newCtx)`, Scope-Registrierung
9. Pro `@ProcessorParameter`-Feld (nicht `ignoreInitialization`): Kind-Context bauen, `rootProvider.provide(childCtx)` — **REKURSION**
10. `processor.processorOnInit()`
11. **POP** fullBeanId vom CreationStack (auch bei Fehler via `finally`)

## extract() — Dehydration
1. VisitedSet-Check → bei Zyklus nur fullBeanId als Referenz zurückgeben
2. `markVisited(processor)`
3. Pro `@ProcessorParameter`-Feld (nicht `ignoreExtractParameter`): Kind-Context bauen, `rootProvider.extract(childCtx)` — **REKURSION**
4. `extractionResult.put(identifier, parameters)`
5. fullBeanId zurückgeben

## Parameter
| Parameter | Typ | Default | Beschreibung |
|---|---|---|---|
| `beanProvider` | `IBeanProvider` | — | Spring-Bean-Lookup. Kapselt `applicationContext.getBean()`. |
| `contextProviderResolver` | `IContextProviderResolver` | — | Findet den zuständigen `IRuntimeContextProviderProcessor` für die Context-Erstellung. |

## Zusammenspiel
- Nutzt `ContextProviderResolver` für die Context-Erstellung
- Ruft `rootProvider.provide()` rekursiv für alle Kind-Parameter auf
- CreationStack und VisitedSet werden über die gesamte Rekursion geteilt (`DefaultInstanceCreationContext` Kind-Konstruktor)

## Beispiel
```
beanParameterMap:
  myProcessor:instance1@context:
    beanProvider: "defaultBeanProvider"
    contextProviderResolver: "defaultContextProviderResolver"

→ provide() erzeugt eine vollständig initialisierte MyProcessor-Instanz
  mit allen @ProcessorParameter-Feldern rekursiv aufgelöst.
```
