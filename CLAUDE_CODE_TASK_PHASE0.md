# Claude Code Task: Phase 0 — Legacy-Bereinigung + Modul-Aufteilung

## Kontext

Das gesamte Framework liegt aktuell in einem einzigen Modul `pfw-base-alt`
(~277 Java-Klassen). Es muss auf die neue Multi-Module-Struktur aufgeteilt werden:

```
pfw-base-alt (277 Klassen) ──► wird aufgeteilt in:
    ├── pfw-base        (39 Klassen)   Kern-API, Interfaces, Annotationen
    ├── pfw-descriptor  (113 Klassen)  Beschreibungs-System, ValueFunctions, Config
    ├── pfw-runtime     (108 Klassen)  AbstractProcessor, Spring, Kernel
    └── pfw-doc         (3 Klassen)    Dokumentations-Renderer
```

Die Multi-Module-Projektstruktur (Parent POM, Modul-POMs, Packages)
existiert bereits — sie wurde mit `setup-project-structure.sh` erstellt.

**Einige Legacy-Bereinigungen sind BEREITS durchgeführt** (z.B. IReconProcessor → IProcessor).
Alle Schritte enthalten daher eine **Prüfung vorweg**, ob die Bereinigung noch nötig ist.


## WICHTIG: Arbeitsweise

1. **Prüfe VOR jeder Bereinigung**, ob sie noch nötig ist (`grep` zählen)
2. **Kompiliere nach JEDEM Schritt**: `mvn compile -pl <modul>`
3. **Committe nach jeder abgeschlossenen Unterphase**
4. Die Reihenfolge ist ZWINGEND: pfw-base → pfw-descriptor → pfw-runtime → pfw-doc
5. Innerhalb jedes Moduls: Erst Interfaces/API, dann Domain, dann Implementierungen

---

## Phase 0.1: Legacy-Bereinigung (IN pfw-base-alt, VOR dem Verschieben)

### Schritt 0.1.1: Bestandsaufnahme

Führe zuerst eine Bestandsaufnahme durch, um zu sehen was noch zu tun ist:

```bash
cd pfw-base-alt

echo "=== Prüfe Legacy-Reste ==="
echo "javax-Imports:            $(grep -rl 'javax\.xml\.bind\|javax\.annotation\|javax\.servlet' src/ 2>/dev/null | wc -l) Dateien"
echo "de.dzbank-Imports:        $(grep -rl 'de\.dzbank' src/ 2>/dev/null | wc -l) Dateien"
echo "IReconProcessor:          $(grep -rl 'IReconProcessor' src/ 2>/dev/null | wc -l) Dateien"
echo "LogOutputHelper:          $(grep -rl 'LogOutputHelper' src/ 2>/dev/null | wc -l) Dateien"
echo "Compression:              $(grep -rl 'de\.dzbank.*Compression' src/ 2>/dev/null | wc -l) Dateien"
echo "BeanUtilities:            $(grep -rl 'BeanUtilities' src/ 2>/dev/null | wc -l) Dateien"
echo "ReconManagerHelper:       $(grep -rl 'ReconManagerHelper' src/ 2>/dev/null | wc -l) Dateien"
echo "StringComparator:         $(grep -rl 'de\.dzbank.*StringComparator' src/ 2>/dev/null | wc -l) Dateien"
echo "IReconProcessorLifecycle: $(grep -rl 'IReconProcessorLifecycleListener' src/ 2>/dev/null | wc -l) Dateien"
echo "ILogBookProcessor (dzbank): $(grep -rl 'de\.dzbank.*ILogBookProcessor' src/ 2>/dev/null | wc -l) Dateien"
```

**Überspringe jeden folgenden Schritt, bei dem die Prüfung 0 Dateien ergibt.**


### Schritt 0.1.2: javax → jakarta

**Prüfung:** `grep -rl 'javax\.xml\.bind\|javax\.annotation\|javax\.servlet' src/`

Falls Treffer vorhanden:
```
Suchen:    javax.xml.bind       → Ersetzen: jakarta.xml.bind
Suchen:    javax.annotation     → Ersetzen: jakarta.annotation
Suchen:    javax.servlet        → Ersetzen: jakarta.servlet
```

