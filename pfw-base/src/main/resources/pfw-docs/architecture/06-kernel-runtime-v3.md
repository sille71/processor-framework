# Kernel, RunLevels und Communication Gateway

> *RunLevels sind Zustände, keine Abläufe.*
> *Der Incubator baut Zustände, der Kernel verwaltet sie.*
> *Kein Enum — RunLevels sind Prozessoren mit Name und Rang.*


## Architekturprinzip

Der Kernel startet nur den RunLevelManager. Alles andere ist "User Space".
Unix: Der Kernel startet PID 1. PFW: Der Kernel erstellt den RootContext,
dann übernimmt der RunLevelManager.


## Die Prozessor-Hierarchie

```
KernelProcessor
  └── @ProcessorParameter runLevelManager: IRunLevelManager
        └── @ProcessorParameter runLevels: List<IRunLevelProcessor>
              │
              ├── "INCUBATION" (rank=10)
              │     └── targets: [frameworkIncubator, instanceProviderChain]
              │
              ├── "RUNTIME" (rank=20)
              │     └── targets: [requestGateway, pfwRestController]
              │
              └── "APPLICATION" (rank=30)
                    └── targets: [csvReconService, xmlReconService]
```

Alles sind Prozessoren. Alles hat @ProcessorParameter. RunLevel-Name und
Rang sind konfigurierbar — kein Enum, kein Sonderkonzept. Neue RunLevels
ohne Code-Änderung möglich:

```json
{
  "dataImportLevel": {
    "runLevelName": "DATA_IMPORT",
    "rank": "15",
    "targets": "csvImporter, dbConnector"
  }
}
```


## RunLevels als Verfügbarkeitsverträge

Ein RunLevel garantiert, dass bestimmte Prozessoren verfügbar sind:

```
BOOTSTRAP (0):      KernelContext, BeanProvider
INCUBATION (10):    FrameworkIncubator, InstanceProviderChain, Descriptoren
RUNTIME (20):       Gateway, Dispatcher, Security
APPLICATION (30):   Fachliche Services
```

Kein Prozessor darf Fähigkeiten nutzen, die sein RunLevel nicht garantiert.
Ein Service mit `gateway@provided` erwartet, dass das Gateway existiert —
wenn RunLevel RUNTIME übersprungen wurde, schlägt die Erzeugung fehl.


## Flexibilität

Kein Gateway nötig? → `--pfw.target-runlevel=INCUBATION`
Eigene RunLevels? → In der beanParameterMap konfigurieren.
Mehrere Services? → Alle als Targets im APPLICATION-RunLevel.
Gestaffelte Services? → Mehrere RunLevels mit verschiedenen Rängen.


## Bootstrap vs. Selbstorganisation

```
RunLevel 0 (BOOTSTRAP):  Hardcoded im Kernel.
                          KernelContext, BeanProvider.
                          Kein Incubator.

RunLevel 1+ :             RunLevelManager übernimmt.
                          Targets über Incubator erzeugt.
                          Selbstorganisierend.
```


## Communication Gateway

Das Gateway ist ein normaler Prozessor mit drei Aspekten:

**Statische Dispatcher:** Konfiguriert als @ProcessorParameter.
**Dynamische Dispatcher:** Services registrieren sich via `registerDispatcher()`.
**Policy:** `allowedDispatcherSet` (ISetProcessor) schränkt die Zulassung ein.

```
DefaultRequestGatewayProcessor
  ├── dispatchers: [jsonDispatcher, assetDispatcher]  (statisch)
  │                 + csvReconDispatcher               (dynamisch registriert)
  ├── responseDispatcher: defaultResponseDispatcher
  └── allowedDispatcherSet: gatewayPolicy              (ISetProcessor)
```

Ein Service registriert seinen Dispatcher in `processorOnInit()`:

```java
public void processorOnInit() {
    gateway.registerDispatcher(csvReconDispatcher);
}
```

Die Registrierung wird gegen die Policy geprüft.
Der effektive Zustand (inkl. Laufzeit-Registrierungen) ist über
`extract()` inspizierbar.


## REST-Controller

EINE Stelle für alle HTTP-Endpoints:

```
POST /api/process                        → beanParameterMap als JSON
GET  /api/docs/{provider}/{assetName}    → Asset-Download
```

Alles geht durch das Gateway. Der Controller ist nur der HTTP-Eingang.
Der `IUrlProviderProcessor` gibt Clients die URL bekannt.


## Unix-Analogie

```
Unix Kernel            = KernelProcessor (Bootstrap, minimal)
PID 1 / systemd        = RunLevelManager (orchestriert Zustände)
systemd Target         = RunLevelProcessor (Name + Rang + Targets)
systemd Unit           = Target-Prozessor (ein konkreter Service)
Unit-File              = Eintrag in der beanParameterMap
runlevel N             = --pfw.target-runlevel=NAME
Allowed/Denied Units   = allowedDispatcherSet (ISetProcessor)
```

> *Biologisch: Die Befruchtung (Bootstrap) ist hardcoded. Danach
> übernimmt das morphogenetische Feld (RunLevelManager) und
> aktiviert Gene (Prozessoren) Schritt für Schritt, bis der
> Organismus (Application) lebensfähig ist. Die Zellmembran
> (Gateway + Policy) kontrolliert, was ein- und ausgeht.*
