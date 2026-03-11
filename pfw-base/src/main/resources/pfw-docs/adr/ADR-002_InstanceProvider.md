# ADR-002: Bidirektionaler IInstanceProvider als Herzstück der Objekterzeugung

## Status
Akzeptiert — in Fork A implementiert

## Kontext

### Bestehendes Framework-Pattern
Das Framework nutzt durchgehend Chain-of-Responsibility für Auflösungsebenen:
TypeRefProviderChain → DescriptorConfigProviderChain → ContextProviderResolverChain.
Jede Chain: Interface mit `isResponsibleFor(context)` + Auflösungsmethode,
Spring `ObjectProvider` für Sammlung, `@Primary` auf der Chain.

### Das Problem: Drei getrennte Erzeugungswege
1. **ProcessorProvider** für Prozessoren: Bean-Lookup + `processor.init(ctx)` (self-managed)
2. **ValueFunctions** für Parameter: `transformValue()` findet und transformiert Werte
3. **BeanProvider** für ValueObjects: `getBeanForId()` + `initBean()` (rekursive Parameterinitialisierung)

Diese drei Wege rufen sich gegenseitig auf, mit verteilter Zirkularitätserkennung
an drei verschiedenen Stellen.

### Bestehende Abstraktionen
- `IInstanceProvider extends IProcessor`: hatte `isResponsibleFor(ctx)` + `provide(ctx)`
- `IInstanceCreationContext extends ITaskContext`: hatte `typeToResolve`, `objectToResolve`,
  `fieldToResolve`, `processorParameter`, `rootProvider`, `descriptorConfigRootProvider`
- `IValueFunction<S, I, O>`: `transformValue()`, `reverseTransformValue()`, `getTypeSignature()`
- `IBeanProvider`: `getBeanForId()`, Spring ApplicationContext-Wrapper
- `IProcessorProvider extends IBeanProvider`: `getProcessorForBeanId()`, `getProcessorForType()`
- 20+ ValueFunction-Implementierungen (String, Integer, Boolean, Enum, Jackson, etc.)

## Entscheidung

### 1. IInstanceProvider wird bidirektional

Neue Methode `extract()` neben `provide()`:

```
IInstanceProvider extends IProcessor
  ├─ isResponsibleFor(IInstanceCreationContext): boolean
  ├─ provide(IInstanceCreationContext): Object    // Config → Objekt (Hydration)
  └─ extract(IInstanceCreationContext): Object    // Objekt → Config (Dehydration)
```

Beide geben `Object` zurück — bewusst untypisiert:
- `provide()`: das erzeugte Objekt (wird per Reflection in typisiertes Feld injiziert)
- `extract()`: der rohe Referenzwert (fullBeanId, Identifier, primitiver Wert)

Die Typsicherheit liegt im Compiler (@ProcessorParameter-Feldtyp),
im Descriptor-System (typeSignature) und im Roundtrip-Test.

### 2. IInstanceCreationContext erweitert

Neue Felder für beide Richtungen:

**Für provide():**
- `parameterValue`: roher Konfigurationswert aus der beanParameterMap
- `creationStack` (Set<String>): fullBeanIds der aktuell in Erzeugung befindlichen Objekte
- `isInCreationPath(fullBeanId)`: Zirkularitätserkennung

**Für extract():**
- `visitedSet` (Set<Object>, IdentityHashMap-basiert): bereits extrahierte Objekte
- `isAlreadyVisited(instance)`, `markVisited(instance)`: Zykluserkennung
- `extractionResult` (Map<String, Map<String, Object>>): die aufzubauende beanParameterMap

**Factory-Methoden:**
- `forProvide(type, paramValue, rootProvider, runtimeContext)`: Root-Context für Hydration
- `forExtract(object, rootProvider, runtimeContext)`: Root-Context für Dehydration
- `createChildForField(field, paramValue)`: Kind-Context für Parameter-Rekursion
- `createChildForExtraction(field, childObject)`: Kind-Context für Extraktions-Rekursion

