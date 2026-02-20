# Analyse: Prozessorframework (PFW) — Code vs. Konzepte

## Übersicht

Die Datei `doc_pfw.txt` umfasst **~40.700 Zeilen** und enthält eine umfangreiche Codebasis unter dem neuen Package `de.starima.pfw.base`. Im Vergleich zur Konzeptdokumentation (die noch `IReconProcessor` referenziert) zeigt sich eine konsequente Umbenennung und Weiterentwicklung.

---

## 1. Namens-Mapping: Alt → Neu

| Konzeptdokumentation (Alt)       | Code in doc_pfw.txt (Neu)                | Status     |
|----------------------------------|------------------------------------------|------------|
| `IReconProcessor`                | `IProcessor`                             | ✅ Umbenannt |
| `AbstractReconProcessor`         | `AbstractProcessor`                      | ✅ Umbenannt |
| `de.dzbank.recon.ms.base.*`      | `de.starima.pfw.base.*`                  | ✅ Neues Package |
| `IContextProviderProcessor`      | `IRuntimeContextProviderProcessor`       | ✅ Präzisiert |
| `ReconContext`                    | `IProcessorContext` (unverändert)        | ✅ Vorhanden |
| `IParameterFunctionProcessor`    | `IValueFunction` (refaktoriert)          | ✅ Modernisiert |
| `AbstractInstanceProcessor`      | `AbstractInstanceProcessorRuntime`       | ✅ Umbenannt |
| `ProcessorUtils`                 | `ProcessorUtils` (unverändert)           | ✅ Vorhanden |
| `DefaultProcessorProvider`       | `DefaultProcessorProvider`               | ✅ Vorhanden |

---

## 2. Kernkonzepte und ihre Realisierung im Code

### Konzept 1: "Everything is a Processor"

**Status: ✅ Vollständig vorhanden**

Das zentrale Interface `IProcessor` (Zeile 15682) definiert den kompletten Lebenszyklus:

- `init(IProcessorContext ctx)` — 4-Phasen-Initialisierung
- `refreshParameters(...)` — Laufzeit-Dynamik
- `extractEffectiveProcessorParameters()` — Selbstinspektion
- `generatePrototypeProcessorDescriptor()` — Selbstbeschreibung
- `generateProcessorDescriptorInstance(LoadStrategy)` — Instanz-Deskriptor
- `createProcessor(...)` — Dynamische Prozessor-Erzeugung

Die `AbstractProcessor` (Zeile 383) implementiert die vollständige 4-Phasen-Initialisierung:
1. **Context-Phase**: `initContextProvider(ctx)` — Kontexthierarchie aufbauen
2. **Descriptor-Phase**: `initProcessorDescriptor(...)` — Blueprint laden
3. **Hydration-Phase**: `initParameters(...)` — Reflection-basierte Parameter-Injektion
4. **Finalisierung**: `processorOnInit()` — Lifecycle-Hook

### Konzept 2: "The Class is the Schema"

**Status: ✅ Vollständig vorhanden**

Die drei Kern-Annotationen sind komplett implementiert:

- **`@Processor`** (Zeile 262): Mit `description`, `categories`, `subCategories`, `tags`, `defaultValues`, `defaultBeanParameterMapFileName`, `descriptorPrototypeIdentifier`, `assetProviderIdentifier`
- **`@ProcessorParameter`** (Zeile 97): Mit `name`, `value`, `range`, `type`, `required`, `ignoreInitialization`, `ignoreRefresh`, `ignoreExtractParameter`, `requiredCategories`, `requiredTags`, `aliases`, `valueFunctionIdentifier`, `valueFunctionPrototypeIdentifier`, `key`
- **`@ValueObject`** (Zeile 224): Mit `description`, `categories`, `tags`, `descriptorPrototypeIdentifier`

**Besonderheit im neuen Code**: Die `@ProcessorParameter`-Annotation hat die alten `parameterFunctionProcessor*`-Attribute als `@Deprecated` markiert und durch `valueFunctionIdentifier` / `valueFunctionPrototypeIdentifier` ersetzt. Das zeigt die Evolution von `IParameterFunctionProcessor` → `IValueFunction`.

