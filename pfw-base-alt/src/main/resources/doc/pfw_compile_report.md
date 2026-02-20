# PFW Compile-Check Report

**Projekt:** pfw-base | **Dateien:** 277 Java | **Zeilen:** 19.492 | **Packages:** 59

---

## Ergebnis-Übersicht

| Kategorie | Anzahl | Aufwand | Priorität |
|-----------|--------|---------|-----------|
| Automatisch fixbar (Search-Replace) | 18 Dateien | 5 min | ⬛ Sofort |
| LogOutputHelper ersetzen | 12 Dateien | 30 min | ⬛ Sofort |
| Legacy-Utils ersetzen | 4 Dateien | 1 h | 🟧 Hoch |
| Fehlende Klassen erstellen | 6 Klassen | 2 h | 🟧 Hoch |
| Legacy-Interfaces migrieren | 3 Dateien | 30 min | 🟧 Hoch |
| pom.xml Dependencies ergänzen | 3 Libs | 10 min | 🟨 Mittel |
| javax → jakarta Migration | 7 Dateien | 10 min | 🟨 Mittel |

**Geschätzter Gesamtaufwand bis zum ersten erfolgreichen `mvn compile`: ~4-5 Stunden**

---

## 1. Automatisch fixbar: Alte Package-Referenzen (Search-Replace)

### 1a) de.dzbank.recon.ms.base → de.starima.pfw.base (9 Dateien)

Diese Dateien importieren noch das alte Package, obwohl die Klassen bereits unter `de.starima.pfw.base` liegen:

```
de.dzbank.recon.ms.base.processor.description.api.*        → de.starima.pfw.base.processor.description.api.*
de.dzbank.recon.ms.base.processor.description.config.api.*  → de.starima.pfw.base.processor.description.config.api.*
de.dzbank.recon.ms.base.processor.description.incubator.api.*    → de.starima.pfw.base.processor.description.incubator.api.*
de.dzbank.recon.ms.base.processor.description.incubator.domain.* → de.starima.pfw.base.processor.description.incubator.domain.*
```

**Betroffene Dateien:**
- DefaultMapValueFunction, DefaultParameterDescriptor, DefaultStructureValueDescriptor
- ProcessorDescriptorConfigProvider, ProcessorParameterDescriptorConfigProvider, ValueObjectDescriptorConfigProvider
- FrameworkIncubator, IIncubator, KernelProcessor

### 1b) javax → jakarta (7 Dateien, Spring Boot 3.x)

```
javax.annotation.PostConstruct    → jakarta.annotation.PostConstruct
javax.annotation.PropertyKey      → (prüfen ob noch benötigt)
javax.servlet.http.HttpServletRequest → jakarta.servlet.http.HttpServletRequest
```

**Betroffene Dateien:**
- KernelProcessor, AbstractInstanceProcessorRuntime, AbstractServiceProcessorRuntime
- AssetHttpRequest, IValueConfig

---

## 2. LogOutputHelper ersetzen (12 Dateien)

`de.dzbank.components.utils.log.LogOutputHelper` ist eine Logging-Hilfsklasse, die `Map`-Inhalte als String formatiert. Wird hauptsächlich für Debug-Ausgaben der beanParameterMap benutzt.

**Empfehlung:** Eigene minimale Utility-Klasse unter `de.starima.pfw.base.util`:

```java
package de.starima.pfw.base.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class LogOutputHelper {
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static String getModelAsStringBuffer(Map<String, ?> map, Object unused) {
        try { return mapper.writeValueAsString(map); }
        catch (Exception e) { return String.valueOf(map); }
    }
}
```

**Betroffene Dateien:** AbstractProcessor, DefaultBeanProvider, DefaultProcessorProvider, DefaultSearchFilterProcessor, KernelProcessor, DefaultMultipartHttpServletRequestDispatcherProcessor, JsonRequestDispatcherProcessor, RequestDispatcherProcessorChain, AbstractInstanceProcessorRuntime, AbstractServiceProcessorRuntime, CalculationHelper, MapUtils

---

## 3. Legacy-Utility-Klassen ersetzen (4 Dateien)

| Alte Klasse | Verwendet in | Empfehlung |
|-------------|-------------|------------|
| `Compression` | AbstractProcessor (1×) | Apache Commons Compress oder eigene Wrapper |
| `BeanUtilities` | DefaultAttributeHelperProcessor (1×) | Spring BeanUtils oder eigene Reflection-Methode |
| `StringComparator` | CalculationHelper (1×) | Apache Commons Lang StringUtils / eigener Comparator |
| `ReconManagerHelper` | DefaultVariableProcessor (1×) | Prüfen was genau gebraucht wird, vermutlich eliminierbar |

---

## 4. Fehlende Klassen (6 Stück)