```bash
find src -name '*.java' -exec sed -i \
  -e 's/javax\.xml\.bind/jakarta.xml.bind/g' \
  -e 's/javax\.annotation/jakarta.annotation/g' \
  -e 's/javax\.servlet/jakarta.servlet/g' \
  {} +
```

Kompilieren: `mvn compile`


### Schritt 0.1.3: de.dzbank Package-Imports

**Prüfung:** `grep -rl 'de\.dzbank' src/`

Falls Treffer vorhanden, in dieser Reihenfolge:

```bash
find src -name '*.java' -exec sed -i \
  -e 's/de\.dzbank\.recon\.ms\.base\./de.starima.pfw.base./g' \
  -e 's/de\.dzbank\.recon\.components\.base\.processors\.api\.IReconProcessor/de.starima.pfw.base.processor.api.IProcessor/g' \
  -e 's/de\.dzbank\.recon\.components\.base\.processors\.api\.IReconProcessorLifecycleListener/REMOVE_THIS_IMPORT/g' \
  -e 's/de\.dzbank\.recon\.components\.base\.utils\.ReconManagerHelper/REMOVE_THIS_IMPORT/g' \
  -e 's/de\.dzbank\.components\.utils\.log\.LogOutputHelper/REMOVE_THIS_IMPORT/g' \
  -e 's/de\.dzbank\.components\.utils\.io\.Compression/REMOVE_THIS_IMPORT/g' \
  -e 's/de\.dzbank\.components\.utils\.BeanUtilities/REMOVE_THIS_IMPORT/g' \
  -e 's/de\.dzbank\.components\.utils\.StringComparator/REMOVE_THIS_IMPORT/g' \
  {} +

# Entferne die markierten Imports
find src -name '*.java' -exec sed -i '/REMOVE_THIS_IMPORT/d' {} +
```

Danach: Die entfernten Klassen ersetzen (siehe Schritte 0.1.4–0.1.7).

Kompilieren: `mvn compile` — wird zunächst FEHLSCHLAGEN (entfernte Klassen).


### Schritt 0.1.4: LogOutputHelper Ersatz erstellen

**Prüfung:** `grep -rl 'LogOutputHelper' src/`

Falls noch referenziert, erstelle einen minimalen Ersatz:

```java
package de.starima.pfw.base.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogOutputHelper {
    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static String getModelAsStringBuffer(Object model) {
        if (model == null) return "null";
        try {
            return mapper.writeValueAsString(model);
        } catch (Exception e) {
            return model.toString();
        }
    }
}
```


### Schritt 0.1.5: IReconProcessor → IProcessor

**Prüfung:** `grep -rl 'IReconProcessor' src/`

Falls noch vorhanden:
```bash
find src -name '*.java' -exec sed -i \
  -e 's/IReconProcessor/IProcessor/g' \
  {} +

# Doppelte Imports entfernen (IProcessor könnte jetzt 2x importiert sein)
# Manuell prüfen oder mit IDE-Optimize-Imports
```


### Schritt 0.1.6: ReconManagerHelper eliminieren

**Prüfung:** `grep -rl 'ReconManagerHelper' src/`

Die Methoden aus ReconManagerHelper, die noch gebraucht werden,
in `ProcessorUtils` oder eine neue Hilfsklasse übernehmen.
Typische Methoden: `getReconContext()`, `getProcessorContext()`.

Prüfe jede Aufrufstelle und ersetze durch den passenden ProcessorUtils-Aufruf.


### Schritt 0.1.7: Compression, BeanUtilities, StringComparator

**Prüfung:** Jeweils `grep -rl '<Klassenname>' src/`

```
Compression       → Import entfernen (nicht genutzt, nur importiert)
BeanUtilities     → Durch Spring BeanUtils oder Reflection-Aufrufe ersetzen
StringComparator  → Durch Comparator.naturalOrder() oder String::compareTo ersetzen
```


### Schritt 0.1.8: Finale Kompilierung

```bash
cd pfw-base-alt
mvn compile
```