### 3. InstanceProvider-Hierarchie

```
InstanceProviderChain (@Primary, @Order irrelevant)
  │
  ├─ @Order(10): ProcessorInstanceProvider
  │    isResponsibleFor: IProcessor.class.isAssignableFrom(typeToResolve)
  │    provide: Scope-Check → Bean-Lookup → Zirkularität → Context → Parameter(rekursiv) → Init
  │    extract: Visited-Check → fullBeanId → Parameter(rekursiv) → extractionResult.put()
  │
  ├─ @Order(20): ValueObjectInstanceProvider
  │    isResponsibleFor: @ValueObject-annotiert
  │    provide: Bean-Lookup → Parameter(rekursiv)
  │    extract: Visited-Check → Identifier → Parameter(rekursiv) → extractionResult.put()
  │
  ├─ @Order(25): CollectionInstanceProvider
  │    isResponsibleFor: Collection, Array
  │    provide: Element-Typ(Generics) → pro Element: Chain.provide(elementCtx)
  │    extract: pro Element: Chain.extract(elementCtx) → Liste von Raw-Werten
  │
  ├─ @Order(25): MapInstanceProvider
  │    isResponsibleFor: Map
  │    provide: Key+Value-Typ → pro Entry: Chain.provide()
  │    extract: pro Entry: Chain.extract() → Map von Raw-Paaren
  │
  └─ @Order(30): ScalarInstanceProvider
       isResponsibleFor: alles andere (String, int, boolean, enum, Class, ...)
       provide: DELEGIERT an bestehende IValueFunction.transformValue()
       extract: DELEGIERT an bestehende IValueFunction.reverseTransformValue()
```

### 4. ValueFunction-Brücke (kein Clean Break)

Die bestehenden 20+ ValueFunctions werden NICHT umgeschrieben.
Der ScalarInstanceProvider delegiert an sie:
- `provide()` → `createValueFunctionForContext()` → `vf.transformValue()`
- `extract()` → `createValueFunctionForContext()` → `vf.reverseTransformValue()`

Die ValueFunction behält ihre Descriptor-Aufgaben (typeSignature, isGeneric, etc.).

### 5. ProcessorProvider bleibt als Fassade

`IProcessorProvider` Interface unverändert. `DefaultProcessorProvider.getProcessorForBeanIdWithType()`
baut intern einen `IInstanceCreationContext` und delegiert an die Chain.
Alle bestehenden Aufrufer funktionieren weiterhin.

### 6. Zirkularitätserkennung — zentral

**provide():** CreationStack (Set<String> von fullBeanIds). PUSH vor Erzeugung, POP im finally.
**extract():** VisitedSet (IdentityHashMap-basiert). Mark vor Rekursion.

Sonderregel: ProcessorDescriptoren dürfen 2 Ebenen tief rekurrieren
(`getMaxRecursionDepth(IProcessorDescriptor.class) == 2`).

## Regeln

1. **Object als Return-Typ:** provide() und extract() geben Object zurück.
   Die Chain-Fähigkeit und die Typ-Heterogenität erfordern das.

2. **Rekursionsmuster:** Struktur-Provider (Processor, ValueObject) iterieren über
   @ProcessorParameter-Felder und rufen `rootProvider.provide/extract(childContext)` auf.
   Scalar-Provider sind Blätter — keine Rekursion.

3. **Geteilte Ressourcen:** CreationStack, VisitedSet und ExtractionResult werden
   im Kind-Context per REFERENZ (nicht Kopie) weitergegeben. Sie sind global
   über die gesamte Rekursionstiefe.

4. **@Order-Reihenfolge:** Processor(10) → ValueObject(20) → Collection(25) → Scalar(30).
   Spezifischere Provider zuerst.

