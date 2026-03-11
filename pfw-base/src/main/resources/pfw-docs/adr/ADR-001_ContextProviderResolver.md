# ADR-001: Externalisierung der Context-Erstellung via ContextProviderResolver

## Status
Akzeptiert — in Fork A implementiert

## Kontext

### Bestehendes Framework-Pattern
Das Prozessorframework nutzt durchgehend ein Chain-of-Responsibility-Pattern
für verschiedene Auflösungsebenen:

```
TypeRefProviderChain  →  ConfigProviderChain  →  InstanceProviderChain
```

Jede Chain folgt dem gleichen Aufbau:
- Ein **Interface** mit `isResponsibleFor(context): boolean` und einer Auflösungsmethode
- Mehrere **Implementierungen** als Spring-Beans (jeweils `@Processor`-annotiert)
- Eine **Chain-Klasse** (`@Primary`, sammelt alle Beans via Spring `ObjectProvider`)
- Chain iteriert über Implementierungen, delegiert an die erste zuständige

### Das Problem
Die Prozessor-Initialisierung ist self-managed: `AbstractProcessor.init(ctx)` enthält
die Methode `initContextProvider(ctx)`, die gleichzeitig:
1. **Resolution** durchführt: "Wer soll den Context erstellen?" (Lookup von
   `contextProviderProcessor` in der beanParameterMap des Parent-Kontextes)
2. **Creation** durchführt: Delegiert an den gefundenen ContextProvider

Diese Vermischung von Verantwortlichkeiten steht im Widerspruch zum sauberen
Chain-Pattern des restlichen Frameworks. Außerdem ist der Parameter-Lookup
über einen fest codierten String `"contextProviderProcessor"` implementiert.

### Bestehende Abstraktionen (relevant)
- `IContextCreationContext extends ITaskContext` — trägt `typeToResolve`, `objectToResolve`
- `ITaskContext` — hat `getRuntimeContext(): IProcessorContext` (= Parent-Kontext)
- `IRuntimeContextProviderProcessor` — hat `createContext(IContextCreationContext)`
- `IProcessorContext` — hat `getContextMergedBeanParameters(beanId)`, `getContextProviderProcessor()`
- `IProcessor` — hat `getIdentifier()`, `getProtoTypeIdentifier()`
- `@ProcessorParameter` — Annotation an Feldern, deklariert konfigurierbare Parameter

### Wichtig: Was schon im Context steckt
Der `IContextCreationContext` enthält bereits alles Nötige:
- `typeToResolve` → die Prozessorklasse (z.B. `MyProcessor.class`)
- `objectToResolve` → die Prozessor-Instanz (hat `getIdentifier()`, `getProtoTypeIdentifier()`)
- `getRuntimeContext()` → der Parent-`IProcessorContext` (hat `getContextMergedBeanParameters()`)

**Es werden KEINE neuen Felder auf IContextCreationContext benötigt.**


## Entscheidung

### 1. Neues Interface: IContextProviderResolver

Trennung von Resolution und Creation — konsistent mit dem bestehenden Chain-Pattern.

```
IContextProviderResolver extends IProcessor
  ├─ isResponsibleFor(IContextCreationContext): boolean
  └─ resolve(IContextCreationContext): IRuntimeContextProviderProcessor
```

`resolve()` gibt den **zuständigen ContextProvider** zurück (nicht den fertigen Context).
Der Aufrufer lässt dann den Provider den Context erstellen.

### 2. DefaultContextProviderResolver

Enthält die extrahierte Logik aus `AbstractProcessor.initContextProvider()`.

**Auflösungsreihenfolge:**
1. Introspiziere die Prozessorklasse: Finde das Feld mit `@ProcessorParameter(contextProvider = true)`
2. Lese den Parameternamen (Feldname oder expliziter `name()`)
3. Lade die Parameter des Prozessors aus dem Parent-Kontext:
   `parentCtx.getContextMergedBeanParameters(processor.getIdentifier())`
4. Suche den konfigurierten ContextProvider unter dem annotierten Parameternamen
5. Falls nicht gefunden: Prüfe Default-beanParameterMap der Prozessorklasse
6. Falls immer noch nicht: Fallback auf `parentCtx.getContextProviderProcessor()`

**Caching:** Die Feld-Introspection (Schritt 1-2) wird pro Klasse einmal durchgeführt
und in einer `ConcurrentHashMap` gecacht.

### 3. ContextProviderResolverChain

`@Primary`-Bean, sammelt alle `IContextProviderResolver` via `ObjectProvider`,
delegiert an den ersten zuständigen. Exakt wie die anderen Chains.

### 4. @ProcessorParameter erweitert um Rollen-Markierung

Statt fest codiertem String `"contextProviderProcessor"` bekommt die Annotation
ein neues Attribut:

```
@ProcessorParameter(contextProvider = true)
```

Der Resolver sucht das Feld mit dieser Markierung und benutzt dessen Namen
als Schlüssel in der beanParameterMap.

**Analog dazu:**
```
@ProcessorParameter(processorDescriptor = true)
```

### 5. Framework-Adoption in den RootContext

Nach der Spring-Initialisierung (Phase 0) und der RootContext-Erstellung (Phase 0.5)
wird eine neue Kernel-Phase eingefügt:

**Phase 1: Framework-Adoption**
- Iteriert über alle Spring-registrierten `IProcessor`-Beans
- Für jeden, der noch keinen RuntimeContext hat:
  - `processor.setRuntimeContext(rootContext)`
  - `rootContext.addProcessor(processor)`
  - Registrierung im Scope-System

