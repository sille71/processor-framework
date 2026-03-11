# Das Prozessorframework als morphogenetisches System

> *Ein Descriptor ist wie ein Gen: Er weiß nicht, wo er ist —
> aber er weiß, was er tun kann, wenn er aktiviert wird.
> Der InstanceProvider ist das morphogenetische Feld, nicht der Architekt.*

## Die biologische Analogie

Das Prozessorframework folgt nicht dem industriellen Modell einer Montagehalle,
in der ein zentraler Bauplan Schritt für Schritt abgearbeitet wird. Es folgt dem
biologischen Modell der **Embryonalentwicklung**: Lokale Autonomie, rekursive
Selbstentfaltung, Differenzierung durch Kontext.

In der Biologie gibt es keinen globalen Bauplan-Ausführer. Jede Zelle enthält
die komplette DNA, aber sie liest nur den Teil, der für ihren Ort und Zustand
relevant ist. Die globale Ordnung entsteht **emergent** aus lokalen Entscheidungen.

Das ist exakt das Prinzip des Prozessorframeworks.


## Die Zuordnung: Biologie → Framework

### DNA und Gene

```
Biologie:  DNA → enthält alle Gene → beschreibt den gesamten Organismus
Framework: beanParameterMap → enthält alle Parameter → beschreibt den gesamten Objektgraph
```

Die beanParameterMap ist die DNA des Systems. Sie enthält die vollständige
Beschreibung aller Prozessoren, ValueObjects und ihrer Parameter — als flache,
serialisierbare Struktur (JSON). Genau wie DNA eine lineare Sequenz von
Nukleotiden ist, die erst im Kontext dreidimensionale Strukturen bildet.

Ein einzelner Eintrag in der beanParameterMap — z.B.
`"reconProcessor": {"contextProviderProcessor": "clusterCtxProvider", "batchSize": "1000"}`
— ist ein **Gen**: eine lokale Beschreibung, die erst im richtigen Kontext
zum Leben erwacht.

### Genexpression: CONSTRUCT (Hydration)

```
Biologie:  DNA wird abgelesen → Protein entsteht → Zelle differenziert sich
Framework: beanParameterMap wird interpretiert → Objekt entsteht → Prozessor initialisiert sich
```

Die **Hydration** (`InstanceProviderChain.provide()`) ist die Genexpression:
Aus der flachen Beschreibung (DNA/beanParameterMap) entsteht ein lebendiges,
dreidimensionales Objekt (Protein/Prozessor) mit Verhalten und Zustand.

### Reverse Genexpression: DESCRIBE (Dehydration)

```
Biologie:  Zelle wird analysiert → DNA-Sequenz wird abgelesen → Genom wird kartiert
Framework: Prozessor wird inspiziert → Parameter werden extrahiert → beanParameterMap entsteht
```

Die **Dehydration** (`InstanceProviderChain.extract()`) ist die Genomsequenzierung:
Aus einem lebendigen Objekt wird seine vollständige Beschreibung rekonstruiert.

Die Roundtrip-Invarianz `extract(provide(dna)) == dna` entspricht dem
biologischen Prinzip, dass die DNA nach der Expression unverändert vorliegt.


### Epigenetik: Context und Konfiguration

```
Biologie:  Gleiche DNA, aber verschiedene Umgebungssignale → verschiedene Zelltypen
Framework: Gleiche Prozessorklasse, aber verschiedene beanParameterMaps → verschiedene Instanzen
```

Die **beanParameterMap ist die Epigenetik** — nicht die DNA selbst. Die DNA
ist die Java-Klasse (`class ReconProcessor`). Die beanParameterMap enthält
die Signale, die bestimmen, welche "Gene" (Parameter) wie "exprimiert"
(konfiguriert) werden. Gleiche Klasse, verschiedene Konfiguration →
verschiedene Instanzen.

Der **IProcessorContext** ist das Gewebe, in dem die Zelle lebt.
Die Context-Hierarchie (Global → Cluster → Recon → Instance) entspricht
der biologischen Hierarchie (Organismus → Organ → Gewebe → Zelle).
Ein Prozessor "erbt" Parameter aus seinem Elternkontext — genau wie
eine Zelle Signale aus dem umgebenden Gewebe empfängt.


