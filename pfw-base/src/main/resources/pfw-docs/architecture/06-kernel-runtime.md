# Kernel und RunLevels

## Kernprinzip

Der Kernel ist **kein Orchestrator** — er ist ein **Bootstrap-Prozess**.

```
KernelProcessor → RunLevelManager → RunLevelProcessors → Targets
     (PID 1)         (systemd)          (unit files)      (services)
```

Der Kernel macht genau eine Sache: Er erstellt den RootContext (RunLevel 0: BOOTSTRAP)
und übergibt dann die Kontrolle an den RunLevelManager. Alles weitere ist "User Space".


## RunLevels als konfigurierbare Systemzustände

RunLevels sind **kein Java-Enum** — sie sind frei konfigurierbare Parameter (Name + Rang).
Neue RunLevels sind ohne Code-Änderungen möglich.

Vordefinierte Konstanten (Convenience, nicht zwingend):

```java
public final class RunLevels {
    public static final String BOOTSTRAP   = "BOOTSTRAP";   // rank 0
    public static final String INCUBATION  = "INCUBATION";  // rank 10
    public static final String RUNTIME     = "RUNTIME";     // rank 20
    public static final String APPLICATION = "APPLICATION"; // rank 30
    public static final String SHUTDOWN    = "SHUTDOWN";    // rank 99
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
    │  advanceTo("APPLICATION", taskCtx)
    │  Iteriert alle konfigurierten RunLevelProcessors (sortiert nach rank)
    │
    ├─▶ DefaultRunLevelProcessor [name="INCUBATION", rank=10]
    │       targets: [frameworkIncubator, instanceProviderChain, ...]
    │
    ├─▶ DefaultRunLevelProcessor [name="RUNTIME", rank=20]
    │       targets: [requestGateway, pfwRestController]
    │
    └─▶ DefaultRunLevelProcessor [name="APPLICATION", rank=30]
            targets: [csvReconService, xmlReconService, ...]
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
5. advanceTo(targetRunLevel, taskCtx) wird aufgerufen
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
    "runLevels": "incubationLevel, runtimeLevel, applicationLevel"
  },
  "incubationLevel": {
    "runLevelName": "INCUBATION",
    "rank": "10",
    "targets": "frameworkIncubator, instanceProviderChain, valueFunctionResolverChain"
  },
  "runtimeLevel": {
    "runLevelName": "RUNTIME",
    "rank": "20",
    "targets": "requestGateway, pfwRestController"
  },
  "applicationLevel": {
    "runLevelName": "APPLICATION",
    "rank": "30",
    "targets": "csvReconService, xmlReconService"
  }
}
```

**Kein Gateway nötig?** Setze `targetRunLevel: INCUBATION`. Die höheren RunLevels
werden nicht aktiviert. Fertig.

**Eigene RunLevels?** Einfach neue DefaultRunLevelProcessor mit eigenem Namen und
Rang konfigurieren — kein Java-Code nötig.

**CLI-Parameter:** `--pfw.target-runlevel=RUNTIME` setzt das Ziel-RunLevel zur Startzeit.


## Gateway-Architektur

Das Gateway ist ein separater Prozessor (IRequestGatewayProcessor), der im
RunLevel RUNTIME aktiviert wird. Er ist **nicht Teil des Kernels**.

```
HTTP Request
    │
    ▼
PfwRestController (@RestController)
    │  POST /api/process → gateway.processRequest(beanParameterMap)
    │  GET  /api/docs/{provider}/{name} → gateway.processRequest(assetRequest)
    │
    ▼
DefaultRequestGatewayProcessor
    │  Chain-of-Responsibility: erster zuständiger Dispatcher gewinnt
    │  dispatcher.isResponsibleForRequest(request) → dispatchRequest(request)
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


## Dynamische Dispatcher-Registrierung

Services können sich zur Laufzeit am Gateway registrieren:

```java
// Im Service-Processor:
@Override
public void processorOnInit() {
    if (gateway != null && csvReconDispatcher != null) {
        boolean registered = gateway.registerDispatcher(csvReconDispatcher);
        // false = abgelehnt durch allowedDispatcherSet-Policy
    }
}
```

### allowedDispatcherSet — Policy als Prozessor

Das Gateway kann über einen `ISetProcessor<String>` konfiguriert werden,
der nur bestimmte Dispatcher zulässt:

```json
{
  "requestGateway": {
    "dispatchers": "jsonDispatcher, assetDispatcher",
    "allowedDispatcherSet": "gatewayPolicy@instance"
  },
  "gatewayPolicy": {
    "members": "jsonDispatcher, assetDispatcher, csvReconDispatcher"
  }
}
```

Dispatcher, deren prototypeId nicht in der Menge ist, werden abgelehnt.
Wenn `allowedDispatcherSet` nicht konfiguriert ist, werden alle akzeptiert.


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

| Unix           | PFW                          |
|----------------|------------------------------|
| Kernel         | KernelProcessor              |
| PID 1          | KernelProcessor              |
| systemd        | DefaultRunLevelManager       |
| Unit-File      | DefaultRunLevelProcessor     |
| Service        | Target-Prozessor             |
| runlevel 3     | RunLevel "RUNTIME" (rank=20) |
| runlevel 5     | RunLevel "APPLICATION" (rank=30) |
| Unit-Depends   | @ProcessorParameter @provided |
| /etc/rc.conf   | beanParameterMap             |