Danach sind alle Framework-Prozessoren vollwertige Mitglieder:
inspizierbar, konfigurierbar, über das UI navigierbar.


## Neue Abstraktionen

| Abstraction | Rolle | Package |
|---|---|---|
| `IContextProviderResolver` | Interface: Wer erstellt den Context? | context.api |
| `DefaultContextProviderResolver` | Lookup via Annotation + beanParameterMap | context |
| `ContextProviderResolverChain` | Chain, `@Primary`, delegiert | context |
| `FrameworkAdoptionPhaseProcessor` | Kernel-Phase: Spring→Framework Bridge | kernel |


## Interaktion mit Bestehendem

### Geändert
- `@ProcessorParameter`: +2 Attribute (`contextProvider`, `processorDescriptor`)
- `AbstractProcessor`: Feld-Annotationen um `contextProvider=true` ergänzt
- `DefaultRuntimeContextProviderProcessor`: `createContext(IContextCreationContext)` ausgefüllt
  (war bisher ein Stub). Enthält NUR Creation-Logik, kein Lookup.
- `DefaultContextCreationContext`: Zusätzlicher Konstruktor als Brücke von
  `IInstanceCreationContext` (damit der ProcessorInstanceProvider einen
  ContextCreationContext aus seinem InstanceCreationContext bauen kann)

### Unverändert
- `IContextCreationContext` — kein neues Feld
- `IRuntimeContextProviderProcessor` — Interface unverändert
- `IInstanceCreationContext` — unverändert
- `ITaskContext` — unverändert


## Regeln

1. **Resolution ≠ Creation:** Der `DefaultContextProviderResolver` hat KEINE
   Methode `createContext()`. Der `DefaultRuntimeContextProviderProcessor` hat
   KEINEN Lookup-Code für andere Provider.

2. **Annotation statt String:** Der Resolver sucht NICHT nach einem fest codierten
   Parameternamen. Er findet das Feld über `@ProcessorParameter(contextProvider = true)`
   und benutzt dessen Feldname als Schlüssel.

3. **Cache:** Die Feld-Introspection erfolgt einmal pro Klasse (ConcurrentHashMap).
   Kein wiederholtes Reflection bei jedem Aufruf.

4. **Fallback-Kette:** Explizite Konfiguration → Default-beanParameterMap → Parent-ContextProvider.
   Nie null zurückgeben — immer mindestens den Parent-ContextProvider.

5. **Interface-Kompatibilität:** Die deprecated Methoden auf `IRuntimeContextProviderProcessor`
   bleiben bestehen. Neue Methode `createContext(IContextCreationContext)` ist die primäre.

6. **Framework-Adoption:** Nach Phase 0.5 (RootContext) und vor Phase 2 (FrameworkIncubator).
   Nur Prozessoren ohne RuntimeContext werden adoptiert. KernelProcessor wird übersprungen.


## Sequenz

```
ProcessorInstanceProvider.provide(instanceContext)
  │
  │  instanceContext hat:
  │    .typeToResolve      → Prozessorklasse
  │    .objectToResolve    → Prozessor-Instanz (mit getIdentifier())
  │    .getRuntimeContext() → Parent-IProcessorContext
  │
  ├─ Baue IContextCreationContext aus IInstanceCreationContext (Brücken-Konstruktor)
  │
  ├─ ContextProviderResolverChain.resolve(creationContext)
  │    └─ DefaultContextProviderResolver.resolve(creationContext)
  │         ├─ Cache-Lookup: @ProcessorParameter(contextProvider=true) → Feldname
  │         ├─ parentCtx.getContextMergedBeanParameters(identifier)
  │         ├─ parameters.get(feldname) → beanId des ContextProviders
  │         ├─ processorProvider.getProcessorForBeanId(beanId) → ContextProvider
  │         └─ Fallback: parentCtx.getContextProviderProcessor()
  │
  ├─ provider.createContext(creationContext) → neuer IProcessorContext
  │
  └─ Reflection: inject "reconContext" in den Prozessor
```


## Verifikation

- [ ] `DefaultContextProviderResolver` hat keine createContext-Methode
- [ ] `DefaultRuntimeContextProviderProcessor.createContext(IContextCreationContext)` enthält
      keinen `parameters.get("contextProvider...")` Lookup
- [ ] `IContextCreationContext` hat kein neues Feld/Methode
- [ ] Introspection-Cache: Zweiter Aufruf für gleiche Klasse triggert kein Reflection
- [ ] Kein fest codierter String "contextProviderProcessor" im Resolver
- [ ] Chain folgt dem Pattern: `ObjectProvider<IContextProviderResolver>`, `@Primary`
- [ ] Legacy-Fallback: `parameterName + "Identifier"` wird auch geprüft


## Konsequenzen

**Positiv:**
- Konsistent mit bestehendem Chain-Pattern (TypeRef, Config, Instance)
- Resolution und Creation isoliert testbar
- Neue Resolver-Strategien ohne Änderung am bestehenden Code
- Framework-Prozessoren sind nach Adoption inspizierbar und konfigurierbar

**Negativ:**
- 4 neue Klassen (Interface + Default + Chain + Adoption)
- Brücken-Konstruktor in DefaultContextCreationContext (technische Schuld,
  auflösbar wenn IInstanceCreationContext extends IContextCreationContext)

**Trade-offs:**
- Mehr Indirektion, aber konsistenter Gesamtentwurf
- Annotation-Attribut statt eigener Annotation (@ContextProvider) — einfacher,
  aber weniger auffindbar über IDE "Find Usages"
