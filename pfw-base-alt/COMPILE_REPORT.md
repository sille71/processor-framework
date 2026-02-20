# PFW Compile-Check Report

## Zusammenfassung

| Kategorie | Anzahl | Status |
|-----------|--------|--------|
| Java-Dateien extrahiert | 277 | ✅ |
| Packages | 59 | ✅ |
| Gesamtzeilen Java | 29.098 | ✅ |
| Fehlende interne Klassen | 6 | ⚠️ Lösbar |
| Legacy-Imports (de.dzbank) | 11 in 26 Dateien | 🔧 Zu migrieren |
| javax → jakarta Migration | 11 Dateien | 🔧 Zu migrieren |
| Optionale Abhängigkeiten | 4 | ℹ️ Optional |

---

## 1. Fehlende interne Klassen (de.starima.pfw.base.*)

Diese 6 Klassen werden importiert, existieren aber nicht im Projekt:

### 1.1 ReconConfiguration (5 Dateien betroffen)
**Package:** `de.starima.pfw.base.migration.domain.configuration`
**Betroffene Dateien:**
- `IProcessorContext.java`
- `DefaultProcessorContext.java`
- `StringConfigurationParameterProvider.java`
- `ConfigTransformerProcessor.java`
- `ProcessorUtils.java`

**Einschätzung:** Legacy-Domänenklasse aus dem Recon. Wird im ConfigTransformerProcessor und den ParameterProvidern referenziert. Entweder als leeres Skeleton erstellen oder die betroffenen Stellen refaktorieren, da ReconConfiguration fachspezifisch ist und nicht ins Base-Framework gehört.

### 1.2 Formatter-Klassen (3 Dateien betroffen)
- `IFormatterProcessor` → in DefaultAttributeConditionProcessor, DefaultAttributeHelperProcessor
- `IFormatterProviderProcessor` → in DefaultAttributeHelperProcessor
- `AttributeNumberFormatter` → in CalculationHelper

**Einschätzung:** Formatter-Package fehlt komplett. Entweder nachliefern oder als eigenes Modul planen (pfw-formatter). Betrifft nur das Attribute-Subsystem.

### 1.3 Migration-Klassen (2 Dateien betroffen)
- `IConfigurationMigrationProcessor`
- `GlobalConfigurationMigrationProcessor`

**Einschätzung:** Betrifft nur ClassPathParameterProvider und StringConfigurationParameterProvider. Migration alter XML-Konfigurationen — kann als separates Modul nachgeliefert werden.

---

## 2. Legacy-Imports (de.dzbank.* → de.starima.pfw.*)

### 2.1 Utility-Klassen — EINFACH zu lösen

| Legacy-Klasse | Verwendet in | Empfehlung |
|---------------|-------------|------------|
| `LogOutputHelper` | 12 Dateien | Eigene Implementierung unter `de.starima.pfw.base.util`. Nutzt nur `getModelAsStringBuffer()` für Debug-Ausgaben. Kann durch Jackson ObjectMapper oder simple toString() ersetzt werden. |
| `Compression` | 1 Datei (AbstractProcessor) | Nur als Import, nicht aktiv genutzt. Import entfernen. |
| `BeanUtilities` | 1 Datei (DefaultAttributeHelperProcessor) | Spring BeanUtils oder eigene Implementierung. |
| `StringComparator` | 1 Datei (CalculationHelper) | Durch `Comparator.naturalOrder()` o.ä. ersetzen. |

### 2.2 Alte Framework-Interfaces — Umbenennung nötig

| Legacy-Klasse | Verwendet in | Empfehlung |
|---------------|-------------|------------|
| `IReconProcessor` | 19 Dateien | Systematisch durch `IProcessor` ersetzen. Die meisten Stellen sind `isAssignableFrom(IReconProcessor.class)` Checks in ProcessorUtils. |
| `IReconProcessorLifecycleListener` | DefaultProcessorProvider | Neues Interface `IProcessorLifecycleListener` in `de.starima.pfw.base.processor.api` erstellen. |
| `ReconManagerHelper` | DefaultProcessorProvider | In ProcessorUtils integrieren oder eigene Klasse. |

### 2.3 Wildcard-Imports auf alte Packages

| Import | Datei | Empfehlung |
|--------|-------|------------|
| `de.dzbank.recon.ms.base.processor.description.api.*` | 8 Dateien | Bereits als `de.starima.pfw.base.processor.description.api.*` vorhanden. Imports ersetzen. |
| `de.dzbank.recon.ms.base.processor.description.config.api.*` | 3 Dateien | Bereits als `de.starima.pfw.base.processor.description.config.api.*` vorhanden. |
| `de.dzbank.recon.ms.base.processor.description.incubator.*` | 2 Dateien | Bereits als `de.starima.pfw.base.processor.description.incubator.*` vorhanden. |