**MUSS grün sein, bevor Phase 0.2 beginnt.**

```bash
# Finale Prüfung: Keine Legacy-Reste mehr
grep -rl 'de\.dzbank' src/ && echo "FEHLER: de.dzbank noch vorhanden!" || echo "OK"
grep -rl 'javax\.xml\.bind\|javax\.annotation\|javax\.servlet' src/ && echo "FEHLER: javax noch vorhanden!" || echo "OK"
```

**Commit:** `git commit -am "Phase 0.1: Legacy-Bereinigung abgeschlossen"`

---

## Phase 0.2: ISubjectFunction nach pfw-base verschieben

### Problem
`IValueFunction` (landet in pfw-descriptor) extends `ISubjectFunction` (liegt im Transformator-Package).
Wenn `ISubjectFunction` in pfw-runtime bleibt, hätte pfw-descriptor eine Dependency auf pfw-runtime → Zirkel!

### Lösung
Die reinen Transformator-API-Interfaces nach pfw-base verschieben:

```
VON: de.starima.pfw.base.processor.transformator.api (pfw-runtime)
NACH: de.starima.pfw.base.processor.transformator.api (pfw-base)
```

Gleicher Package-Name, anderes Modul. Keine Import-Änderungen nötig!

### Zu verschieben (7 Interfaces):
- [ ] `ITransformerProcessor` (13 Zeilen)
- [ ] `ISubjectTransformerProcessor` (20 Zeilen)
- [ ] `ISubjectFunction` (25 Zeilen)
- [ ] `IFunction` (25 Zeilen)
- [ ] `IPropertyFunction` (23 Zeilen)
- [ ] `IValueTransformerProcessor` (7 Zeilen)
- [ ] `IBidirectionalPropertyTransformerProcessor` (12 Zeilen)

```bash
# In pfw-base das Package erstellen
mkdir -p pfw-base/src/main/java/de/starima/pfw/base/processor/transformator/api

# Dateien kopieren (nicht moven — pfw-base-alt bleibt vorerst intakt)
cp pfw-base-alt/src/main/java/de/starima/pfw/base/processor/transformator/api/ITransformerProcessor.java \
   pfw-base/src/main/java/de/starima/pfw/base/processor/transformator/api/
# ... analog für die anderen 6 Interfaces
```

Kompilieren: `mvn compile -pl pfw-base`

**Commit:** `git commit -am "Phase 0.2: Transformator-API-Interfaces nach pfw-base"`

---

## Phase 0.3: pfw-base befüllen (39 Klassen)

### Strategie
Dateien werden aus `pfw-base-alt` nach `pfw-base` KOPIERT (nicht verschoben).
Das alte Modul bleibt funktionsfähig. Erst wenn alle Module befüllt sind,
wird `pfw-base-alt` deaktiviert.

Alternativ: In IntelliJ `Refactor → Move Class` nutzen — das passt Imports
automatisch an. Bei Nutzung von Claude Code: `cp` + `mvn compile`.

### Schritt 0.3.1: Annotationen (3 Klassen)

```
- [ ] annotation/Processor.java
- [ ] annotation/ProcessorParameter.java
- [ ] annotation/ValueObject.java
```

```bash
SRC=pfw-base-alt/src/main/java/de/starima/pfw/base
DST=pfw-base/src/main/java/de/starima/pfw/base

cp $SRC/annotation/Processor.java $DST/annotation/
cp $SRC/annotation/ProcessorParameter.java $DST/annotation/
cp $SRC/annotation/ValueObject.java $DST/annotation/

mvn compile -pl pfw-base
```

### Schritt 0.3.2: Domain-Klassen (3 Klassen)

```
- [ ] domain/ProcessorScope.java
- [ ] domain/api/IArtifact.java
- [ ] exception/InvalidVersionNumberException.java
```

### Schritt 0.3.3: Kern-Interfaces (3 Klassen)

```
- [ ] processor/api/IProcessor.java
- [ ] processor/api/IProcessorProvider.java
- [ ] processor/api/IBeanProvider.java
```

