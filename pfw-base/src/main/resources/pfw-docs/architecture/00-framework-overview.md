# Prozessorframework — Architekturübersicht

> **Everything is a Processor.**

## 1. Grundprinzip

Das Prozessorframework (PFW) ist eine modulare, selbstbeschreibende Architektur
für rekonfigurierbare Anwendungen. Jede Funktion — Transformation, Beschreibung,
Konfiguration, Kommunikation, Logging — ist ein standardisierter Prozessor.

Die beanParameterMap (JSON) ist die universelle Sprache des Systems:
Konfiguration, Austauschformat, Serialisierung und UI-Grundlage in einem.


## 2. Modulstruktur

```
pfw-base        ← pfw-descriptor ← pfw-runtime
(Das Genom)       (Die Zellen)      (Der Organismus)
```

**pfw-base (~70 Klassen):** Alle Interfaces, abstrakte Basisklassen, Annotationen
und Utils. Definiert das Framework vollständig. Ein Entwickler, der nur pfw-base
als Dependency hat, kennt das gesamte API.

**pfw-descriptor (~75 Klassen):** Implementierungen des Descriptor-Systems.
DefaultValueDescriptor, ValueFunctions, TypeRef-Provider, Config-Provider,
InstanceProvider, Incubator.

**pfw-runtime (~85 Klassen):** Spring-Integration, Kernel, Provider,
Service-Runtime, plus alle Erweiterungs-APIs (Condition, Attribute, Data,
Request, Variable, Locale) mit ihren Referenz-Implementierungen.


## 3. Die sechs Chains

Das Framework folgt dem Chain-of-Responsibility-Pattern auf allen Ebenen:

```
1. TypeRefProviderChain          Java-Typ → ITypeRef
2. DescriptorConfigProviderChain ITypeRef → IDescriptorConfig
3. ContextProviderResolverChain  IProcessor → IRuntimeContextProviderProcessor
4. ValueFunctionResolverChain    Kontext → IValueFunction
5. InstanceProviderChain         Config → Objekt (provide) / Objekt → Config (extract)
6. DescriptorResolverChain       BuildContext → IDescriptorProcessor
```


## 4. Die Dreischichtung

```
┌─────────────────────────────────────────────────────────────────┐
│  INCUBATOR (Orchestrierung + Sessions)                          │
│  startDescribe / startConstruct / startEdit                     │
│  Cursor-Stages, Placeholder, Workspace, Patches                 │
├─────────────────────────────────────────────────────────────────┤
│  INSTANCE-PROVIDER (Operative Transformation)                   │
│  provide(): Config → Objekt    extract(): Objekt → Config       │
│  ProcessorIP → ValueObjectIP → CollectionIP → MapIP → ScalarIP  │
├─────────────────────────────────────────────────────────────────┤
│  VALUE-FUNCTION (Atomare Transformation + Typ-Metadaten)        │
│  transformValue / reverseTransformValue                         │
│  Domain / CoDomain / Image (mathematische Mengen)               │
└─────────────────────────────────────────────────────────────────┘
```


## 5. Scope-Hierarchie

```
prototype:     Immer neu erzeugt, nie registriert
context:       Im eigenen Kontext registriert
parentcontext: In der Hierarchie sichtbar
instance:      Im Root-Kontext, erzeugt wenn nötig (Singleton)
provided:      Muss bereits existieren — deklarierte Abhängigkeit
```


## 6. Kontext-Hierarchie

```
ITaskContext
  └── ITransformationContext              (ValueFunction-Sicht)
        └── IInstanceCreationContext      (InstanceProvider-Sicht)

IBuildTaskContext extends ITaskContext
  ├── IConstructTaskContext               (beanParameterMap → Objekt)
  ├── IDescribeTaskContext                (Objekt → Descriptor)
  └── IEditTaskContext                    (Workspace + Lazy + Patches)
```

`IInstanceCreationContext extends ITransformationContext` — ein einziger
Kontext fließt durchs gesamte System, ohne Konvertierung.


## 7. Die mathematische Funktion

Die IValueFunction ist eine Funktion im mathematischen Sinn:

```
f: A → B

A = Domain (Parameterraum, ISetProcessor)
B = CoDomain (Objektraum, ISetProcessor)
Image ⊆ B (tatsächlicher Wertebereich)

transformValue:        f(a) = b
reverseTransformValue: f⁻¹(b) = a
```

Für Skalare implementiert die ValueFunction f direkt.
Für Strukturen delegiert die ValueFunction an den InstanceProvider.
Die Abhängigkeitsrichtung ist einheitlich: `f = Σ ∘ τ`.


## 8. Kernel und RunLevels

RunLevels sind **frei konfigurierbar** — Name und Rang als Parameter, kein Java-Enum.
Neue RunLevels sind ohne Code-Änderungen möglich.

```
Der Kernel kennt nur den RunLevelManager. Alle RunLevels sind konfiguriert,
nicht hardcoded (außer RunLevel 0: Bootstrap).

KernelProcessor → RunLevelManager → RunLevelProcessors → Targets
    (PID 1)           (systemd)         (unit files)      (services)

Vordefinierte Stufen (Rang als Integer, Name als String):
  rank=0:   BOOTSTRAP   — KernelContext, BeanProvider (hardcoded)
  rank=10:  INCUBATION  — FrameworkIncubator, InstanceProviderChain, Descriptoren
  rank=20:  RUNTIME     — Gateway, Dispatcher, PfwRestController
  rank=30:  APPLICATION — Fachliche Services

CLI: --pfw.target-runlevel=RUNTIME (fährt nur bis RUNTIME hoch)
```


## 9. Biologie-Analogie

> *Ein Descriptor ist wie ein Gen: Er weiß nicht, wo er ist —
> aber er weiß, was er tun kann, wenn er aktiviert wird.*

```
DNA / Genom:              beanParameterMap
Gen:                      Descriptor-Knoten
Genexpression:            CONSTRUCT (Hydration: provide)
Genomsequenzierung:       DESCRIBE (Dehydration: extract)
Epigenetik:               Context + Konfiguration
Morphogenetisches Feld:   InstanceProviderChain
Differenzierung:          Polymorphie + getPossibleValueDescriptors
Stammzelle:               PlaceholderDescriptor
Organ:                    Sub-Prozessor
Brutkasten:               Incubator + EditSession
Genetiker:                KI-Assistent (Phase 5)
```


## 10. Pipeline — zukünftige Erweiterungen

Alle als Plugin/Zusatz, kein Architekturumbau:

- **Parameter-Relationen:** order + dependsOn auf @ProcessorParameter
- **Reaktive Domains:** ISetProcessor als Prozessor mit @provided-Referenzen
- **Werte-Constraints:** IValueConstraints (min/max, pattern, minItems)
- **AI Integration Layer:** IProcessorCatalog + IAIBlueprintGenerator + IAIConversationProcessor
- **TypeScript-Port:** ADR-basiert, ~3-4 Claude Code Sessions
- **Sandbox + LogBook:** Prozessor-Selbsttest + strukturiertes Logging
