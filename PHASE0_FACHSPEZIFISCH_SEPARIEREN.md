# Phase 0 — Ergänzung: Fachspezifische Klassen separieren

## Angepasste Modulverteilung

```
pfw-base-alt (277 Klassen)
    │
    ├──► pfw-base        (39 Klassen)     Kern-API, Interfaces, Annotationen
    ├──► pfw-descriptor  (113 Klassen)    Beschreibungs-System, ValueFunctions, Config
    ├──► pfw-runtime     (~78 Klassen)    AbstractProcessor, Spring, Kernel
    │                                      (war 108, minus ~30 fachspezifische)
    ├──► pfw-doc         (3 Klassen)      Dokumentations-Renderer
    └──► NICHT ins Framework (~30 Klassen) Rcn-spezifische Implementierungen
                                           → Verbleiben in pfw-base-alt
                                           → Später in die Abgleichs-Anwendung
```


## Die Dreiteilung: Interface → Referenz-Impl → Fachspezifisch

### Ebene 1: Generische Interfaces → pfw-runtime (processor/…/api/)

Bleiben im Framework. Sind generisch typisiert, keine Rcn-Abhängigkeit.

**Attribute-Interfaces (5 Klassen) → pfw-runtime:**
```
- [ ] IAttribute                               attribute/api/
- [ ] IAttributeProviderProcessor<A>           attribute/api/
- [ ] IAttributeProviderSelectorProcessor<A>   attribute/api/
- [ ] IAttributeModelMapperProcessor           attribute/api/
- [ ] IAttributeHelperProcessor<A,C>           attribute/api/
```

**Condition-Interfaces (4 Klassen) → pfw-runtime:**
```
- [ ] ICondition<C>                            condition/api/
- [ ] IConditionProcessor<Item>                condition/api/
- [ ] IConditionProviderProcessor<C>           condition/api/
- [ ] IConditionConfigProviderProcessor<C>     condition/api/
```

**Data-Interfaces (4 Klassen) → pfw-runtime:**
```
- [ ] IDataProviderProcessor<H,R>              data/api/
- [ ] ISearchFilterProcessor<S>                data/api/
- [ ] IPager                                   data/api/
- [ ] IDataPage<H,R>                           data/api/
```

**Summe Ebene 1: 13 Interfaces → pfw-runtime**


### Ebene 2: Referenz-Implementierungen → pfw-runtime

Generisch genug für das Framework. Zeigen wie die Interfaces
benutzt werden sollen. Keine Rcn-Typisierung.

**Condition-Referenz (3 Klassen) → pfw-runtime:**
```
- [ ] ConditionConfig                          condition/domain/
- [ ] Condition                                condition/domain/
- [ ] DefaultConditionProvider                 condition/
```

**Data-Referenz (1 Klasse) → pfw-runtime:**
```
- [ ] Pager                                    data/domain/
```

**Summe Ebene 2: 4 Referenz-Implementierungen → pfw-runtime**


### Ebene 3: Fachspezifisch (Rcn) → NICHT ins Framework

Typisiert auf `RcnAttribute`, `ConditionConfig` oder konkrete
Abgleichslogik. Verbleibt in `pfw-base-alt` und wird später
in die Abgleichs-Anwendung verschoben.

**Attribute-Implementierungen (11 Klassen) → RAUS:**
```
- [x] RcnAttribute                            → Rcn-Domain
- [x] RcnAttributeProcessor                   → Rcn-Domain
- [x] DefaultAttributeProvider                 → typisiert auf RcnAttribute
- [x] DefaultAttributeHelperProcessor (602!)   → typisiert auf RcnAttribute
- [x] AttributeProviderChain                   → typisiert auf RcnAttribute
- [x] TableAttributeProviderSelector           → typisiert auf RcnAttribute
- [x] ReferenceAttributeProvider               → Rcn-spezifisch
- [x] AttributeExpressionModelMapper           → Rcn-spezifisch
- [x] AttributeTransformerModelMapper          → Rcn-spezifisch
- [x] AttributeBeanWrapperModelMapper          → Rcn-spezifisch
```

**Condition-Implementierungen (5 Klassen) → RAUS:**
```
- [x] DefaultAttributeConditionProcessor       → IAttribute-spezifisch
- [x] AttributeTypeConditionProcessor          → IAttribute-spezifisch
- [x] AttributeSqlTypeConditionProcessor       → IAttribute-spezifisch
- [x] AttributeIsNumericConditionProcessor     → IAttribute-spezifisch
```

**Data-Implementierungen (2 Klassen) → RAUS:**
```
- [x] DefaultSearchFilterProcessor             → typisiert auf RcnAttribute
- [x] DataPage                                 → typisiert auf IAttribute, String[]
```

**Summe Ebene 3: ~17 Klassen → verbleiben in pfw-base-alt**