**ACHTUNG:** `IProcessor` referenziert `IProcessorDescriptor`, der erst in pfw-descriptor liegt.
**Lösung:** Temporär die Referenz auskommentieren oder als Vorwärtsdeklaration belassen.
Wenn pfw-descriptor von pfw-base abhängt, wird IProcessorDescriptor dort sichtbar.

Prüfe: Hat IProcessor eine direkte Methode `getProcessorDescriptor()` die IProcessorDescriptor zurückgibt?
Falls ja: Dieses Interface muss auch nach pfw-base (als reines Interface ohne Implementierung).

```bash
# Prüfe die Dependency
grep "IProcessorDescriptor" pfw-base-alt/src/main/java/de/starima/pfw/base/processor/api/IProcessor.java
```

Falls `IProcessorDescriptor` in IProcessor referenziert wird:
→ `IProcessorDescriptor.java` (nur das Interface!) ebenfalls nach pfw-base kopieren.
→ Alternativ: Temporär `Object getProcessorDescriptor()` als Return-Typ.

### Schritt 0.3.4: Context-Interfaces (9 Klassen)

```
- [ ] processor/context/api/LoadStrategy.java
- [ ] processor/context/api/ITaskContext.java
- [ ] processor/context/api/IProcessorContext.java
- [ ] processor/context/api/ITransformationContext.java
- [ ] processor/context/api/IDescriptorConstructorContext.java
- [ ] processor/context/api/IRuntimeContextProviderProcessor.java
- [ ] processor/context/api/IContextCreationContext.java
- [ ] processor/context/api/IValueFunctionConstructorContext.java
- [ ] processor/context/api/IProposalContext.java
```

**ACHTUNG bei IProcessorContext:**
Referenziert `IParameterProviderProcessor` — dieses Interface muss auch nach pfw-base.

**ACHTUNG bei ITaskContext:**
Referenziert `MapUtils` — muss ebenfalls nach pfw-base.

### Schritt 0.3.5: Util-Basis (bis zu 14 Klassen)

```
- [ ] util/MapUtils.java
- [ ] util/MapDetailLevel.java
- [ ] util/IdentifierUtils.java
- [ ] util/StreamUtils.java
- [ ] util/ProcessorUtils.java          ← GROSS: 2042 Zeilen, erstmal komplett
- [ ] util/CalculationHelper.java
- [ ] util/ConfigurationModel.java
- [ ] util/SerializationTarget.java
- [ ] util/api/IUUIDIdentifier.java
- [ ] util/UUIDIdentifier.java
- [ ] util/ArtifactIdentifier.java
- [ ] util/ArtifactIdentifierComparator.java
- [ ] util/Version.java
- [ ] util/LogOutputHelper.java          ← Falls in 0.1.4 erstellt
```

**ProcessorUtils** hat Abhängigkeiten auf Descriptor-Klassen (ValueFunction etc.).
Diese Imports werden zunächst rot sein — das löst sich, wenn pfw-base von pfw-descriptor
als Dependency nichts braucht (ProcessorUtils gehört eigentlich nach pfw-runtime,
aber wird von überall referenziert).

**ENTSCHEIDUNG:** ProcessorUtils erstmal nach pfw-base. Langfristig aufteilen:
- `ProcessorUtils` (statische Kern-Methoden) → pfw-base
- `ProcessorInitUtils` (initBeanParameters etc.) → pfw-runtime
- `ProcessorValueFunctionUtils` (createValueFunctionForContext) → wird durch ValueFunctionResolverChain ersetzt

Für den Moment: Komplett nach pfw-base, fehlende Imports akzeptieren (kommen mit pfw-descriptor).

### Schritt 0.3.6: Weitere API-Interfaces (7 Klassen)

```
- [ ] processor/parameter/api/IParameterProviderProcessor.java
- [ ] processor/parameter/api/IParameterChangeListener.java
- [ ] processor/parameter/api/IBeanTypeMapProcessor.java
- [ ] processor/parameter/api/IParameterFormatterProcessor.java
- [ ] processor/parameter/api/IParameterValidatorProcessor.java
- [ ] processor/artifact/api/IArtifactProvider.java
- [ ] processor/parameter/PropertyType.java
```

