# InstanceProvider — Hydration (provide)

## Überblick
Die Hydration beschreibt den Weg von einer rohen Konfiguration (beanParameterMap) zu einem vollständig initialisierten, lebendigen Objekt.

**Einstiegspunkt:** `InstanceProviderChain.provide(context)`

**Kontext:** `DefaultInstanceCreationContext.forProvide(type, paramValue, chain, runtimeCtx)`

---

## Sequenzdiagramm

```
Aufrufer
  │
  ▼
InstanceProviderChain.provide(ctx)
  │  setzt ctx.rootProvider = this
  │
  ├──► ProcessorInstanceProvider.isResponsibleFor()? → JA (IProcessor-Typ)
  │
  ▼
ProcessorInstanceProvider.provide(ctx)
  │
  ├─ Scope-Check: schon registriert? → zurück
  ├─ beanProvider.getBeanForId(prototypeId)
  ├─ setIdentifier, setScope
  ├─ isInCreationPath? → Abbruch (Zirkularität)
  ├─ creationStack.add(fullBeanId)          ← PUSH
  ├─ contextProviderResolver.resolve()
  ├─ provider.createContext()
  ├─ processor.setRuntimeContext(newCtx)
  │
  ├─ für jedes @ProcessorParameter-Feld:
  │    DefaultInstanceCreationContext childCtx = new(parent)
  │    childCtx.setTypeToResolve(field.type)
  │    childCtx.setParameterValue(paramValue)
  │    │
  │    ▼
  │    rootProvider.provide(childCtx)       ← REKURSION zur Chain
  │    (kann ScalarInstanceProvider, ProcessorInstanceProvider, etc. treffen)
  │    │
  │    ▼
  │    field.set(processor, value)
  │
  ├─ ProcessorUtils.registerProcessorInScope(processor, newCtx)
  ├─ processor.processorOnInit()
  └─ creationStack.remove(fullBeanId)       ← POP
```

---

## Zirkularitätserkennung

Der `CreationStack` ist ein `LinkedHashSet<String>` das über die gesamte Rekursionstiefe **geteilt** wird (im Kind-Konstruktor von `DefaultInstanceCreationContext` wird die Referenz übernommen, nicht kopiert).

```
Normaler Ablauf:
  provide("A") → push "A" → provide("B") → push "B" → provide("C") → push "C"
  → pop "C" → pop "B" → pop "A"

Zirkulärer Ablauf:
  provide("A") → push "A" → provide("B") → push "B" → provide("A")
  → isInCreationPath("A") = true → ABBRUCH, return null
```

**Sonderregel ProcessorDescriptor:** `getMaxRecursionDepth(type)` gibt `2` zurück für `IProcessorDescriptor`-Klassen, da Descriptoren sich selbst beschreiben dürfen (1 Ebene Selbstreferenz erlaubt).

---

## Rekursionsbeispiel

```
KernelProcessor
  ├── clusterContextProvider (IContextProvider)
  │     ├── fileSystemParamProvider (IParameterProvider)
  │     │     └── basePath: "/config" (String → ScalarInstanceProvider)
  │     └── propertiesParamProvider (IParameterProvider)
  │           └── fileName: "cluster.properties" (String → ScalarInstanceProvider)
  └── requestDispatcherProcessor (IProcessor)
        └── ...
```

Für jeden Knoten:
1. `ProcessorInstanceProvider` springt an (IProcessor-Typ)
2. `ScalarInstanceProvider` springt für String-Blätter an
3. CreationStack verhindert doppeltes Erzeugen derselben Bean

---

## Factory-Methode

```java
// Root-Kontext für eine provide()-Operation:
DefaultInstanceCreationContext ctx = DefaultInstanceCreationContext.forProvide(
    KernelProcessor.class,           // typeToResolve
    "kernelProcessor@singleton",     // parameterValue (fullBeanId)
    instanceProviderChain,           // rootProvider
    parentRuntimeContext             // runtimeContext
);
Object kernel = instanceProviderChain.provide(ctx);
```