5. **ValueFunction-Brücke:** ScalarInstanceProvider delegiert an bestehende ValueFunctions.
   Keine neuen ValueFunctions nötig. Kein Clean Break.

6. **Roundtrip-Invarianz:** `extract(provide(config))` muss semantisch gleich `config` sein.
   Bekannte Asymmetrie: Default-Werte werden explizit (Config wird "normalisiert").

## Sequenz — provide()

```
ProcessorInstanceProvider.provide(ctx)
  ├─ scopeCheck → existiert? return
  ├─ beanProvider.getBeanForId(prototypeId) → Instanz
  ├─ setIdentifier, setScope
  ├─ creationStack.add(fullBeanId)                    ◄ PUSH
  ├─ contextProviderResolver.resolve() → provider
  ├─ provider.createContext() → newCtx
  ├─ injectField("reconContext", newCtx)
  ├─ for each @ProcessorParameter:
  │    childCtx = createChildForField(field, paramValue)
  │    value = rootProvider.provide(childCtx)          ◄ REKURSION
  │    field.set(processor, value)
  ├─ creationStack.remove(fullBeanId)                  ◄ POP
  └─ processorOnInit()
```

## Sequenz — extract()

```
ProcessorInstanceProvider.extract(ctx)
  ├─ visitedSet.contains(processor)? → return fullBeanId
  ├─ visitedSet.add(processor)                         ◄ MARK
  ├─ fullBeanId = processor.getFullBeanId()
  ├─ for each @ProcessorParameter:
  │    childCtx = createChildForExtraction(field, fieldValue)
  │    rawValue = rootProvider.extract(childCtx)       ◄ REKURSION
  │    parameters.put(fieldName, rawValue)
  ├─ extractionResult.put(identifier, parameters)
  └─ return fullBeanId
```

## Verifikation

- [ ] provide() und extract() auf IInstanceProvider Interface
- [ ] Object als Return-Typ bei beiden (kein Generics)
- [ ] IInstanceCreationContext hat: creationStack, visitedSet, extractionResult, parameterValue
- [ ] DefaultInstanceCreationContext hat: forProvide(), forExtract(), createChildForField(), createChildForExtraction()
- [ ] InstanceProviderChain: @Primary, ObjectProvider, setzt rootProvider
- [ ] ProcessorInstanceProvider: @Order(10), Scope-Check + Context + Rekursion
- [ ] ScalarInstanceProvider: @Order(30), DELEGIERT an IValueFunction (nicht neu implementiert)
- [ ] Bestehende ValueFunctions UNVERÄNDERT
- [ ] IProcessorProvider Interface UNVERÄNDERT
- [ ] DefaultProcessorProvider delegiert intern an InstanceProviderChain
- [ ] CreationStack als Set<String> (fullBeanIds), VisitedSet als IdentityHashMap-basiert
- [ ] ProcessorDescriptor-Sonderregel: maxRecursionDepth == 2
- [ ] Dokumentation: @Processor(description, categories, tags) auf jeder Klasse
- [ ] Langbeschreibung: resources/docs/processors/<prototypeIdentifier>.md

## Konsequenzen

**Positiv:**
- Ein einziger Einstiegspunkt für alle Objekterzeugung
- Zentrale Zirkularitätserkennung statt drei verteilte
- Bidirektional: provide + extract als inverse Operationen
- Roundtrip-testbar: algebraischer Korrektheitsbeweis möglich
- ValueFunctions bleiben erhalten (Brücke statt Bruch)
- Konsistent mit bestehendem Chain-Pattern

**Negativ:**
- 6 neue Klassen (Chain + 5 Provider)
- Zwei Indirektionsebenen (Chain → Provider → ValueFunction für Skalare)
- Migration: ProcessorProvider muss schrittweise umgestellt werden

**Trade-offs:**
- Object statt Generics: weniger Typsicherheit im Provider, aber Chain-fähig
- ValueFunction-Brücke: keine saubere Trennung, aber kein Umbau von 20+ Klassen
