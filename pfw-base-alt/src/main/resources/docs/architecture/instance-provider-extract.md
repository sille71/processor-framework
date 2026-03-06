# InstanceProvider — Dehydration (extract)

## Überblick
Die Dehydration beschreibt den umgekehrten Weg: von einem lebendigen Objekt zurück zu einer rekonstruierbaren Konfiguration (beanParameterMap).

**Einstiegspunkt:** `InstanceProviderChain.extract(context)`

**Kontext:** `DefaultInstanceCreationContext.forExtract(object, chain, runtimeCtx)`

---

## Sequenzdiagramm

```
Aufrufer
  │
  ▼
InstanceProviderChain.extract(ctx)
  │  setzt ctx.rootProvider = this
  │
  ├──► ProcessorInstanceProvider.isResponsibleFor()? → JA (IProcessor)
  │
  ▼
ProcessorInstanceProvider.extract(ctx)
  │
  ├─ isAlreadyVisited(processor)? → return fullBeanId (nur Referenz)
  ├─ markVisited(processor)
  ├─ fullBeanId = processor.getFullBeanId()
  │
  ├─ für jedes @ProcessorParameter-Feld (nicht ignoreExtractParameter):
  │    childValue = field.get(processor)
  │    DefaultInstanceCreationContext childCtx = createChildForExtraction(field, childValue)
  │    │
  │    ▼
  │    rootProvider.extract(childCtx)         ← REKURSION zur Chain
  │    (Rückgabe: rawChildValue = fullBeanId, identifier, primitiver Wert, ...)
  │    │
  │    parameters.put(paramName, rawChildValue)
  │
  ├─ extractionResult.put(identifier, parameters)   ← NEBENEFFEKT
  └─ return fullBeanId
```

---

## VisitedSet-Mechanik

Das `visitedSet` ist ein **Identity-basiertes Set** (`Collections.newSetFromMap(new IdentityHashMap<>())`):
- Basiert auf `==` (Objekt-Identität), nicht auf `equals()`
- Verhindert Endlosrekursion bei zirkulären Referenzen
- Wird im Kind-Konstruktor von `DefaultInstanceCreationContext` als Referenz geteilt

```
Normaler Ablauf:
  extract(A) → visited={A} → extract(B) → visited={A,B} → extract(C) → ...

Zirkulärer Ablauf:
  extract(A) → visited={A} → extract(B) → visited={A,B} → extract(A)
  → isAlreadyVisited(A) = true → return A.getFullBeanId() (nur Referenz, keine Rekursion)
```

---

## Extraktionsergebnis

Das `extractionResult` ist eine `LinkedHashMap<String, Map<String, Object>>`:
- Key: Bean-Identifier (z.B. `"instance1"`)
- Value: Parameter-Map des Objekts (z.B. `{ "paramA": "valueA", "childBean": "childBean:id@context" }`)

**Wichtig:** Nur strukturelle Provider (Prozessoren, ValueObjects) schreiben in diese Map. Skalare geben ihren Wert direkt zurück und schreiben nicht in die Map.

```java
// Ergebnis für eine extrahierte Prozessor-Hierarchie:
extractionResult = {
    "instance1": {
        "paramProvider": "fileSystemParamProvider:fp1@context",
        "threshold": "42",
        "tags": ["a", "b", "c"]
    },
    "fp1": {
        "basePath": "/config/cluster"
    }
}
```

---

## Roundtrip-Invarianz

Die algebraische Kernaussage des InstanceProvider-Systems:

```
extract(provide(config)) ≈ config
```

Das bedeutet: Die aus einem Objekt extrahierte Konfiguration muss ausreichen, um dasselbe Objekt wieder zu erzeugen. Dies ist der Beweis, dass Hydration und Dehydration konsistent implementiert sind.

**Abweichungen** sind erlaubt für:
- Berechnete Felder (`ignoreExtractParameter = true`)
- Injizierte Spring-Beans ohne Konfiguration

---

## Factory-Methode

```java
// Root-Kontext für eine extract()-Operation:
DefaultInstanceCreationContext ctx = DefaultInstanceCreationContext.forExtract(
    kernelProcessorInstance,    // objectToResolve
    instanceProviderChain,      // rootProvider
    runtimeContext              // runtimeContext
);
instanceProviderChain.extract(ctx);

// Ergebnis:
Map<String, Map<String, Object>> config = ctx.getExtractionResult();
```