### Schritt 0.3.7: Kompilierung und Verifikation

```bash
mvn compile -pl pfw-base
```

Falls Fehler wegen fehlender Descriptor-Klassen in ProcessorUtils:
→ Temporär auskommentieren oder pfw-base-alt als Dependency hinzufügen.
→ Saubere Lösung: ProcessorUtils aufteilen (kann später passieren).

**Pragmatische Lösung für den Prototyp:**
```xml
<!-- In pfw-base/pom.xml temporär: -->
<dependency>
    <groupId>de.starima.pfw</groupId>
    <artifactId>pfw-base-alt</artifactId>
    <scope>provided</scope>
</dependency>
```
Das erlaubt die Kompilierung, ohne eine echte Runtime-Dependency zu erzeugen.
Wird entfernt, sobald pfw-descriptor befüllt ist.

**Commit:** `git commit -am "Phase 0.3: pfw-base befüllt (39 Klassen)"`

---

## Phase 0.4: pfw-descriptor befüllen (113 Klassen)

### Schritt 0.4.1: Descriptor-API-Interfaces (12 Klassen)

```
- [ ] description/api/IValueDescriptor.java
- [ ] description/api/IParameterDescriptor.java
- [ ] description/api/IStructureValueDescriptor.java
- [ ] description/api/IProcessorDescriptor.java
- [ ] description/api/ICollectionValueDescriptor.java
- [ ] description/api/IMapValueDescriptor.java
- [ ] description/api/IReferenceValueDescriptor.java
- [ ] description/api/IParameterGroupDescriptor.java
- [ ] description/api/IValueFunction.java
- [ ] description/api/IDescriptorProcessor.java
- [ ] description/api/IDescriptorResolver.java
- [ ] description/api/IDomainProviderProcessor.java
```

### Schritt 0.4.2: Config-API-Interfaces (13 Klassen)

```
- [ ] description/config/api/ITypeRef.java
- [ ] description/config/api/ITypeRefProvider.java
- [ ] description/config/api/ITypeResolutionContext.java
- [ ] description/config/api/IDescriptorConfig.java
- [ ] description/config/api/IDescriptorConfigProvider.java
- [ ] description/config/api/IDescriptorConfigCreationContext.java
- [ ] description/config/api/IValueConfig.java
- [ ] description/config/api/IProcessorParameterConfig.java
- [ ] description/config/api/IValueObjectConfig.java
- [ ] description/config/api/IProcessorConfig.java
- [ ] description/config/api/ICollectionValueConfig.java
- [ ] description/config/api/IMapValueConfig.java
- [ ] description/config/api/IValueFunctionConfig.java
```

### Schritt 0.4.3: Config-Implementierungen (27 Klassen)

**TypeRef:**
DefaultTypeRef, DefaultTypeResolutionContext, TypeRefProviderChain,
StringTypeRefProvider, NumberTypeRefProvider, BooleanTypeRefProvider,
EnumTypeRefProvider, ProcessorTypeRefProvider, ValueObjectTypeRefProvider,
ArrayTypeRefProvider, ParameterizedTypeRefProvider, ClassTypeRefProvider

**Config:**
DefaultDescriptorConfig, ValueConfig, ProcessorParameterConfig,
ProcessorConfig, ValueObjectConfig, ValueFunctionConfig,
CollectionValueConfig, MapValueConfig,
DefaultDescriptorConfigCreationContext,
ScalarValueConfigProvider, CollectionValueConfigProvider,
ProcessorParameterDescriptorConfigProvider,
ProcessorDescriptorConfigProvider, ValueObjectDescriptorConfigProvider,
DescriptorConfigProviderChain

### Schritt 0.4.4: ValueFunctions (21 Klassen)

**Basisklassen:** AbstractValueFunction, AbstractCollectionValueFunction

**Skalare:** StringVF, IntegerVF, DoubleVF, BooleanVF, EnumVF, ClassVF, JacksonVF

**Collections:** DefaultListVF, DefaultArrayVF, StringArrayVF, JacksonGenericListVF