### Konzept 3: "The Descriptor" — Selbstbeschreibung

**Status: ✅ Vorhanden, stark erweitert**

Das Descriptor-System wurde massiv ausgebaut. Es gibt jetzt ein vollständiges **Descriptor-Config-System**:

**Neue Config-Schicht** (Package `description.config`):
- `IDescriptorConfig` / `DefaultDescriptorConfig` — Basis-Konfiguration
- `IProcessorParameterConfig` / `ProcessorParameterConfig` — Parameter-Konfiguration
- `IValueConfig` / `ValueConfig` — Wert-Konfiguration
- `IValueObjectConfig` / `ValueObjectConfig` — ValueObject-Konfiguration
- `IProcessorConfig` / `ProcessorConfig` — Prozessor-Konfiguration
- `ICollectionValueConfig` / `CollectionValueConfig` — Listen-Konfiguration
- `IMapValueConfig` / `MapValueConfig` — Map-Konfiguration
- `IValueFunctionConfig` / `ValueFunctionConfig` — ValueFunction-Konfiguration

**TypeRef-System** (komplett neu):
- `ITypeRef` / `DefaultTypeRef` — Sprachübergreifende Typ-Referenzen mit `Kind`-Enum (CLASS, PARAMETERIZED, ARRAY, WILDCARD, TYPE_VARIABLE)
- `ITypeRefProvider`-Chain mit spezialisierten Providern:
  - `StringTypeRefProvider`, `NumberTypeRefProvider`, `BooleanTypeRefProvider`
  - `EnumTypeRefProvider`, `ProcessorTypeRefProvider`, `ValueObjectTypeRefProvider`
  - `ArrayTypeRefProvider`, `ParameterizedTypeRefProvider`
  - `ClassTypeRefProvider` (Fallback)
  - `TypeRefProviderChain` (Orchestrierung)

**DescriptorConfig-Provider-Chain**:
- `DescriptorConfigProviderChain` — Orchestriert alle Provider
- `ProcessorDescriptorConfigProvider` — Für Prozessor-Klassen
- `ProcessorParameterDescriptorConfigProvider` — Für Parameter
- `ScalarValueConfigProvider` — Für skalare Typen
- `CollectionValueConfigProvider` — Für Listen/Arrays
- `ValueObjectDescriptorConfigProvider` — Für ValueObjects

### Konzept 4: "beanParameterMap"

**Status: ✅ Vollständig vorhanden**

Die beanParameterMap-Logik ist durchgängig implementiert:
- `IProcessorContext.getBeanParameterMap()` / `getContextMergedBeanParameterMap()`
- `getBeanParameters(String beanId)` / `getContextMergedBeanParameters(String beanId)`
- Hierarchisches Merging über Kontextkette
- `refreshBeanParameterMap(...)` für Laufzeit-Updates
- `extractEffectiveProcessorParameterMap()` für Rück-Extraktion

Die `fullBeanId`-Syntax `<prototypeIdentifier>:<identifier>@<scope>` ist in `ProcessorUtils.createFullBeanId()` (Zeile 17099) implementiert.

### Konzept 5: Kontexthierarchie

**Status: ✅ Vollständig vorhanden**

- `IProcessorContext` (Zeile 15182): Vollständiges Kontext-Interface mit Parent/Child-Navigation, beanParameterMap, beanIdTypeMap, Prozessor-Registry
- `IRuntimeContextProviderProcessor` (Zeile 15444): Kontext-Erzeugung mit `createContext(...)`, `createInitialzerContext(...)`
- `ITaskContext` (Zeile 15516): Leichtgewichtiger Aufgabenkontext (parallel zum schweren IProcessorContext)
- Spezialisierte Task-Kontexte: `ITransformationContext`, `IDescriptorConstructorContext`, `IValueFunctionConstructorContext`, `IProposalContext`, `IContextCreationContext`, `ITypeResolutionContext`, `IDescriptorConfigCreationContext`
- `LoadStrategy`-Enum: `DEEP`, `SHALLOW`, `LAZY`

