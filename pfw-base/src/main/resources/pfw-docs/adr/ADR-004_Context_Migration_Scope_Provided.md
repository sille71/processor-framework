# ADR-004: Source → Context Migration + Scope Provided

## Status
Akzeptiert

## Kontext

### Source-Objekte sind redundant
IConstructSource, IDescribeSource und IEditSource sind reine Datenträger,
deren Felder identisch mit den TaskContext-Feldern sind. Der Incubator
wandelt Sources intern in Contexte um — eine unnötige Indirektion.

### Neuer Scope: provided
Services brauchen Infrastruktur-Prozessoren (Gateway, Incubator), die
vom Kernel bereitgestellt werden. Ein neuer Scope `provided` deklariert
diese Abhängigkeit explizit.

## Entscheidung

### 1. Sources → TaskContexte
```
IConstructSource + IConstructPolicy → IConstructTaskContext
IDescribeSource + IDescribePolicy   → IDescribeTaskContext
IEditSource + IEditPolicy           → IEditTaskContext
```

IIncubator-Signaturen: `startConstruct(IConstructTaskContext)` statt
`startConstruct(IConstructSource, IConstructPolicy)`.

### 2. ProcessorScope.provided
Lookup in der Kontext-Hierarchie. Fehler wenn nicht gefunden.
Keine Erzeugung.

### 3. Parameter-Refresh als Patch
Edit-Session-Patches werden das Refresh-Interface.
Skalar-Refresh (schnell) vs Struktur-Refresh (Rebuild Sub-Baum).
processorOnDestroy() als fehlender Lifecycle-Hook.

## Verifikation
- [ ] IBuildTaskContext erweitert: BuildMode, ConstructionManager
- [ ] IConstructTaskContext: rootBeanId, targetType, draftProcessor
- [ ] IDescribeTaskContext: sourceObject, sourceType, sourceField
- [ ] IEditTaskContext: editTarget, maxDepth, pageSize
- [ ] IIncubator: neue Signaturen
- [ ] ProcessorScope: + provided
- [ ] Source/Policy-Interfaces gelöscht