### 4a) Package `de.starima.pfw.base.processor.formatter` (3 Klassen)

| Klasse | Typ | Verwendet von |
|--------|-----|---------------|
| `AttributeNumberFormatter` | Klasse | CalculationHelper |
| `IFormatterProcessor` | Interface | DefaultAttributeHelperProcessor |
| `IFormatterProviderProcessor` | Interface | DefaultAttributeConditionProcessor, DefaultAttributeHelperProcessor |

**Empfehlung:** Erstmal leere Interfaces/Stubs erstellen:

```java
package de.starima.pfw.base.processor.formatter.api;
public interface IFormatterProcessor extends IProcessor { ... }
public interface IFormatterProviderProcessor extends IProcessor { 
    IFormatterProcessor getFormatterForSubject(Object subject);
}
```

### 4b) Package `de.starima.pfw.base.processor.migration` (2 Klassen)

| Klasse | Typ | Verwendet von |
|--------|-----|---------------|
| `GlobalConfigurationMigrationProcessor` | Klasse | ClassPathParameterProvider |
| `IConfigurationMigrationProcessor` | Interface | StringConfigurationParameterProvider |

**Kontext:** Wird für die Migration alter XML-Konfigurationen benutzt. Kann für das neue Projekt als Stub implementiert oder die Referenzen können entfernt werden, wenn XML-Migration nicht mehr gebraucht wird.

### 4c) ReconConfiguration (1 Domain-Klasse)

| Klasse | Verwendet von |
|--------|---------------|
| `ReconConfiguration` | StringConfigurationParameterProvider, ConfigTransformerProcessor |

**Kontext:** Legacy-Domain-Objekt des alten Recon. Die Referenzen in IProcessorContext und DefaultProcessorContext sind bereits auskommentiert. In den verbleibenden 2 Dateien prüfen, ob es durch eine generische Map ersetzbar ist.

---

## 5. Legacy-Interfaces migrieren (3 Dateien)

| Alt | Neu | Dateien |
|-----|-----|---------|
| `IReconProcessor` | `IProcessor` | IParameterFormatterProcessor (extends) |
| `IReconProcessorLifecycleListener` | Neues Interface erstellen | DefaultProcessorProvider (Listener-Liste) |
| `ReconManagerHelper` | Eliminieren oder ersetzen | DefaultVariableProcessor |

**IParameterFormatterProcessor** sollte einfach `extends IProcessor` werden statt `extends IReconProcessor`.

**IReconProcessorLifecycleListener** wird in DefaultProcessorProvider genutzt, um Events nach Prozessor-Erzeugung zu feuern. Empfehlung: Eigenes Interface erstellen:

```java
package de.starima.pfw.base.processor.api;
public interface IProcessorLifecycleListener {
    void onProcessorCreated(IProcessor processor);
    void onProcessorDestroyed(IProcessor processor);
}
```

---

## 6. Fehlende Dependencies in pom.xml

```xml
<!-- Markdown Rendering (MarkdownDocRendererProcessor, DocUtils) -->
<dependency>
    <groupId>com.vladsch.flexmark</groupId>
    <artifactId>flexmark-all</artifactId>
    <version>0.64.8</version>
    <optional>true</optional>
</dependency>

<!-- PDF Rendering (MarkdownDocRendererProcessor) -->
<dependency>
    <groupId>com.openhtmltopdf</groupId>
    <artifactId>openhtmltopdf-pdfbox</artifactId>
    <version>1.0.10</version>
    <optional>true</optional>
</dependency>

<!-- Content Type Detection (DocUtils) -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>2.9.2</version>
    <optional>true</optional>
</dependency>
```

---

## 7. Bereinigungen (bereits durchgeführt)

Beim Aufteilen der Datei wurden folgende Probleme automatisch behoben:

- **TypeRefFormat.java**: 9.548 Zeilen Gesprächstext entfernt (war an Java-Klasse angehängt)
- **DefaultListValueFunction.java**: Doppelte DescriptorConfigProviderChain-Klasse entfernt
- **Concatenated boundaries**: `}package` Verkettungen in 30+ Stellen aufgetrennt
- **ProcessorParameter.java**: War nicht als eigenständige Datei extrahiert — manuell korrigiert

---

## 8. Empfohlene Reihenfolge

1. **Search-Replace** ausführen (alte Packages, javax→jakarta) → 5 min
2. **LogOutputHelper** als eigene Utility erstellen → 15 min
3. **Stub-Interfaces** für Formatter und Migration erstellen → 30 min
4. **IReconProcessor → IProcessor** Migration in 3 Dateien → 15 min
5. **pom.xml** Dependencies ergänzen → 5 min
6. **Erster `mvn compile`** — verbleibende Fehler iterativ fixen
7. **Erster `mvn test`** mit TestInstanceProcessorRuntime