## Angepasster Schritt 3.9 (Condition) in Phase 0.5

### ALT (aus der ursprünglichen Checkliste):
```
Schritt 3.9: Condition-Subsystem (11 Klassen) → pfw-runtime
```

### NEU:
```
Schritt 3.9: Condition-Subsystem → pfw-runtime (NUR 7 Klassen)

API-Interfaces (4):
- [ ] ICondition                               condition/api/
- [ ] IConditionProcessor                      condition/api/
- [ ] IConditionProviderProcessor              condition/api/
- [ ] IConditionConfigProviderProcessor        condition/api/

Referenz-Implementierungen (3):
- [ ] ConditionConfig                          condition/domain/
- [ ] Condition                                condition/domain/
- [ ] DefaultConditionProvider                 condition/

NICHT verschieben (Rcn-spezifisch):
  ✗ DefaultAttributeConditionProcessor
  ✗ AttributeTypeConditionProcessor
  ✗ AttributeSqlTypeConditionProcessor
  ✗ AttributeIsNumericConditionProcessor
```


## Angepasster Schritt 3.10 (Attribute) in Phase 0.5

### ALT:
```
Schritt 3.10: Attribute-Subsystem (16 Klassen) → pfw-runtime
```

### NEU:
```
Schritt 3.10: Attribute-Subsystem → pfw-runtime (NUR 5 Interfaces)

API-Interfaces (5):
- [ ] IAttribute                               attribute/api/
- [ ] IAttributeProviderProcessor              attribute/api/
- [ ] IAttributeProviderSelectorProcessor      attribute/api/
- [ ] IAttributeModelMapperProcessor           attribute/api/
- [ ] IAttributeHelperProcessor                attribute/api/

NICHT verschieben (Rcn-spezifisch, 11 Klassen):
  ✗ RcnAttribute
  ✗ RcnAttributeProcessor
  ✗ DefaultAttributeProvider
  ✗ DefaultAttributeHelperProcessor (602 Zeilen!)
  ✗ AttributeProviderChain
  ✗ AttributeModelMapperChain
  ✗ TableAttributeProviderSelector
  ✗ ReferenceAttributeProvider
  ✗ AttributeExpressionModelMapper
  ✗ AttributeTransformerModelMapper
  ✗ AttributeBeanWrapperModelMapper
```


## Angepasster Schritt 3.11 (Data) in Phase 0.5

### ALT:
```
Schritt 3.11: Data-Subsystem (7 Klassen) → pfw-runtime
```

### NEU:
```
Schritt 3.11: Data-Subsystem → pfw-runtime (NUR 5 Klassen)

API-Interfaces (4):
- [ ] IDataProviderProcessor                   data/api/
- [ ] ISearchFilterProcessor                   data/api/
- [ ] IPager                                   data/api/
- [ ] IDataPage                                data/api/

Referenz-Implementierung (1):
- [ ] Pager                                    data/domain/

NICHT verschieben (Rcn-spezifisch):
  ✗ DefaultSearchFilterProcessor
  ✗ DataPage
```


## Aktualisierte Zusammenfassung pro Modul

| Modul | Klassen | Zeilen (ca.) | Änderung |
|-------|---------|-------------|----------|
| pfw-base | 39 | ~3.800 | unverändert |
| pfw-descriptor | 113 | ~6.500 | unverändert |
| pfw-runtime | **~78** | **~5.400** | **-30 Klassen, -1.800 Zeilen** |
| pfw-doc | 3+1 | ~360 | unverändert |
| **pfw-base-alt (Rest)** | **~17** | **~1.600** | **verbleibt, wird nicht verschoben** |
| **Gesamt Framework** | **~233** | **~16.060** | **schlanker, kein Rcn-Ballast** |

Die 17 verbleibenden Klassen in pfw-base-alt sind Kandidaten für ein
zukünftiges `recon-processors` Modul in der Abgleichs-Anwendung.


## Für Claude Code: Anweisung

Wenn du Phase 0.5 mit Claude Code durchführst:

```
claude "Lies CLAUDE_CODE_TASK_PHASE0.md und die Ergänzung 
PHASE0_FACHSPEZIFISCH_SEPARIEREN.md.

Bei Schritt 3.9 (Condition): Kopiere NUR die 4 API-Interfaces 
und die 3 Referenz-Implementierungen. Die Attribute*ConditionProcessors 
NICHT verschieben.

Bei Schritt 3.10 (Attribute): Kopiere NUR die 5 API-Interfaces. 
Alle Rcn*-Klassen, Default*-Implementierungen und *ModelMapper NICHT verschieben.

Bei Schritt 3.11 (Data): Kopiere NUR die 4 API-Interfaces und Pager. 
DefaultSearchFilterProcessor und DataPage NICHT verschieben."
```
