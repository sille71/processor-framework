# Richtlinie: Prozessor-Dokumentation

## Zwei Ebenen der Dokumentation

### Ebene 1: @Processor-Annotation (Kurzform, maschinenlesbar)

```java
@Processor(
    description = "Transformiert CSV-Dateien in eine tabellarische Datenstruktur.",
    categories = {"import", "reader"},
    tags = {"csv", "file", "tabular", "encoding"}
)
```

**Pflichtfelder:**
- `description`: Ein Satz, der erklärt was der Prozessor tut.
- `categories`: Hierarchische Einordnung (z.B. "import/reader").
- `tags`: Suchbegriffe für den KI-Katalog und das UI.

### Ebene 2: Markdown-Langbeschreibung (docs/processors/<prototypeIdentifier>.md)

```markdown
# CsvReaderProcessor

## Überblick
Liest CSV-Dateien zeilenweise ein und wandelt sie in eine
tabellarische Datenstruktur um.

## Parameter
- **path** (String, required): Pfad zur CSV-Datei
- **encoding** (String, default: "UTF-8"): Zeichenkodierung
- **delimiter** (String, default: ","): Spaltentrennzeichen

## Zusammenspiel
Wird typischerweise als sourceProcessor in einem ReconProcessor eingesetzt.
Erwartet einen ContextProvider, der den Dateipfad bereitstellt.

## Beispiel-Konfiguration
...
```

Der `MarkdownDocRendererProcessor` liest diese Datei über:
`resources/docs/processors/<prototypeIdentifier>.md`


## @ProcessorParameter-Richtlinie

```
IMMER @ProcessorParameter:
  - Konfigurierbare Felder
  - Zustandsbeschreibende Felder
  - Sub-Prozessor-Referenzen
  - Collections/Maps von konfigurierbaren Werten

AUSNAHME (kein @ProcessorParameter):
  - Interne Caches
  - Transiente Laufzeitdaten
  - Spring-injizierte Infrastruktur
  - Berechnete Werte

Pflicht-Attribute:
  description   → bei jedem nicht-trivialen Feld
  value         → wenn sinnvoller Default existiert
  required      → wenn ohne den Wert nichts funktioniert
```


## Biologie-Annotation (optional, empfohlen)

Ein Einzeiler als Blockquote, der die Rolle des Prozessors
in der biologischen Analogie beschreibt:

```markdown
> *Biologisch: Das morphogenetische Feld — stellt die Bedingungen her,
> unter denen sich der Prozessor selbst entfalten kann.*
```
