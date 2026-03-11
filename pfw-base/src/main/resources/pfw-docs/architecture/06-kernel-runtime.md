# Kernel und RunLevels

## Kernprinzip

Der Kernel ist **kein Orchestrator** — er ist ein **Bootstrap-Prozess**.

```
KernelProcessor → RunLevelManager → RunLevelProcessors → Targets
     (PID 1)         (systemd)          (unit files)      (services)
```

Der Kernel macht genau eine Sache: Er erstellt den RootContext (RunLevel 0: BOOTSTRAP)
und übergibt dann die Kontrolle an den RunLevelManager. Alles weitere ist "User Space".


## RunLevels als Systemzustände

RunLevels sind **ZUSTÄNDE**, keine Abläufe. Jeder RunLevel garantiert, dass bestimmte
Prozessoren verfügbar sind.

```java
public enum RunLevel {
    BOOTSTRAP(0),       // KernelContext, BeanProvider, Logging
    INCUBATION(1),      // FrameworkIncubator, InstanceProviderChain, Descriptoren
    RUNTIME(2),         // Gateway, Dispatcher, Security
    APPLICATION(3),     // Fachliche Services
    SHUTDOWN(99);       // Geordnetes Herunterfahren
}
```

Biologische Metapher: Entwicklungsstadien eines Organismus — jedes Stadium
garantiert bestimmte Fähigkeiten, nicht nur Prozessschritte.


## Systemarchitektur

```
Spring Bootstrap
    │
    ▼
KernelProcessor (@Service, Singleton)
    │  @PostConstruct initProcessor()
    │  1. Erstellt RootContext via KernelRuntimeContextProviderProcessor
    │  2. Registriert sich im RootContext
    │  3. Holt RunLevelManager via BeanProvider
    │
    ▼
DefaultRunLevelManager
    │  advanceTo(APPLICATION)
    │  Iteriert alle konfigurierten RunLevelProcessors
    │
    ├─▶ DefaultRunLevelProcessor [INCUBATION]
    │       targets: [frameworkIncubator, instanceProviderChain, ...]
    │
    ├─▶ DefaultRunLevelProcessor [RUNTIME]
    │       targets: [requestGateway]
    │
    └─▶ DefaultRunLevelProcessor [APPLICATION]
            targets: [csvReconService, ...]
```


## Bootstrap-Phasen im Detail

### RunLevel 0: BOOTSTRAP (hardcoded)

Dieser RunLevel ist **nicht konfigurierbar** — er ist im KernelProcessor hardcoded.
Grund: Es gibt noch keinen Incubator, der die Konfiguration verarbeiten könnte.

```
Was passiert:
1. Spring injiziert KernelRuntimeContextProviderProcessor (via @Autowired)
2. @PostConstruct: RootContext wird erstellt
3. Kernel registriert sich im RootContext
4. RunLevelManager wird via BeanProvider geholt
5. advanceTo(targetRunLevel) wird aufgerufen
```

### RunLevel 1+: Selbstorganisierend

Ab INCUBATION ist das System selbstorganisierend. Der RunLevelManager aktiviert
jeden RunLevelProcessor, der seine Targets erzeugt und registriert.

```
RunLevelProcessor.activate(ctx):
    for target in targets:
        ProcessorUtils.registerProcessorInScope(target, ctx.getRuntimeContext())
```


## Konfiguration via beanParameterMap

Das ist die Mächtigkeit dieses Designs: Die gesamte Systemkonfiguration ist
eine beanParameterMap — änderbar ohne Code-Änderungen.

```json
{
  "kernelProcessor": {
    "targetRunLevel": "APPLICATION"
  },
  "defaultRunLevelManager": {
    "runLevels": "incubationRunLevel, runtimeRunLevel, applicationRunLevel"
  },
  "incubationRunLevel": {
    "runLevel": "INCUBATION",
    "targets": "frameworkIncubator, instanceProviderChain, valueFunctionResolverChain"
  },
  "runtimeRunLevel": {
    "runLevel": "RUNTIME",
    "targets": "requestGateway"
  },
  "applicationRunLevel": {
    "runLevel": "APPLICATION",
    "targets": "csvReconService"
  }
}
```

**Kein Gateway nötig?** Setze `targetRunLevel: INCUBATION`. Der RUNTIME-Level
und APPLICATION-Level werden nicht aktiviert. Fertig.


## Gateway-Architektur

Das Gateway ist ein separater Prozessor (IRequestGatewayProcessor), der im
RunLevel RUNTIME aktiviert wird. Er ist **nicht Teil des Kernels**.

```
HTTP Request
    │
    ▼
PfwRestController (@RestController)
    │  delegiert an gateway.processRequest(request)
    │
    ▼
DefaultRequestGatewayProcessor
    │  requestDispatcher.dispatchRequest(request)
    │  responseDispatcher.dispatchResponse(response)
    │
    ▼
RequestDispatcherProcessorChain
    │  findet den passenden Handler
    │
    ▼
Fachlicher RequestProcessor
```

Der PfwRestController ist **EINE Stelle für alle REST-Endpoints** — nicht
verteilt über KernelProcessor und verschiedene Service-Klassen.


## ProcessorScope.provided

Ein neuer Scope `provided` ermöglicht deklarative Abhängigkeiten über RunLevel-Grenzen:

```java
@ProcessorParameter(value = "requestGateway@provided",
    description = "Das Gateway — muss vom RunLevel RUNTIME bereitgestellt sein")
private IRequestGatewayProcessor gateway;
```

`@provided` bedeutet: "Dieser Prozessor muss bereits im Kontext existieren.
Ich erstelle ihn nicht — ich erwarte ihn von einem früheren RunLevel."

Bei fehlender Bereitstellung: `ProcessorNotProvidedException` (kein Fallback).


## Unix-Analogie

| Unix         | PFW                        |
|--------------|----------------------------|
| Kernel       | KernelProcessor            |
| PID 1        | KernelProcessor            |
| systemd      | DefaultRunLevelManager     |
| Unit-File    | DefaultRunLevelProcessor   |
| Service      | Target-Prozessor           |
| runlevel 3   | RunLevel.RUNTIME           |
| runlevel 5   | RunLevel.APPLICATION       |
| Unit-Depends | @ProcessorParameter @provided |