**Prozessor:** ProcessorVF, ProcessorListVF, ProcessorMapVF

**ValueObject:** ValueObjectVF, ValueObjectListVF, ValueObjectMapVF

**Map:** SimpleMapVF, DefaultMapVF

**Spezial:** ArtifactIdentifierVF

### Schritt 0.4.5: Descriptor-Implementierungen (13 Klassen)

DefaultValueDescriptor, AnyValueDescriptor,
DefaultStructureValueDescriptor, DefaultProcessorDescriptor,
DefaultParameterDescriptor, DefaultParameterGroupDescriptor,
DefaultCollectionValueDescriptor, DefaultMapValueDescriptor,
DefaultReferenceValueDescriptor, DefaultDescriptorResolver,
DescriptorProcessor, BeanWrapperProcessor

### Schritt 0.4.6: Incubator-System (27 Klassen)

**API (10):** IIncubator, IIncubatorSession, IConstructSession, IDescribeSession,
IEditSession, IConstructionManager, IDescriptorResolver (incubator),
IInstanceCreationContext, IInstanceProvider, ITypeIntrospector

**Domain (12):** ISeed, IConstructSource, IDescribeSource, IEditSource,
IConstructPolicy, IDescribePolicy, IEditPolicy, IBuildTaskContext,
IConstructTaskContext, IDescribeTaskContext, IEditTaskContext, ConstructSource

**Impl (5):** FrameworkIncubator, DefaultConstructionManager,
DefaultInstanceCreationContext, DescriptorResolverChain, TypeIntrospectorChain

### Schritt 0.4.7: Set-Prozessoren (4 Klassen)

ISetProcessor, SimpleTypeSet, ProcessorPrototypeIdentifierSet,
ProcessorPrototypeIdentifierListSet

### Schritt 0.4.8: Kompilierung

```bash
mvn compile -pl pfw-base,pfw-descriptor
```

**Commit:** `git commit -am "Phase 0.4: pfw-descriptor befüllt (113 Klassen)"`

---

## Phase 0.5: pfw-runtime befüllen (108 Klassen)

### Schritt 0.5.1: Context-Implementierungen (8 Klassen)

DefaultProcessorContext, DefaultTaskContext, DefaultTransformationContext,
DefaultDescriptorConstructorContext, DefaultContextCreationContext,
DefaultValueFunctionConstructorContext, DefaultProposalContext,
DefaultRuntimeContextProviderProcessor

### Schritt 0.5.2: Kern-Implementierungen (4 Klassen)

AbstractProcessor, DefaultProcessorProvider, DefaultBeanProvider,
DefaultValueObjectProviderProcessor

### Schritt 0.5.3: Parameter-Subsystem (6 Klassen)

AbstractParameterProviderProcessor, DefaultParameterProviderProcessor,
FileSystemParameterProviderProcessor, ClassPathParameterProvider,
StringConfigurationParameterProvider, DefaultBeanTypeMapProcessor

### Schritt 0.5.4: Kernel-System (9 Klassen)

IRuntimeKernelProcessor, IKernelBeanProvider, IPhaseProcessor, RunLevel,
KernelProcessor, KernelBeanProvider, KernelRuntimeContextProviderProcessor,
KernelConstructionManager, FullIncubatorPhaseProcessor

### Schritt 0.5.5: Service + Transformator + Request + Rest (restliche ~81 Klassen)

In Untergruppen (Service-Runtime, Transformatoren, Request, Variable,
Condition, Attribute, Data, Assets, Locale, StarimaBaseApplication).

Reihenfolge: API-Interfaces zuerst, dann Implementierungen.

### Schritt 0.5.6: Kompilierung

```bash
mvn compile -pl pfw-base,pfw-descriptor,pfw-runtime
```

**Commit:** `git commit -am "Phase 0.5: pfw-runtime befüllt (108 Klassen)"`

---

## Phase 0.6: pfw-doc befüllen (3 Klassen)

```
- [ ] IDocRendererProcessor.java
- [ ] DocRendererProcessor.java
- [ ] MarkdownDocRendererProcessor.java
- [ ] DocUtils.java (aus pfw-base hierher verschieben falls dort)
```