---

## 3. javax → jakarta Migration (Spring Boot 3)

Spring Boot 3 hat von `javax.*` auf `jakarta.*` umgestellt. Betroffen:

| Alt | Neu | Dateien |
|-----|-----|---------|
| `javax.xml.bind.*` | `jakarta.xml.bind.*` | ConditionConfig, Condition, ProcessorUtils |
| `javax.annotation.PostConstruct` | `jakarta.annotation.PostConstruct` | IValueConfig, AbstractServiceProcessorRuntime, AbstractInstanceProcessorRuntime, KernelProcessor |
| `javax.servlet.*` | `jakarta.servlet.*` | AbstractServiceProcessorRuntime, AbstractInstanceProcessorRuntime, KernelProcessor, AssetHttpRequest |

**Lösung:** Globales Suchen/Ersetzen:
```
javax.xml.bind    → jakarta.xml.bind
javax.annotation  → jakarta.annotation
javax.servlet     → jakarta.servlet
```

---

## 4. Optionale Abhängigkeiten

| Bibliothek | Verwendet in | Status |
|-----------|-------------|--------|
| `com.openhtmltopdf` | MarkdownDocRendererProcessor | Für PDF-Export aus Markdown. Optional. |
| `com.vladsch.flexmark` | DocUtils, MarkdownDocRendererProcessor | Markdown-Parsing. Optional. |
| `Spring Batch (ItemProcessor)` | IItemTransformatorProcessor, ITransformerProcessor, IConditionProcessor | Nur das Interface. Kann durch eigenes Interface ersetzt werden. |
| `Groovy` | Referenziert in Variablen | Für Groovy-basierte Expressions. Optional. |

---

## 5. Nicht extrahierte Klassen (Bewertung)

### Bewusst ausgelassen (Shape-Konzept — entfällt laut Silvio):
- `ShapeResolveProcessor`, `ShapeResolverProcessor`
- `StructShape`, `CollectionShape`, `ProcessorShape`, `PolymorphicShape`, `ReferenceShape`
- `ValueProbeFactoryProcessor`

### Konzeptentwürfe im falschen Package (unter de.starima.core.typeref geparkt):
Diese Klassen ab Zeile ~20500 in doc_pfw.txt hatten keine saubere Package-Deklaration und wurden als Gesprächsfragmente / Konzeptentwürfe erkannt:
- `DescriptorBuildProcessor`, `DescriptorBuilderProcessor`, `DescriptorServiceProcessor`
- `StructDescriptorProcessor`, `PropertyDescriptorProcessor`, `CollectionDescriptorProcessor`, `ProcessorDescriptorProcessor`
- `IDescriptorGraphBuilder`, `DescriptorGraphBuilderProcessor`
- `ProcessorResolverProcessor`, `ScalarResolverProcessor`
- `IConstructionPort`, `IConstructionPortProcessor`, `ProcessorAssembler`
- `ProcessorSandbox`, `TestCaseRunnerProcessor`, `TestCaseParameter`, `TestMapGenerator`
- `ProcessorLoggerProcessor`, `LogbookServiceProcessor`, `ProcessorLogEvent`

**Empfehlung:** Diese gehören in separate Module (pfw-test, pfw-logging) und müssen als eigenständige Klassen mit korrektem Package neu angelegt werden.

---

## 6. Empfohlene Reihenfolge zum Compilen

### Phase 1: Compile-fähig machen (Quick Wins)
1. **javax → jakarta** — Globales Suchen/Ersetzen (11 Dateien)
2. **de.dzbank Wildcard-Imports** → de.starima Imports ersetzen (13 Dateien)
3. **LogOutputHelper** — Stub erstellen unter `de.starima.pfw.base.util.LogOutputHelper`
4. **Compression Import** — aus AbstractProcessor entfernen
5. **IReconProcessor-Referenzen** → durch IProcessor ersetzen

### Phase 2: Fehlende Interfaces/Klassen erstellen
6. **ReconConfiguration** — Leeres Skeleton oder Interface erstellen
7. **IProcessorLifecycleListener** — Neues Interface (3 Methoden)
8. **IFormatterProcessor / IFormatterProviderProcessor** — Leere Interfaces
9. **IConfigurationMigrationProcessor** — Leeres Interface

### Phase 3: Optionale Dependencies entscheiden
10. Spring Batch ItemProcessor → eigenes Interface oder Dependency behalten?
11. Flexmark/OpenHTMLtoPDF → als optionale Dependency oder separates Modul?

### Phase 4: Architektur-Bereinigung
12. ProcessorUtils (2042 Zeilen) — Schrittweise in TypeRefProvider/ConfigProvider/InstanceProvider zerlegen
13. ReconConfiguration-Referenzen aus dem Base-Framework herauslösen
14. Migration-Klassen in eigenes Modul verschieben
