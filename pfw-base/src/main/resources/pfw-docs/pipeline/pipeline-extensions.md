# Pipeline — Zukünftige Erweiterungen

Alle folgenden Erweiterungen sind Plugins/Zusätze.
Kein Architekturumbau. Das bestehende Design trägt sie.

---

## Parameter-Relationen und reaktive Wertebereiche

### Erweiterung 1: Initialisierungsreihenfolge
```java
@ProcessorParameter(order = 10)
private String country;

@ProcessorParameter(order = 20, dependsOn = "country")
private String city;
```

Eine Zeile Sortierung im ProcessorInstanceProvider.

### Erweiterung 2: dependsOn als Descriptor-Kante

Im Descriptor-Graph: gerichtete Kante `country → city`.
UI kann daraus Aktualisierungen ableiten.

### Erweiterung 3: Reaktive Domains

ISetProcessor IST ein Prozessor mit eigenen Parametern.
Der Domain von "city" hat einen `countryFilter`-Parameter:

```java
@Processor(description = "Städte gefiltert nach Land")
public class CountryFilteredCityDomain extends AbstractProcessor 
        implements ISetProcessor<String> {
    @ProcessorParameter
    private String countryFilter;
    
    @Override
    public List<String> getMembers() {
        return cityDatabase.getCitiesForCountry(countryFilter);
    }
}
```

Kaskade bei Änderung: Patch → Parameter setzen → dependsOn prüfen →
Domain-Refresh → Validierung → UI-Aktualisierung.

---

## AI Integration Layer (Phase 5)

### Drei Säulen

**IProcessorCatalog:** Durchsuchbarer Index über bestehende Descriptoren.
Keine eigene Datenstruktur — Suchfassade über das Descriptor-System.

**IAIBlueprintGenerator:** Strukturierte Intention + Katalog → beanParameterMap.
Mit Erklärungen, Annahmen, offenen Fragen, Confidence-Score.

**IAIConversationProcessor:** Natürlichsprachlicher Dialog.
Selbst ein Prozessor, über beanParameterMap konfigurierbar.
System-Prompt dynamisch aus dem Prozessor-Katalog generiert.

### Warum das Framework einzigartig ist

Selbstbeschreibende Prozessoren → KI KANN den Katalog lesen.
beanParameterMap als JSON → KI KANN valide Konfigurationen erzeugen.
Edit-Session mit Patches → KI KANN inkrementell arbeiten.
Roundtrip → KI KANN bestehende Systeme analysieren UND neue erzeugen.
Descriptor als API → KI braucht keinen Quellcode.

---

## Werte-Constraints (IValueConstraints)

```java
public interface IValueConstraints {
    Number getMin();
    Number getMax();
    Integer getMinLength();
    Integer getMaxLength();
    String getPattern();
    Integer getMinItems();
    Integer getMaxItems();
    Number getStep();
    boolean isReadOnly();
    boolean isNullable();
}
```

Auf IValueDescriptor oder IParameterDescriptor. Ergänzt ISetProcessor
für einfache Standardfälle. ISetProcessor bleibt für komplexe Wertebereiche.

---

## TypeScript-Port

ADR-basiert, gleiche Architektur, andere Sprache:
- Decorators statt Annotations (mächtiger, Config direkt im Decorator)
- Union Types statt Generics-Erasure
- Registry-Pattern statt Spring
- JSON als native beanParameterMap
- Geschätzter Aufwand: ~3-4 Claude Code Sessions, ~6.000-7.000 Zeilen

---

## Sandbox und LogBook

ProcessorSandbox: Isolierte Testumgebung für Prozessoren.
Prozessoren testen sich selbst — deklarativ, über beanParameterMap.

LogBookProcessor: Strukturiertes Logging als Prozessorkette.
Filter → Formatter → Sink — alles Prozessoren, alles konfigurierbar.