```bash
mvn compile
```

**Commit:** `git commit -am "Phase 0.6: pfw-doc befüllt"`

---

## Phase 0.7: pfw-base-alt deaktivieren

### Schritt 0.7.1: Temporäre Dependencies entfernen

Falls in Phase 0.3 die temporäre `pfw-base-alt` Dependency in pfw-base
hinzugefügt wurde → jetzt entfernen.

### Schritt 0.7.2: pfw-base-alt aus Parent POM entfernen

```xml
<!-- In processor-framework/pom.xml: -->
<!-- ENTFERNEN: <module>pfw-base-alt</module> -->
```

### Schritt 0.7.3: Finale Kompilierung OHNE pfw-base-alt

```bash
mvn clean compile
```

**Alles muss grün sein.**

### Schritt 0.7.4: pfw-base-alt archivieren

```bash
# Optional: als Backup behalten
mv pfw-base-alt pfw-base-alt-ARCHIVED

# Oder direkt löschen wenn Git-History ausreicht
# rm -rf pfw-base-alt
```

**Commit:** `git commit -am "Phase 0.7: pfw-base-alt deaktiviert — Migration abgeschlossen"`

---

## Claude Code Hinweise

### Bulk-Kopieren mit Verifikation

```bash
# Template für das Kopieren einer Klasse:
copy_class() {
    local CLASS=$1
    local SRC_MODULE=$2
    local DST_MODULE=$3
    
    local SRC="$SRC_MODULE/src/main/java/$CLASS"
    local DST="$DST_MODULE/src/main/java/$CLASS"
    
    if [ ! -f "$SRC" ]; then
        echo "SKIP: $SRC existiert nicht"
        return 1
    fi
    
    mkdir -p "$(dirname $DST)"
    cp "$SRC" "$DST"
    echo "OK: $(basename $CLASS)"
}

# Beispiel:
copy_class "de/starima/pfw/base/annotation/Processor.java" pfw-base-alt pfw-base
copy_class "de/starima/pfw/base/annotation/ProcessorParameter.java" pfw-base-alt pfw-base
copy_class "de/starima/pfw/base/annotation/ValueObject.java" pfw-base-alt pfw-base
mvn compile -pl pfw-base
```

### Bei Kompilierfehlern

Häufigste Ursache: Fehlende Klasse im Zielmodul, die noch im Quellmodul liegt.

```bash
# Zeigt welche Imports nicht aufgelöst werden:
mvn compile -pl pfw-base 2>&1 | grep "cannot find symbol" | sort -u
```

Lösung: Die fehlende Klasse ebenfalls verschieben, oder (wenn sie in ein
anderes Modul gehört) als temporäre provided-Dependency auf pfw-base-alt setzen.

### IntelliJ-Alternative (schneller für große Mengen)

```
1. Öffne die Klasse in pfw-base-alt
2. Refactor → Move Class (F6)
3. Wähle das Ziel-Package im neuen Modul
4. IntelliJ passt alle Imports automatisch an
```

Das ist bei 277 Klassen schneller als `cp` + manuelle Import-Anpassung.
Claude Code kann aber die Kompilier-Verifikation und Commit-Steuerung übernehmen.

---

## Zusammenfassung Zeitaufwand

```
Phase 0.1: Legacy-Bereinigung          ~30 Min (mechanisch)
Phase 0.2: ISubjectFunction            ~5 Min
Phase 0.3: pfw-base (39 Klassen)       ~45 Min
Phase 0.4: pfw-descriptor (113 Klassen) ~90 Min
Phase 0.5: pfw-runtime (108 Klassen)   ~90 Min
Phase 0.6: pfw-doc (3 Klassen)         ~10 Min
Phase 0.7: Deaktivierung               ~15 Min
                                        ─────────
Gesamt:                                 ~4-5 Stunden
```

Danach ist die Modul-Struktur sauber und Phase 1 (ITransformationContext-Modernisierung +
ITypeDescriptor + ValueFunctionResolverChain) kann starten.
