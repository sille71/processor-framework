# InstanceProvider — Hydration und Dehydration

> *Das morphogenetische Feld — stellt die Bedingungen her,
> unter denen sich der Prozessor selbst entfalten kann.*

## Kernkonzept

Der InstanceProvider ist die operative Schicht der Objekterzeugung.
Bidirektional: `provide()` (Config → Objekt) und `extract()` (Objekt → Config).

Die Roundtrip-Invarianz `extract(provide(config)) == config` ist der
algebraische Korrektheitsbeweis.


## Die Chain

```
InstanceProviderChain (@Primary)
  ├─ @Order(10): ProcessorInstanceProvider     Scope + Context + Rekursion
  ├─ @Order(20): ValueObjectInstanceProvider   Bean + Rekursion (ohne Context)
  ├─ @Order(25): CollectionInstanceProvider    Element-Rekursion
  ├─ @Order(25): MapInstanceProvider           Key + Value Rekursion
  └─ @Order(30): ScalarInstanceProvider        VFResolver → VF.transformValue()
```


## ProcessorInstanceProvider — provide()

```
1. parameterValue interpretieren (String → fullBeanId)
2. Scope-Check: getProcessorFromScope() → bereits vorhanden?
   - @provided: NUR Lookup, KEINE Erzeugung. Fehler wenn nicht da.
   - @instance: Lookup, dann Erzeugung wenn nötig.
   - @prototype: immer neu erzeugen.
3. Bean-Lookup: beanProvider.getBeanForId(prototypeId)
4. Identifier + Scope setzen
5. Zirkularität: creationStack.contains(fullBeanId)? → abbrechen
6. creationStack.add(fullBeanId) — PUSH
7. Context: contextProviderResolver.resolve() → createContext()
8. Parameter-Iteration: 
   - Descriptor vorhanden? → parameterDescriptors (geordnet)
   - Kein Descriptor? → @ProcessorParameter-Felder
   - Pro Feld: childCtx → instanceProviderChain.provide(childCtx) — REKURSION
9. creationStack.remove(fullBeanId) — POP (finally!)
10. processorOnInit()
11. Scope-Registrierung
```


## ProcessorInstanceProvider — extract()

```
1. Zykluserkennung: visitedSet.contains(processor)? → return fullBeanId
2. visitedSet.add(processor)
3. Parameter-Iteration: @ProcessorParameter-Felder
   Pro Feld: childCtx → instanceProviderChain.extract(childCtx) — REKURSION
4. extractionResult.put(identifier, parameters)
5. return fullBeanId
```


## Kontext-Vereinheitlichung

`IInstanceCreationContext extends ITransformationContext` — ein einziger
Kontext fließt durchs gesamte System.

```
ScalarIP.provide(ctx)
  → vf.transformValue(ctx, input)     ← ctx IST ITransformationContext
  
ProcessorVF.transformValue(ctx, input)
  → instanceProviderChain.provide(ctx) ← ctx IST IInstanceCreationContext
  
KEINE Konvertierung. Ein Objekt, zwei Interface-Sichten.
```


## ValueFunction als vollständige mathematische Funktion

Die ValueFunction verliert KEINE Methoden. Für Skalare implementiert sie
f direkt. Für Strukturen delegiert sie an den InstanceProvider:

```
Skalar:   ScalarIP → VF.transformValue()       (IP benutzt VF)
Struktur: VF.transformValue() → ProcessorIP    (VF delegiert an IP)
```

Beide Richtungen enden beim selben Ergebnis. Kein Zirkel — die Chain
ruft die VF für Strukturen nie direkt auf.