### Morphogenetisches Feld: InstanceProviderChain

```
Biologie:  Das morphogenetische Feld bestimmt nicht WAS entsteht,
           sondern stellt die Bedingungen her, unter denen sich
           die Zelle SELBST entfalten kann.
Framework: Die InstanceProviderChain bestimmt nicht WAS gebaut wird,
           sondern orchestriert die Bedingungen (Context, Scope,
           Parameter-Auflösung), unter denen der Prozessor entsteht.
```

Die InstanceProviderChain ist NICHT der Architekt. Sie ist das Feld.
Sie sorgt dafür, dass:
- Der richtige Kontext bereitsteht (ContextProviderResolver)
- Zirkularitäten erkannt werden (CreationStack)
- Jeder Parameter rekursiv aufgelöst wird (Rekursion über die Chain)
- Skalare Werte transformiert werden (ValueFunction)

Aber sie entscheidet NICHT, welcher Prozessor-Typ entsteht — das bestimmt
die beanParameterMap (die DNA/Epigenetik).


### Differenzierung: Polymorphie und Resolver

```
Biologie:  Eine Stammzelle kann sich in verschiedene Zelltypen differenzieren.
           Welcher Typ entsteht, hängt von lokalen Signalen ab.
Framework: Ein Interface (IFormatProcessor) kann durch verschiedene
           Implementierungen realisiert werden. Welche gewählt wird,
           hängt von der beanParameterMap ab.
```

Die **getPossibleValueDescriptors()** im Descriptor-System ist die
Stammzell-Logik: "Dieses Feld kann Herz oder Lunge werden."
Die konkrete Auswahl erfolgt durch das epigenetische Signal
(den Wert `"formatProcessor": "defaultFormatProcessor"` in der beanParameterMap).


### Organe und Gewebe: Composite-Strukturen

```
Biologie:  Ein Organ besteht aus verschiedenen Zelltypen in geordneter Struktur.
Framework: Ein Prozessor besteht aus verschiedenen Sub-Prozessoren und
           ValueObjects in geordneter Parameterstruktur.
```

Eine `List<IProcessor>` ist ein **Gewebe**: gleichartige Zellen
in einer geordneten Struktur. Der `CollectionInstanceProvider`
ist die Gewebe-Morphogenese — er sorgt dafür, dass jedes Element
im richtigen Kontext entsteht.

Eine `Map<String, IProcessor>` ist ein **Organ mit spezialisierten Bereichen**:
Jeder Bereich (Key) hat seinen eigenen Spezialisten (Value).


## Die Embryonalentwicklung eines Prozessors

Am Beispiel eines `ReconProcessor` mit den Parametern
`contextProviderProcessor`, `sourceProcessor`, `targetProcessor`, `batchSize`:

### 1. Zygote (Startpunkt)

```
DNA: beanParameterMap = {
  "reconProcessor": {
    "contextProviderProcessor": "clusterCtxProvider",
    "sourceProcessor": "csvReaderProcessor:reader1@prototype",
    "targetProcessor": "dbWriterProcessor:writer1@singleton",
    "batchSize": "1000"
  },
  "clusterCtxProvider": { ... },
  "csvReaderProcessor": { ... }
}

Signal: "Erzeuge reconProcessor" → InstanceProviderChain.provide()
```

### 2. Erste Zellteilung (Bean-Lookup)

```
Die Zygote teilt sich: Spring liefert die rohe Bean-Instanz.
processor = beanProvider.getBeanForId("reconProcessor")
processor.setIdentifier("reconProcessor")
processor.setScope(prototype)

Biologisch: Die Zelle hat jetzt eine Identität.
```

### 3. Gastrulation (Context-Erstellung)

```
Der ContextProviderResolver findet den zuständigen ContextProvider:
@ProcessorParameter(contextProvider = true) → "clusterCtxProvider"

Der ClusterCtxProvider wird rekursiv erzeugt (!) und erstellt
einen neuen IProcessorContext als Kind des Parent-Kontextes.

Biologisch: Das morphogenetische Feld (Gewebe/Kontext) bildet sich.
Die Zelle weiß jetzt, in welchem Gewebe sie lebt.
```

