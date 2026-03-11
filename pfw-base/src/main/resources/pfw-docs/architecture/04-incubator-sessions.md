# Incubator — Sessions und Cursor-Stages

> *Der Brutkasten — stellt die Umgebung bereit, in der sich
> der Prozessor unter kontrollierten Bedingungen entfaltet.*

## Drei Modi, drei Sessions

```
CONSTRUCT:  beanParameterMap → lebendiger Objektgraph
            IConstructTaskContext → IConstructSession

DESCRIBE:   Java-Objekt/Typ → Descriptor-Graph
            IDescribeTaskContext → IDescribeSession

EDIT:       Langlebiger Workspace + Lazy Loading + Patches
            IEditTaskContext → IEditSession
```

Der Incubator erzeugt pro Aufruf einen TaskContext mit ConstructionManager
und delegiert an die InstanceProviderChain.


## TaskContext-Hierarchie (ersetzt Sources + Policies)

```
IBuildTaskContext extends ITaskContext
  ├── BuildMode (CONSTRUCT | DESCRIBE | EDIT)
  ├── IConstructionManager
  ├── maxDepth, loadStrategy
  │
  ├── IConstructTaskContext
  │     rootBeanId, targetType, draftProcessor, beanParameterMap
  │
  ├── IDescribeTaskContext
  │     sourceObject, sourceType, sourceField
  │
  └── IEditTaskContext
        editTarget, maxDepth, pageSize
```


## Cursor-Stages (Lazy Loading)

> Biologisch: Nicht alle Gene werden gleichzeitig exprimiert.
> Zellen aktivieren Gene nur bei Bedarf.

```
Root:
  R1  ROOT_HEADER           → ProcessorDescriptor (ohne Slots)
  R2  ROOT_SLOTS_ENUM       → Slot-Keys ["title", "path", ...]

Slot:
  S1  SLOT_DESCRIPTOR        → ParameterDescriptor (ohne ValueDescriptor)

Value:
  V1  VALUE_HEADER           → ValueDescriptor-Typ (Scalar/Collection/Map/Poly)

Scalar:
  SC1 SCALAR_FUNCTION        → ValueFunction-Metadaten
  SC2 SCALAR_RAWVALUE        → konkreter Wert

Processor/Structure:
  P1  PROCESSOR_SLOTS        → Subprozessor-Slots (Rekursion)

Collection:
  C1  COLLECTION_META        → elementType, Größe
  C2  COLLECTION_ELEMENT_SCHEMA → Element-Descriptor
  C3  COLLECTION_ITEMS_PAGE  → Items seitenweise

Map:
  M1  MAP_META               → keyType, valueType
  M2  MAP_KEY_SCHEMA         → Key-Descriptor
  M3  MAP_VALUE_SCHEMA       → Value-Descriptor
  M4  MAP_ENTRIES_PAGE       → Entries seitenweise

Polymorphie:
  PO1 POLY_META              → Polymorphie-Info
  PO2 POLY_CANDIDATES_INDEX  → getPossibleValueDescriptors()
  PO3 POLY_SELECTION_SET     → Auswahl getroffen
  PO4 POLY_SELECTED_SUBTREE  → Unterteilbaum für gewählten Typ
```


## Edit-Session und Patches

```
IEditSession
  ├── getWorkspace(): IDescriptorWorkspace
  │     ├── root(): IDescriptorProcessor
  │     ├── find(path): Optional<IDescriptorProcessor>
  │     ├── replace(path, newNode)
  │     ├── getPlaceholders(): List<IPlaceholderDescriptor>
  │     └── exportBeanParameterMap()
  │
  ├── expand(path, policy): ExpandResult
  │     Ersetzt Placeholder durch vollständigen Descriptor
  │
  └── applyPatch(patch): PatchResult
        SetRawValuePatch, SelectCandidatePatch,
        InsertListItemPatch, RemoveListItemPatch,
        InsertMapEntryPatch, RemoveMapEntryPatch
```


## Placeholder-Descriptoren

```
IPlaceholderDescriptor extends IDescriptorProcessor
  ├── PlaceholderKind: SLOT | VALUE | CHILDREN | CANDIDATES | SUBTREE
  ├── targetPath
  ├── nextStage (welche Cursor-Stage beim Expand)
  └── expandHints (selectedPrototypeId, pageRange, ...)
```
