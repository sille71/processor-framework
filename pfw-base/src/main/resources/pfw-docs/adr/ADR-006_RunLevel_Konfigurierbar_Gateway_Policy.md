# ADR-006: Konfigurierbare RunLevels + Gateway Dispatcher-Policy

## Status
Akzeptiert

## Datum
2026-03-11

## Kontext

Drei Probleme im bestehenden Design wurden adressiert:

1. **RunLevel war ein Java-Enum** — ein Fremdkörper im Processor-First-Modell.
   Neue RunLevels erforderten Code-Änderungen. Das widersprach dem Grundprinzip
   "alles ist konfigurierbar".

2. **Das Gateway hatte keine Policy** für die Dispatcher-Zulassung.
   Services konnten beliebig Dispatcher registrieren — kein Kontrollmechanismus.

3. **REST-Endpoints waren über drei Klassen verteilt** (AbstractInstanceProcessorRuntime,
   AbstractServiceProcessorRuntime, KernelProcessor) — unübersichtlich, schwer wartbar,
   kein Single Point of Configuration.


## Entscheidung

### 1. RunLevel als konfigurierbare Parameter

Das `RunLevel`-Enum wird durch zwei freie Parameter ersetzt:

- `IRunLevelProcessor.getRunLevelName()` — frei wählbarer String
- `IRunLevelProcessor.getRank()` — int für Sortierung

```java
// Vorher:
public enum RunLevel { BOOTSTRAP(0), INCUBATION(1), RUNTIME(2), APPLICATION(3) }

// Nachher:
@ProcessorParameter(description = "Name des RunLevels")
private String runLevelName;  // z.B. "DATA_IMPORT"

@ProcessorParameter(description = "Rang für Sortierung", value = "0")
private int rank;  // z.B. 25
```

Convenience-Konstanten in `RunLevels.java` für Standard-Namen, aber nicht erzwungen.

CLI-Parameter: `--pfw.target-runlevel=<name>` (String statt Enum).


### 2. Gateway mit Dispatcher-Policy

`IRequestGatewayProcessor` erhält eine neue Methode:

```java
boolean registerDispatcher(IRequestDispatcherProcessor dispatcher);
```

`DefaultRequestGatewayProcessor` implementiert eine `allowedDispatcherSet`-Policy:

- Wenn `allowedDispatcherSet` konfiguriert: nur Dispatcher mit bekannter prototypeId akzeptiert
- `allowedDispatcherSet` ist ein `ISetProcessor<String>` — selbst ein Prozessor,
  damit inspizierbar, konfigurierbar und austauschbar
- Duplikat-Prüfung via `getFullBeanId()`

Wenn nicht konfiguriert: alle Dispatcher akzeptiert (open policy).


### 3. REST-Controller konsolidiert

`PfwRestController` ist die **einzige** Klasse mit `@RestController`.

Zwei Endpoints:
- `POST /api/process` — beanParameterMap als JSON Body → Gateway
- `GET /api/docs/{provider}/{name}` — Asset-Download → Gateway

Alle anderen REST-Annotations aus `AbstractServiceProcessorRuntime` und
`AbstractInstanceProcessorRuntime` wurden entfernt.


## Begründung

**Processor-First-Konsistenz:** RunLevels folgen jetzt demselben Muster wie alle
anderen Prozessoren — konfigurierbar via beanParameterMap, keine Java-Enums.

**Erweiterbarkeit ohne Code:** Anwendungen können eigene RunLevels (z.B. "DATA_IMPORT",
"WARMING_UP", "DEGRADED") einfach konfigurieren.

**Policy als Prozessor:** `ISetProcessor<String>` ist keine Sonderlösung — es ist
derselbe Mechanismus wie überall im Framework. Die Policy ist selbst konfigurierbar,
testbar und dokumentierbar.

**Single Responsibility:** PfwRestController hat genau eine Aufgabe — HTTP zu
Gateway übersetzen. Service-Klassen kümmern sich nicht mehr um Transport.


## Verifikation

- [x] RunLevel-Enum gelöscht (`RunLevel.java` entfernt)
- [x] `RunLevels.java` Konstanten-Klasse erstellt
- [x] `IRunLevelProcessor`: `getRunLevelName()`, `getRank()`
- [x] `IRunLevelManager`: `advanceTo(String, ITaskContext)`, `advanceToRank(int, ITaskContext)`
- [x] `KernelProcessor`: `targetRunLevel` als `String`
- [x] `IRequestGatewayProcessor`: `registerDispatcher(IRequestDispatcherProcessor)`
- [x] `DefaultRequestGatewayProcessor`: `allowedDispatcherSet` + Duplikat-Prüfung
- [x] `PfwRestController`: nur `POST /api/process` + `GET /api/docs/{...}/{...}`
- [x] `@RestController` aus `AbstractServiceProcessorRuntime` entfernt
- [x] `@RestController` aus `AbstractInstanceProcessorRuntime` entfernt
- [x] `mvn compile` grün


## Alternativen verworfen

**RunLevel als Interface-Konstanten:** Besser als Enum, aber immer noch
kein echter Prozessor. Verworfen zugunsten vollständiger Konfigurierbarkeit.

**Gateway-Policy als Map:** Möglich, aber nicht inspizierbar. ISetProcessor
ist expliziter und folgt dem Framework-Muster.

**REST-Endpoints in Kernel behalten:** Pragmatisch, aber verletzt das
Single-Responsibility-Prinzip. PfwRestController ist klarer.