### 4. Organinduktion (Parameter-Rekursion)

```
Für jeden @ProcessorParameter:

  batchSize = "1000"
    → ScalarInstanceProvider → IntegerValueFunction.transformValue("1000") → 1000
    Biologisch: Ein einfaches Protein wird synthetisiert.

  sourceProcessor = "csvReaderProcessor:reader1@prototype"
    → ProcessorInstanceProvider.provide() → REKURSION!
    → Bean-Lookup → Context → Parameter → ...
    Biologisch: Ein neues Organ beginnt sich zu entwickeln.
    Jedes Organ hat seine eigene Entwicklungsgeschichte.

  targetProcessor = "dbWriterProcessor:writer1@singleton"
    → ProcessorInstanceProvider → Scope-Check → bereits vorhanden!
    Biologisch: Ein gemeinsam genutztes Organ (wie das Herz)
    wird nicht doppelt gebaut, sondern geteilt.
```

### 5. Geburt (processorOnInit)

```
Alle Parameter sind gesetzt. Der Prozessor wird im Scope registriert.
processorOnInit() wird aufgerufen — der Prozessor ist lauffähig.

Biologisch: Das Organ ist funktionsfähig und beginnt zu arbeiten.
```


## Die drei Schichten — biologisch verstanden

```
┌─────────────────────────────────────────────────────────────────┐
│  INCUBATOR (Der Brutkasten)                                     │
│                                                                 │
│  Stellt die Umgebung bereit: Wärme, Nährstoffe, Monitoring.    │
│  Startet und überwacht Entwicklungsprozesse.                    │
│  Verwaltet den Workspace (partieller Organismus für das UI).    │
│                                                                 │
│  = IIncubator + IEditSession + DescriptorWorkspace              │
├─────────────────────────────────────────────────────────────────┤
│  MORPHOGENETISCHES FELD (Die Entwicklungssteuerung)             │
│                                                                 │
│  Orchestriert die Bedingungen: Context, Scope, Rekursion.       │
│  Steuert nicht WAS entsteht, sondern WIE es entsteht.           │
│  Erkennt und verhindert Missbildungen (Zirkularität).           │
│                                                                 │
│  = InstanceProviderChain + ContextProviderResolver              │
├─────────────────────────────────────────────────────────────────┤
│  GENE UND PROTEINE (Die molekulare Ebene)                       │
│                                                                 │
│  Gene: Descriptoren (Beschreibung der Struktur und Typen)       │
│  Proteine: ValueFunctions (atomare Transformationen)            │
│  Ribosomen: ScalarInstanceProvider (übersetzt Gene in Proteine) │
│                                                                 │
│  = IValueDescriptor + IValueFunction + ITypeDescriptor          │
└─────────────────────────────────────────────────────────────────┘
```


## Leitsätze für das Design

1. **Keine zentrale Steuerung.** Kein Prozessor weiß, wo er im Gesamtsystem steht.
   Er weiß nur, was er kann und welche Parameter er braucht.

2. **Lokale Autonomie.** Jeder Descriptor ist für sich korrekt.
   Globale Ordnung entsteht emergent aus lokalen Entscheidungen.

3. **Kontext statt Befehle.** Kein "baue X hier". Sondern: "hier ist dein Kontext,
   entfalte dich." Der ContextProviderResolver liefert das morphogenetische Signal.

4. **Zwei Entwicklungsphasen.** Genom/Schema (DESCRIBE) und Expression/Instance (CONSTRUCT).
   Beides nutzt dieselbe Maschinerie (InstanceProviderChain), nur in verschiedenen Modi.

5. **Differenzierung statt if/else.** Polymorphie ist kein Sonderfall, sondern der
   Normalfall. `getPossibleValueDescriptors()` ist die Stammzell-Logik.

6. **Roundtrip als Korrektheitsbeweis.**
   `extract(provide(dna)) == dna` — das Genom bleibt nach der Expression erhalten.
   Das ist der algebraische Beweis, dass das System korrekt arbeitet.