### Konzept 6: ValueFunction / typeSignature-System

**Status: ✅ Vorhanden und ausgebaut**

Die `IValueFunction<C extends ITransformationContext, I, O>` ersetzt die alte `IParameterFunctionProcessor` und bietet:
- `transformValue(C context, I input)` — Kontextbewusste Transformation
- `reverseTransformValue(C context, O value)` — Rücktransformation
- `getTypeSignature()` — Sprachübergreifende Typ-Signatur
- `isGeneric()` — Generik-Erkennung
- `isResponsibleForSubject(C context)` — Chain-of-Responsibility

**Konkrete Implementierungen vorhanden**:
- `BooleanValueFunction` → `"boolean"`
- `DefaultListValueFunction` → `"list<...>"` (dynamisch)
- `ProcessorListValueFunction` → `"list<processor>"`
- `DefaultArrayValueFunction` → Array-Handling
- `SimpleMapValueFunction` → Map-Handling
- `ValueObjectMapValueFunction` → `"map<valueObject>"`
- `AbstractValueFunction`, `AbstractCollectionValueFunction` — Basisklassen

### Konzept 7: Incubator-System (NEU)

**Status: 🆕 Komplett neues Konzept**

Ein vollständiges **Incubator-Pattern** im Package `description.incubator`:
- `IIncubator` / `FrameworkIncubator` — Orchestriert den gesamten Build-Prozess
- `IConstructionManager` / `DefaultConstructionManager` — Bau-Management
- `IDescriptorResolver` / `DescriptorResolverChain` — Descriptor-Auflösung
- `ITypeIntrospector` / `TypeIntrospectorChain` — Typ-Inspektion
- Sessions: `IIncubatorSession`, `IConstructSession`, `IDescribeSession`, `IEditSession`
- Policies: `IConstructPolicy`, `IDescribePolicy`, `IEditPolicy`
- Sources: `IConstructSource`, `IDescribeSource`, `IEditSource`
- Domain: `ISeed`, `IBuildTaskContext`, `IConstructTaskContext`, `IDescribeTaskContext`, `IEditTaskContext`

Dies ist eine Abstraktionsschicht über der Descriptor-Erzeugung, die das Erzeugen, Beschreiben und Editieren von Prozessoren systematisiert.

### Konzept 8: Runtime-Infrastruktur

**Status: ✅ Vorhanden**

- `DefaultProcessorProvider` (Zeile 8595): Spring-basierte Prozessor-Registry
- `AbstractInstanceProcessorRuntime` (Zeile 11338): Basis für Microservices
- `TestInstanceProcessorRuntime` (Zeile 24): Konkreter Test-Einstiegspunkt
- `FileSystemParameterProviderProcessor` (Zeile 10176): Filesystem-basierte Parameter
- `ProcessorScope`-Enum: `singleton`, `prototype`, `instance`, `unknown`
- `StarimaBaseApplication` (Zeile 19880): Spring Boot Starter

---

## 3. Weitere vorhandene Subsysteme

| Subsystem | Klassen | Status |
|-----------|---------|--------|
| **Assets** | `IAssetProviderProcessor`, `DocumentAssetProviderProcessor`, `AbstractAssetProviderProcessor`, `Asset` | ✅ Vorhanden |
| **Variables** | `IVariableProcessor`, `DefaultVariableProcessor` | ✅ Vorhanden |
| **Transformatoren** | `ITransformerProcessor`, `IValueTransformerProcessor`, `IConfigurationModelTransformer`, `JacksonValueTransformerProcessor`, `ConfigTransformerProcessor` | ✅ Vorhanden |
| **Utilities** | `ProcessorUtils` (~2000 Zeilen), `MapUtils`, `ArtifactIdentifier`, `Version`, `DocUtils`, `StreamUtils`, `CalculationHelper` | ✅ Vorhanden |
| **Artifact-System** | `IArtifact`, `IArtifactProvider`, `ArtifactIdentifier`, `ArtifactIdentifierComparator` | ✅ Vorhanden |
| **Domain** | `ProcessorScope`, `ConfigurationModel`, `MapDetailLevel`, `SerializationTarget` | ✅ Vorhanden |
| **Set-Prozessoren** | `ISetProcessor` (Definitionsbereich / Domain) | ✅ Referenziert |

