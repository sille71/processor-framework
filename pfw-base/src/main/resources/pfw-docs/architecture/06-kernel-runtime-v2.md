# Kernel, RunLevels und Communication Gateway

> *RunLevels sind Zustände, keine Abläufe.*
> *Der Incubator baut Zustände, der Kernel verwaltet sie.*


## Architekturprinzip: Der Kernel startet nur den Orchestrator

Unix: Der Kernel startet PID 1, dann übernimmt der User Space.
PFW: Der Kernel erstellt den RootContext (Bootstrap), dann übernimmt
der RunLevelManager. Der Kernel kennt keine fachliche Logik,
keine Services, keine Requests.


## Die Prozessor-Hierarchie

```
KernelProcessor
  └── @ProcessorParameter runLevelManager: IRunLevelManager
        └── @ProcessorParameter runLevels: List<IRunLevelProcessor>
              │
              ├── RunLevel BOOTSTRAP (rank=0)
              │     └── targets: [kernelBeanProvider, kernelContextProvider]
              │
              ├── RunLevel INCUBATION (rank=1)
              │     └── targets: [frameworkIncubator, instanceProviderChain,
              │                   valueFunctionResolverChain, typeRefProviderChain]
              │
              ├── RunLevel RUNTIME (rank=2)
              │     └── targets: [requestGateway]
              │
              └── RunLevel APPLICATION (rank=3)
                    └── targets: [csvReconService]
```

Alles sind Prozessoren. Alles hat @ProcessorParameter.
Alles ist über beanParameterMap konfigurierbar.
Kein Enum-Sonderkonzept, kein CapabilityProvider, kein PhaseProcessor.


## RunLevels als Verfügbarkeitsverträge

Ein RunLevel sagt nicht WIE etwas gemacht wird, sondern WAS
garantiert verfügbar ist:

```
BOOTSTRAP:      KernelContext, BeanProvider, Logging
INCUBATION:     FrameworkIncubator, InstanceProviderChain, Descriptoren
RUNTIME:        Gateway, Dispatcher, Security
APPLICATION:    Fachliche Services
SHUTDOWN:       Geordnetes Herunterfahren
```

Kein Prozessor darf Fähigkeiten nutzen, die sein RunLevel
nicht garantiert.


## Flexibilität

Kein Gateway nötig? → RunLevel RUNTIME weglassen oder
`--pfw.target-runlevel=INCUBATION` per CLI.

Zusätzliches RunLevel? → Neuen RunLevelProcessor konfigurieren,
in die beanParameterMap des RunLevelManagers eintragen. Kein Code.


## Bootstrap vs. Selbstorganisation

```
RunLevel 0 (BOOTSTRAP):  Hardcoded im Kernel.
                          KernelContext-Erstellung, BeanProvider.
                          Kein Incubator verfügbar.

RunLevel 1+ :             Der RunLevelManager übernimmt.
                          Selbstorganisierend.
                          Targets werden über den Incubator erzeugt.
```

Das ist der harte Übergangspunkt — wie PID 1 in Unix.


## Communication Gateway

```java
IRequestGatewayProcessor (scope=instance, erzeugt in RunLevel RUNTIME)
  ├── requestDispatcher: RequestDispatcherChain
  │     ├── JsonRequestDispatcher
  │     ├── AssetRequestDispatcher
  │     ├── MultipartRequestDispatcher
  │     └── DescriptorRequestDispatcher
  └── responseDispatcher: ResponseDispatcherChain

PfwRestController (EINE Stelle für alle REST-Endpoints)
  └── gateway: IRequestGatewayProcessor @provided
```

Der REST-Controller ist ein eigener Prozessor, der das Gateway
via @provided findet. Kein Kopieren von Endpoints in 3 Klassen.


## Scope `provided` im Kontext der RunLevels

```java
@ProcessorParameter(value = "requestGateway@provided")
private IRequestGatewayProcessor gateway;
```

"Ich brauche das Gateway. Es MUSS schon da sein."
Wenn das Gateway in RunLevel 2 erzeugt wird und der Service
in RunLevel 3 startet, findet er es über den Kontext.
Wenn RunLevel 2 übersprungen wurde → Fehler beim Start des Service.
Das ist die deklarierte Abhängigkeit.


## beanParameterMap als systemd-Unit-Format

Die gesamte Systemkonfiguration ist eine beanParameterMap:

```json
{
  "kernelProcessor": {
    "targetRunLevel": "APPLICATION",
    "runLevelManager": "defaultRunLevelManager@instance"
  },
  "defaultRunLevelManager": {
    "runLevels": "incubationRunLevel, runtimeRunLevel, applicationRunLevel"
  },
  "incubationRunLevel": {
    "runLevel": "INCUBATION",
    "targets": "frameworkIncubator, instanceProviderChain"
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

Änderbar ohne Code. Inspizierbar über Descriptoren.
Die bestehende beanParameterMap aus der Produktion funktioniert weiter.


## Unix-Analogie

```
Unix Kernel         = KernelProcessor (Bootstrap, minimal)
PID 1 / systemd     = RunLevelManager (orchestriert Zustände)
systemd Target      = RunLevelProcessor (deklarativer Zielzustand)
systemd Unit        = Target-Prozessor (ein konkreter Service)
Unit-File           = Eintrag in der beanParameterMap
runlevel N          = --pfw.target-runlevel=N
```

> *Biologisch: Entwicklungsstadien eines Organismus. Die Befruchtung
> (Bootstrap) ist hardcoded. Danach übernimmt das morphogenetische
> Feld (RunLevelManager) und aktiviert Gene (Prozessoren) Schritt
> für Schritt, bis der Organismus (Application) lebensfähig ist.*
