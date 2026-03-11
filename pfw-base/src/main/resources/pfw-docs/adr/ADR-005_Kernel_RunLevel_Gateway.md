# ADR-005: Kernel-Redesign, RunLevel-System und Gateway-Extraktion

**Status:** Accepted
**Datum:** 2026-03-11
**Kontext:** Kernel-Redesign, Trennung von Verantwortlichkeiten

---

## Kontext

Der KernelProcessor war überladen: Er war gleichzeitig REST-Controller,
Incubator, Bootstrap-Manager, Service-Dispatcher und Phase-Orchestrator.
Dies verletzte das Single Responsibility Principle und machte den Kernel
schwer verständlich, testbar und erweiterbar.

Konkrete Probleme:
1. KernelProcessor hatte `@RestController` + REST-Endpoints (HTTP-Kopplung)
2. KernelProcessor implementierte `IIncubator` (Descriptor-Kopplung)
3. `IPhaseProcessor` war ein imperatives Konzept ("init diese Phase")
4. AbstractProcessor hatte ~15 Methoden für Initialisierungsorchestrierung
5. ProcessorUtils war mit ~30 Hilfsmethoden überfrachtet
6. Kein `ProcessorScope.provided` für deklarative Abhängigkeiten

---

## Entscheidungen

### 1. KernelProcessor → nur Bootstrap + RunLevelManager

**Alt:** KernelProcessor orchestriert alles
**Neu:** KernelProcessor erstellt nur den RootContext, dann übernimmt der RunLevelManager

```java
@PostConstruct
public void initProcessor() {
    this.runtimeContext = kernelContextProvider.createContext(null);
    ProcessorUtils.registerProcessorInScope(this, this.runtimeContext);
    if (runLevelManager != null) {
        runLevelManager.advanceTo(targetRunLevel, taskCtx);
    }
}
```

### 2. IRunLevelManager + IRunLevelProcessor statt IPhaseProcessor

**Alt:** `IPhaseProcessor.initPhase(ctx)` — imperativ, prozessspezifisch
**Neu:** `IRunLevelProcessor.activate(ctx)` — deklarativ, zustandsbasiert

RunLevels sind ZUSTÄNDE (Systemzustände), keine Abläufe.
Der RunLevelManager kennt alle Zustände und organisiert Transitionen.

### 3. RunLevels = deklarative Zustände

```java
public enum RunLevel {
    BOOTSTRAP(0),    // KernelContext, BeanProvider, Logging
    INCUBATION(1),   // FrameworkIncubator, InstanceProviderChain
    RUNTIME(2),      // Gateway, Dispatcher, Security
    APPLICATION(3),  // Fachliche Services
    SHUTDOWN(99);    // Geordnetes Herunterfahren
}
```

Die gesamte Systemkonfiguration ist eine beanParameterMap — änderbar ohne Code.

### 4. IRequestGatewayProcessor als separater Prozessor

**Alt:** REST-Endpoints in KernelProcessor und AbstractInstanceProcessorRuntime
**Neu:** Dedizierter PfwRestController + DefaultRequestGatewayProcessor

Vorteile:
- Einheitliche REST-Schnittstelle an EINER Stelle
- Gateway konfigurierbar via beanParameterMap
- RunLevel-getrieben: Gateway nur wenn nötig

### 5. PfwRestController als EINE Stelle für REST

Alle HTTP-Endpoints gebündelt in einer Klasse statt verteilt über Kernel,
AbstractInstanceProcessorRuntime und AbstractServiceProcessorRuntime.

### 6. ProcessorScope.provided für deklarierte Abhängigkeiten

```java
@ProcessorParameter(value = "requestGateway@provided")
private IRequestGatewayProcessor gateway;
```

`@provided` = "Dieser Prozessor muss bereits im Kontext existieren."
Unterschied zu `@instance`: Instance erzeugt bei Bedarf, provided nie.

### 7. AbstractProcessor entkernt (nur lifecycle-Hooks)

**Entfernt:** initContextProvider, initProcessorDescriptor, initParameters,
resetProcessor, getUsedContextProviderProcessor — wird von ProcessorInstanceProvider erledigt.

**Hinzugefügt:** `processorOnDestroy()` — Lifecycle-Hook für Shutdown/Refresh

**Vereinfacht:** `init(ctx)` setzt nur Context und ruft `processorOnInit()` auf.

### 8. AI Domain-Klassen als ValueObjects

Alle AI Domain-Klassen (AIMessage, BlueprintDraft, etc.) annotiert mit
`@ValueObject` + `@ProcessorParameter` für Framework-Konsistenz und
Hydration/Dehydration über beanParameterMap.

---

## Konsequenzen

### Positiv
- Kernel ist testbar und verständlich (nur 3 Abhängigkeiten)
- Systemkonfiguration vollständig via beanParameterMap
- RunLevel-Konfiguration ist KI-lesbar und -generierbar
- Einheitliche REST-Schnittstelle
- processorOnDestroy() ermöglicht sauberes Shutdown
- `@provided` macht Abhängigkeiten zwischen RunLevels explizit

### Negativ / Trade-offs
- Bootstrap-Problem: RunLevelManager muss Spring-Bean sein (pragmatisch gelöst)
- AbstractInstanceProcessorRuntime noch nicht komplett bereinigt (REST-Endpoints)
- ProcessorUtils-Cleanup noch nicht abgeschlossen (~30 Methoden für später)

---

## Verifikation

- [x] pfw-base kompiliert (84+ Quelldateien)
- [x] pfw-descriptor kompiliert (87+ Quelldateien)
- [x] pfw-runtime kompiliert (101+ Quelldateien)
- [x] RunLevel.java mit neuen Werten (BOOTSTRAP/INCUBATION/RUNTIME/APPLICATION/SHUTDOWN)
- [x] IRunLevelProcessor und IRunLevelManager neu erstellt
- [x] DefaultRunLevelProcessor und DefaultRunLevelManager implementiert
- [x] KernelProcessor vereinfacht (kein REST, kein IIncubator)
- [x] IPhaseProcessor, FullIncubatorPhaseProcessor, FrameworkAdoptionPhaseProcessor gelöscht
- [x] IRequestGatewayProcessor, DefaultRequestGatewayProcessor, PfwRestController neu
- [x] ProcessorScope.provided hinzugefügt
- [x] processorOnDestroy() in IProcessor + AbstractProcessor
- [x] AI Domain-Klassen mit @ValueObject + @ProcessorParameter annotiert