---

## 4. Was fehlt / Was ist noch "abgespeckt"?

### Definitiv fehlend (nur als Referenz/Konzept):
1. **LogBookProcessor / RuntimeShell**: Nur als Konzept in den hinteren Zeilen (~35000+), nicht als lauffähiger Code unter `de.starima.pfw.*`
2. **ProcessorSandbox**: Nur als altes Konzept (Zeile ~33308, noch `de.dzbank.recon.*`)
3. **TestCaseRunnerProcessor**: Nur als altes Konzept (Zeile ~33725, noch `de.dzbank.recon.*`)
4. **REST-API / Controller**: Kein REST-Endpoint vorhanden
5. **Frontend/TypeScript**: Nicht enthalten

### Teilweise vorhanden / In Arbeit:
1. **DefaultProcessorDescriptor**: Nur als Markdown-Beschreibung (Zeile 1038), keine Java-Klasse unter `de.starima.pfw.*`
2. **IParameterDescriptor / IValueDescriptor**: Interfaces werden referenziert, aber konkrete Implementierungen fehlen teilweise
3. **Descriptor-Builder-Konzepte** (Zeile ~20500+): Neue Entwürfe für `DescriptorBuildProcessor`, `ShapeResolveProcessor`, `TypeShape`-Hierarchie — offenbar Work-in-Progress

### Konfiguration vorhanden:
- `application.properties` (Test + Main)
- Beispiel-`beanParameterMap` als JSON (Zeile 60-64, 40762-40773)
- IntelliJ Module-Config (Zeile 65-76)

---

## 5. Bewertung: Kann man damit ein Projekt aufsetzen?

### Ja, mit Einschränkungen.

**Was funktionieren sollte (Kern-Laufzeit):**
- Spring Boot starten mit `StarimaBaseApplication`
- `TestInstanceProcessorRuntime` als Einstiegspunkt
- `DefaultProcessorProvider` für Prozessor-Erzeugung
- `FileSystemParameterProviderProcessor` für beanParameterMap aus Dateien
- `AbstractProcessor` mit kompletter 4-Phasen-Initialisierung
- Alle Annotationen (`@Processor`, `@ProcessorParameter`, `@ValueObject`)
- `ProcessorUtils` mit der gesamten Reflection-Logik
- ValueFunction-Chain für Typ-Transformation
- Kontext-Hierarchie und Parameter-Merging

**Was noch aufgebaut werden muss:**
1. **Maven/Gradle Projektstruktur**: Die Datei muss in einzelne `.java`-Dateien aufgeteilt werden
2. **Dependencies**: `pom.xml` / `build.gradle` mit Spring Boot, Lombok, Jackson, Commons-Lang etc.
3. **Fehlende Implementierungen**: Einige Interfaces werden referenziert, deren konkrete Klassen fehlen (z.B. `DefaultReconContext`, konkrete `IProcessorDescriptor`-Implementierung)
4. **Test-Infrastruktur**: JUnit-Tests, TestCaseRunner
5. **LogBook/RuntimeShell**: Muss von Konzept in `de.starima.pfw.*` portiert werden

---

## 6. Empfohlene nächste Schritte

1. **Datei aufteilen**: Automatisiert in einzelne Java-Dateien nach Package-Struktur
2. **Maven-Projekt erstellen**: Multi-Module mit `pfw-base`, `pfw-test`, `pfw-sample`
3. **Compile-Check**: Fehlende Imports und Klassen identifizieren
4. **Minimaler Integrationstest**: `TestInstanceProcessorRuntime` mit einer simplen beanParameterMap starten
5. **Schrittweises Auffüllen**: Fehlende Implementierungen ergänzen
