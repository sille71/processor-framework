# ADR-003: ITypeDescriptor + ValueFunctionResolverChain

## Status
Akzeptiert — in Fork A implementiert

## Kontext

### Inkonsistenz: Abhängigkeitsrichtung dreht sich um
- Skalare: InstanceProvider → ValueFunction (IP konsumiert VF)
- Strukturen: ValueFunction → InstanceProvider (VF delegiert an IP)
- Die Abhängigkeitsrichtung ist für Skalare und Strukturen ENTGEGENGESETZT

### IValueFunction mischt drei Concerns
1. **Typ-Beschreibung:** typeSignature, isProcessor, isGeneric, isEnum, ...
2. **Atomare Transformation:** transformValue, reverseTransformValue
3. **Orchestrierung (nur bei Strukturen):** ProcessorValueFunction ruft ProcessorProvider

### Config-System ist konsistenter
Im Config-System gibt es ITypeRef (reine Typ-Beschreibung) getrennt von
IValueFunctionConfig (Verweis auf Transformation). Im Descriptor-System
ist beides in der IValueFunction zusammengelegt.

### createValueFunctionForContext() ist eine implizite Chain
Die statische Methode in ProcessorUtils macht drei Schritte:
1. Annotation prüfen (explizite Konfiguration)
2. Dynamisch suchen (isResponsibleForSubject)
3. elementFunction/keyFunction injizieren (Komposition als Seiteneffekt)

## Entscheidung

### 1. ITypeDescriptor als Interface-Segregation (0 Breaking Changes)

Neues Interface extrahiert die Typ-Beschreibungs-Methoden:

```
ITypeDescriptor
  ├─ getTypeSignature(): String
  ├─ getTypeIdentifier(): String
  ├─ isGeneric(), isPolymorphic()
  ├─ isProcessor(), isValueObject()
  ├─ isNumeric(), isBoolean(), isString(), isEnum(), isDate(), isTime()
  ├─ isScalar() (default: atomarer Typ und nicht Struktur)
  └─ isStructure() (default: isProcessor || isValueObject)

IValueFunction extends ISubjectFunction, ITypeDescriptor, IDescriptorProcessor
```

Alle Methoden existieren bereits auf IValueFunction. Keine Implementierung ändert sich.
Reines Interface-Refactoring.

Korrespondenz mit Config-System:
```
Config:      ITypeRef         → Typ-Beschreibung (aus Java-Reflection erzeugt)
Descriptor:  ITypeDescriptor  → Typ-Beschreibung (von ValueFunction implementiert)
```

### 2. IValueDescriptor bekommt ITypeRef

IValueDescriptor erweitert um optionales ITypeRef-Feld:
- Default-Methoden: TypeRef first, ValueFunction als Fallback
- Konsistenz: Config und Descriptor nutzen dieselbe ITypeRef

### 3. ValueFunctionResolverChain

Formalisiert createValueFunctionForContext() als Chain.
Drei Resolver in @Order-Reihenfolge:

```
ValueFunctionResolverChain (@Primary)
  ├─ @Order(10): AnnotationVFResolver
  │    isResponsibleFor: @ProcessorParameter hat valueFunctionIdentifier?
  │    resolve: beanProvider.getBeanForId(vfId)
  │
  ├─ @Order(20): DescriptorVFResolver
  │    isResponsibleFor: context.valueDescriptor hat ValueFunction?
  │    resolve: return valueDescriptor.getValueFunction()
  │
  └─ @Order(30): DynamicVFResolver
       isResponsibleFor: true (Fallback)
       resolve: alle VF-Beans, filter isResponsibleForSubject()
```

### 4. Return-Typ: IValueFunction (raw, ohne Generics)

Am Resolution-Punkt wird IMMER zu raw gecastet. Jede Aufrufstelle
in ProcessorUtils hat @SuppressWarnings("rawtypes"). Der Resolver
bildet das ehrlich ab.

### 5. Keine elementFunction/keyFunction-Komposition

Die bisherige createValueFunctionForContext() hat bei Listen/Maps
die Kind-ValueFunction in die Eltern-ValueFunction injiziert
(Seiteneffekt). Das entfällt — Collection/MapInstanceProvider
machen die Rekursion über die InstanceProviderChain.

## Regeln

1. ITypeDescriptor ist ZERO Breaking Changes — nur Interface-Extraktion
2. IValueFunction extends ITypeDescriptor — keine Implementierung ändert sich
3. IValueFunctionResolver.resolve() gibt IValueFunction (raw) zurück
4. KEINE elementFunction/keyFunction-Injection im Resolver
5. IValueDescriptor Default-Methoden: ITypeRef → IValueFunction Fallback
6. Bestehende ValueFunctions UNVERÄNDERT
7. ProcessorUtils.createValueFunctionForContext() wird @Deprecated

## Verifikation

- [ ] ITypeDescriptor Interface mit allen Typ-Methoden
- [ ] IValueFunction extends ITypeDescriptor (KEIN Implementierungscode geändert)
- [ ] IValueDescriptor hat ITypeRef-Feld mit Fallback-Defaults
- [ ] ValueFunctionResolverChain: @Primary, ObjectProvider
- [ ] Drei Resolver: Annotation(10) → Descriptor(20) → Dynamic(30)
- [ ] resolve() Return-Typ: IValueFunction (raw)
- [ ] Keine elementFunction-Injection in Resolvern
- [ ] ScalarInstanceProvider nutzt ValueFunctionResolverChain
- [ ] createValueFunctionForContext() → @Deprecated
