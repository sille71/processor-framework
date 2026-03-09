# PFW Migration Checkliste v3 — pfw-base als Prozessor-Genom

## Leitprinzip

> **"Kann ein Prozessor geboren werden, leben und sich beschreiben,
> ohne dass diese Klasse existiert?"**
>
> NEIN → pfw-base (essentiell)
> JA  → pfw-runtime oder pfw-descriptor (Erweiterung / Implementierung)


## Modulverteilung

```
pfw-base-alt (277 Klassen)
    │
    ├──► pfw-base        (~70 Klassen)    Das Prozessor-Genom
    │                                      Alles was zum Leben NÖTIG ist
    │
    ├──► pfw-descriptor  (~75 Klassen)    Descriptor-Implementierungen
    │                                      Default*, *ValueFunction, *Provider
    │
    ├──► pfw-runtime     (~85 Klassen)    Laufzeit + ALLE Erweiterungen
    │                                      Spring, Kernel, Condition, Attribute, ...
    │
    ├──► pfw-doc         (4 Klassen)      Dokumentations-Renderer
    │
    └──► verbleibt       (~17 Klassen)    Rcn-spezifisch
```


## Dependency-Kette (KEINE Rückwärts-Abhängigkeit)

```
pfw-base  ←──  pfw-descriptor  ←──  pfw-runtime  ←──  pfw-doc
                                                  ←──  pfw-sample
```


---


## PHASE 1: pfw-base (~70 Klassen)

### Tier 1 — Identität: Annotationen + Kern (6 Klassen)

```
- [ ] annotation/Processor.java                              105 Z.
- [ ] annotation/ProcessorParameter.java                     126 Z.
- [ ] annotation/ValueObject.java                             37 Z.
- [ ] domain/ProcessorScope.java                              17 Z.  Enum
- [ ] domain/api/IArtifact.java                                5 Z.
- [ ] exception/InvalidVersionNumberException.java             8 Z.
```


### Tier 2 — Kontext: Wo lebt der Prozessor? (6 Interfaces)

```
- [ ] processor/context/api/ITaskContext.java                 59 Z.
- [ ] processor/context/api/IProcessorContext.java           147 Z.
- [ ] processor/context/api/ITransformationContext.java        44 Z.
- [ ] processor/context/api/IRuntimeContextProviderProcessor.java  45 Z.
- [ ] processor/context/api/IContextCreationContext.java       15 Z.
- [ ] processor/context/api/LoadStrategy.java                 20 Z.  Enum
```

HINWEIS: IProcessorContext referenziert `IParameterProviderProcessor`
→ wandert als Tier-3-Interface ebenfalls nach pfw-base.

HINWEIS: ITransformationContext referenziert `IValueDescriptor`, `IValueFunction`
→ ok, diese sind Tier 6/7 und kommen ebenfalls nach pfw-base.


### Tier 3 — Erzeugung: Wer baut den Prozessor? (4 Interfaces)

```
- [ ] processor/api/IProcessor.java                          69 Z.
- [ ] processor/api/IBeanProvider.java                       23 Z.
- [ ] processor/api/IProcessorProvider.java                  20 Z.
- [ ] processor/parameter/api/IParameterProviderProcessor.java  31 Z.
      (referenziert von IProcessorContext)
```


### Tier 4 — Typ-System: Was für ein Typ bin ich? (4 Interfaces)

```
- [ ] processor/description/config/api/ITypeRef.java        156 Z.  Kern des Typsystems
- [ ] processor/description/config/api/ITypeRefProvider.java  32 Z.
- [ ] processor/description/config/api/ITypeResolutionContext.java 21 Z.
- [ ] processor/set/api/ISetProcessor.java                   26 Z.  Domain/CoDomain/Image
```


### Tier 5 — Funktions-Hierarchie: Wie transformiere ich? (9 Klassen)

**HINWEIS: IFunction/ISubjectFunction tragen wieder ISetProcessor
für Domain, CoDomain und Image — die mathematische Funktion ist unteilbar.**

```
Interfaces (7):
- [ ] processor/transformator/api/ITransformerProcessor.java          13 Z.
- [ ] processor/transformator/api/ISubjectTransformerProcessor.java   20 Z.
- [ ] processor/transformator/api/ISubjectFunction.java               25 Z.  ← hat ISetProcessor
- [ ] processor/transformator/api/IFunction.java                      25 Z.  ← hat ISetProcessor
- [ ] processor/transformator/api/IPropertyFunction.java              23 Z.  ← hat ISetProcessor
- [ ] processor/transformator/api/IValueTransformerProcessor.java      7 Z.
- [ ] processor/transformator/api/IBidirectionalPropertyTransformerProcessor.java 12 Z.

Abstrakte Basisklassen (2):
- [ ] processor/description/AbstractValueFunction.java                97 Z.
- [ ] processor/description/AbstractCollectionValueFunction.java     228 Z.
```


### Tier 6 — Selbstbeschreibung: Wie beschreibe ich mich? (12 Klassen)

```
Interfaces (11):
- [ ] processor/description/api/IDescriptorProcessor.java   141 Z.
- [ ] processor/description/api/IValueDescriptor.java       193 Z.
- [ ] processor/description/api/IParameterDescriptor.java   140 Z.
- [ ] processor/description/api/IParameterGroupDescriptor.java  9 Z.
- [ ] processor/description/api/IStructureValueDescriptor.java 83 Z.
- [ ] processor/description/api/IProcessorDescriptor.java    18 Z.
- [ ] processor/description/api/ICollectionValueDescriptor.java 22 Z.
- [ ] processor/description/api/IMapValueDescriptor.java     20 Z.
- [ ] processor/description/api/IReferenceValueDescriptor.java 29 Z.
- [ ] processor/description/api/IValueFunction.java         121 Z.
- [ ] processor/description/api/IDescriptorResolver.java     25 Z.

Abstrakte Basisklasse (1):
- [ ] processor/description/DescriptorProcessor.java         66 Z.
      (Basis für alle Descriptor-Knoten, extends AbstractProcessor)
```

HINWEIS: `IDomainProviderProcessor` — Grenzfall. Wird von Descriptoren
für Range/Proposals genutzt. Ist essentiell wenn Proposals zum
Lebenszyklus gehören (getPossibleValueDescriptors). Entscheidung:
```
- [ ] processor/description/api/IDomainProviderProcessor.java  8 Z.  ← OPTIONAL
```


### Tier 7 — Config-System: Wie wird mein Descriptor erzeugt? (13 Interfaces)

```
- [ ] processor/description/config/api/IDescriptorConfig.java           64 Z.
- [ ] processor/description/config/api/IDescriptorConfigProvider.java    9 Z.
- [ ] processor/description/config/api/IDescriptorConfigCreationContext.java 47 Z.
- [ ] processor/description/config/api/IValueConfig.java                59 Z.
- [ ] processor/description/config/api/IProcessorParameterConfig.java   98 Z.
- [ ] processor/description/config/api/IProcessorConfig.java             5 Z.
- [ ] processor/description/config/api/IValueObjectConfig.java          69 Z.
- [ ] processor/description/config/api/ICollectionValueConfig.java      10 Z.
- [ ] processor/description/config/api/IMapValueConfig.java             19 Z.
- [ ] processor/description/config/api/IValueFunctionConfig.java         3 Z.

Kontext-Interfaces für Descriptor/VF-Bau (2):
- [ ] processor/context/api/IDescriptorConstructorContext.java          93 Z.
- [ ] processor/context/api/IValueFunctionConstructorContext.java        8 Z.

Proposals (1):
- [ ] processor/context/api/IProposalContext.java                      15 Z.
```


### Tier 8 — Incubator: Hydration / Dehydration (22 Klassen)

```
API-Interfaces (10):
- [ ] processor/description/incubator/api/IIncubator.java              28 Z.
- [ ] processor/description/incubator/api/IIncubatorSession.java        6 Z.
- [ ] processor/description/incubator/api/IConstructSession.java        8 Z.
- [ ] processor/description/incubator/api/IDescribeSession.java        11 Z.
- [ ] processor/description/incubator/api/IEditSession.java            11 Z.
- [ ] processor/description/incubator/api/IConstructionManager.java     5 Z.
- [ ] processor/description/incubator/api/IInstanceProvider.java        7 Z.
- [ ] processor/description/incubator/api/IInstanceCreationContext.java 37 Z.
- [ ] processor/description/incubator/api/IDescriptorResolver.java      9 Z. (incubator)
- [ ] processor/description/incubator/api/ITypeIntrospector.java        9 Z.

Domain-Typen (12):
- [ ] processor/description/incubator/domain/ISeed.java                11 Z.
- [ ] processor/description/incubator/domain/IConstructSource.java     11 Z.
- [ ] processor/description/incubator/domain/IDescribeSource.java       3 Z.
- [ ] processor/description/incubator/domain/IEditSource.java           3 Z.
- [ ] processor/description/incubator/domain/IConstructPolicy.java      3 Z.
- [ ] processor/description/incubator/domain/IDescribePolicy.java       3 Z.
- [ ] processor/description/incubator/domain/IEditPolicy.java           3 Z.
- [ ] processor/description/incubator/domain/IBuildTaskContext.java      5 Z.
- [ ] processor/description/incubator/domain/IConstructTaskContext.java  3 Z.
- [ ] processor/description/incubator/domain/IDescribeTaskContext.java   3 Z.
- [ ] processor/description/incubator/domain/IEditTaskContext.java       3 Z.
- [ ] processor/description/incubator/domain/ConstructSource.java      14 Z.
```


### Tier 9 — Referenzierte Nebenspieler (3 Klassen)

```
- [ ] processor/assets/api/IAssetProviderProcessor.java     18 Z.
      (referenziert von IDescriptorProcessor.getGeneralAssetProviderProcessor)
- [ ] processor/artifact/api/IArtifactProvider.java          8 Z.
- [ ] processor/parameter/api/IParameterChangeListener.java  9 Z.
      (referenziert von AbstractProcessor: implements IParameterChangeListener)
```


### Tier 10 — Utils (12 Klassen)

```
- [ ] util/ProcessorUtils.java                             2042 Z.  ← Das Arbeitstier
- [ ] util/MapUtils.java                                    216 Z.
- [ ] util/MapDetailLevel.java                               20 Z.  Enum
- [ ] util/IdentifierUtils.java                              81 Z.
- [ ] util/StreamUtils.java                                  65 Z.
- [ ] util/ConfigurationModel.java                           21 Z.  Enum
- [ ] util/SerializationTarget.java                          17 Z.  Enum
- [ ] util/LogOutputHelper.java                              ~20 Z.  (Ersatz aus Phase 0.1)
- [ ] util/api/IUUIDIdentifier.java                           8 Z.
- [ ] util/UUIDIdentifier.java                               14 Z.
- [ ] util/ArtifactIdentifier.java                           49 Z.
- [ ] util/ArtifactIdentifierComparator.java                 62 Z.
```

**NICHT nach pfw-base:**
```
  ✗ Version.java          → Grenzfall, nur von ArtifactIdentifier genutzt
                             Falls ArtifactIdentifier es braucht → doch rein
  ✗ CalculationHelper.java → eher Fachlogik, pfw-runtime
  ✗ DocUtils.java          → pfw-doc (Flexmark-Dependency)
```

Prüfe: Braucht ArtifactIdentifier die Version-Klasse?
Falls ja → Version.java ebenfalls nach pfw-base.


### AbstractProcessor — DER zentrale Baustein (1 Klasse)

```
- [ ] processor/AbstractProcessor.java                      458 Z.
```

**ACHTUNG beim Kopieren — Bereinigungen in der KOPIE:**

1. Import `DefaultProcessorProvider` → entfernen (ist Implementierung, pfw-runtime)
   Stattdessen: nur IProcessorProvider nutzen
2. Import `FileSystemParameterProviderProcessor` → entfernen (ist Implementierung)
   Stattdessen: nur IParameterProviderProcessor nutzen
3. Import `ApplicationContext` / `BeanNameAware` → bleibt (Spring Context ist pfw-base Dependency)
4. Legacy-Reste (LogOutputHelper etc.) → müssen in Phase 0.1 schon bereinigt sein


### Kompilier-Verifikation

```bash
mvn compile -pl pfw-base
```

**Erwartung:** Kompiliert sauber. Keine PFW-intermodulare Dependency.
Externe Dependencies: Spring Context, Jackson, Commons Lang, Commons IO.


### Zusammenfassung pfw-base

```
Tier 1 - Identität:              6
Tier 2 - Kontext:                6
Tier 3 - Erzeugung:              4
Tier 4 - Typ-System:             4
Tier 5 - Funktionen:             9  (7 Interfaces + 2 Abstract)
Tier 6 - Selbstbeschreibung:    12  (11 Interfaces + 1 Abstract)
Tier 7 - Config-System:         13
Tier 8 - Incubator:             22
Tier 9 - Referenzierte:          3
Tier 10 - Utils:                12
AbstractProcessor:                1
                                ────
Gesamt pfw-base:               ~70  (+2 wenn Version + IDomainProviderProcessor)
```


---


## PHASE 2: pfw-descriptor (~75 Klassen)

Dependency: nur pfw-base.
**NUR Implementierungen — kein einziges neues Interface.**

### Schritt 2.1: TypeRef-Implementierungen (12)

```
- [ ] description/config/DefaultTypeRef.java
- [ ] description/config/DefaultTypeResolutionContext.java
- [ ] description/config/TypeRefProviderChain.java
- [ ] description/config/StringTypeRefProvider.java
- [ ] description/config/NumberTypeRefProvider.java
- [ ] description/config/BooleanTypeRefProvider.java
- [ ] description/config/EnumTypeRefProvider.java
- [ ] description/config/ProcessorTypeRefProvider.java
- [ ] description/config/ValueObjectTypeRefProvider.java
- [ ] description/config/ArrayTypeRefProvider.java
- [ ] description/config/ParameterizedTypeRefProvider.java
- [ ] description/config/ClassTypeRefProvider.java
```

### Schritt 2.2: Config-Implementierungen (15)

```
- [ ] description/config/DefaultDescriptorConfig.java
- [ ] description/config/ValueConfig.java
- [ ] description/config/ProcessorParameterConfig.java
- [ ] description/config/ProcessorConfig.java
- [ ] description/config/ValueObjectConfig.java
- [ ] description/config/ValueFunctionConfig.java
- [ ] description/config/CollectionValueConfig.java
- [ ] description/config/MapValueConfig.java
- [ ] description/config/DefaultDescriptorConfigCreationContext.java
- [ ] description/config/ScalarValueConfigProvider.java
- [ ] description/config/CollectionValueConfigProvider.java
- [ ] description/config/ProcessorParameterDescriptorConfigProvider.java
- [ ] description/config/ProcessorDescriptorConfigProvider.java
- [ ] description/config/ValueObjectDescriptorConfigProvider.java
- [ ] description/config/DescriptorConfigProviderChain.java
```

### Schritt 2.3: ValueFunctions (21)

```
Skalare (7):
- [ ] description/StringValueFunction.java
- [ ] description/IntegerValueFunction.java
- [ ] description/DoubleValueFunction.java
- [ ] description/BooleanValueFunction.java
- [ ] description/EnumValueFunction.java
- [ ] description/ClassValueFunction.java
- [ ] description/JacksonValueFunction.java

Collections (4):
- [ ] description/DefaultListValueFunction.java
- [ ] description/DefaultArrayValueFunction.java
- [ ] description/StringArrayValueFunction.java
- [ ] description/JacksonGenericListValueFunction.java

Prozessor (3):
- [ ] description/ProcessorValueFunction.java
- [ ] description/ProcessorListValueFunction.java
- [ ] description/ProcessorMapValueFunction.java

ValueObject (3):
- [ ] description/ValueObjectValueFunction.java
- [ ] description/ValueObjectListValueFunction.java
- [ ] description/ValueObjectMapValueFunction.java

Map (2):
- [ ] description/SimpleMapValueFunction.java
- [ ] description/DefaultMapValueFunction.java

Spezial (1):
- [ ] description/ArtifactIdentifierValueFunction.java
```

### Schritt 2.4: Descriptor-Implementierungen (11)

```
- [ ] description/DefaultValueDescriptor.java
- [ ] description/AnyValueDescriptor.java
- [ ] description/DefaultStructureValueDescriptor.java
- [ ] description/DefaultProcessorDescriptor.java
- [ ] description/DefaultParameterDescriptor.java
- [ ] description/DefaultParameterGroupDescriptor.java
- [ ] description/DefaultCollectionValueDescriptor.java
- [ ] description/DefaultMapValueDescriptor.java
- [ ] description/DefaultReferenceValueDescriptor.java
- [ ] description/DefaultDescriptorResolver.java
- [ ] description/BeanWrapperProcessor.java
```

### Schritt 2.5: Set-Implementierungen (3)

```
- [ ] processor/set/SimpleTypeSet.java
- [ ] processor/set/ProcessorPrototypeIdentifierSet.java
- [ ] processor/set/ProcessorPrototypeIdentifierListSet.java
```

### Schritt 2.6: Incubator-Implementierungen (5)

```
- [ ] description/incubator/FrameworkIncubator.java
- [ ] description/incubator/DefaultConstructionManager.java
- [ ] description/incubator/DefaultInstanceCreationContext.java
- [ ] description/incubator/DescriptorResolverChain.java
- [ ] description/incubator/TypeIntrospectorChain.java
```

### Kompilierung

```bash
mvn compile -pl pfw-base,pfw-descriptor
```


---


## PHASE 3: pfw-runtime (~85 Klassen)

Dependency: pfw-base + pfw-descriptor.
**Implementierungen + ALLE Erweiterungs-Interfaces und deren Referenz-Implementierungen.**

### Schritt 3.1: Context-Implementierungen (8)

```
- [ ] context/domain/DefaultProcessorContext.java
- [ ] context/domain/DefaultTaskContext.java
- [ ] context/domain/DefaultTransformationContext.java
- [ ] context/domain/DefaultDescriptorConstructorContext.java
- [ ] context/domain/DefaultContextCreationContext.java
- [ ] context/domain/DefaultValueFunctionConstructorContext.java
- [ ] context/domain/DefaultProposalContext.java
- [ ] context/DefaultRuntimeContextProviderProcessor.java
```

### Schritt 3.2: Provider-Implementierungen (3)

```
- [ ] processor/DefaultProcessorProvider.java
- [ ] processor/DefaultBeanProvider.java
- [ ] processor/DefaultValueObjectProviderProcessor.java
```

### Schritt 3.3: Parameter-Implementierungen (6)

```
- [ ] parameter/AbstractParameterProviderProcessor.java
- [ ] parameter/DefaultParameterProviderProcessor.java
- [ ] parameter/FileSystemParameterProviderProcessor.java
- [ ] parameter/ClassPathParameterProvider.java
- [ ] parameter/StringConfigurationParameterProvider.java
- [ ] parameter/DefaultBeanTypeMapProcessor.java
```

### Schritt 3.4: Kernel-System (9)

```
- [ ] kernel/api/IRuntimeKernelProcessor.java
- [ ] kernel/api/IKernelBeanProvider.java
- [ ] kernel/api/IPhaseProcessor.java
- [ ] kernel/domain/RunLevel.java
- [ ] kernel/KernelProcessor.java
- [ ] kernel/KernelBeanProvider.java
- [ ] kernel/KernelRuntimeContextProviderProcessor.java
- [ ] kernel/KernelConstructionManager.java
- [ ] kernel/FullIncubatorPhaseProcessor.java
```

### Schritt 3.5: Service-Runtime (4)

```
- [ ] service/api/IRuntimeServiceProcessor.java
- [ ] service/AbstractInstanceProcessorRuntime.java
- [ ] service/AbstractServiceProcessorRuntime.java
- [ ] service/TestInstanceProcessorRuntime.java
```

### Schritt 3.6: Transformator-Erweiterungen (7)

```
- [ ] transformator/api/IConfigurationModelTransformer.java
- [ ] transformator/IItemTransformatorProcessor.java
- [ ] transformator/JacksonValueTransformerProcessor.java
- [ ] transformator/JacksonValueTransformerChainProcessor.java
- [ ] transformator/JacksonGenericListValueTransformerProcessor.java
- [ ] transformator/DefaultConfigurationModelTransformer.java
- [ ] transformator/ConfigTransformerProcessor.java
```

### Schritt 3.7: Request-Erweiterung (15)

```
API (5):
- [ ] request/api/IRequestProcessor.java
- [ ] request/api/IRequestConsumerProcessor.java
- [ ] request/api/IRequestProducerProcessor.java
- [ ] request/api/IRequestInterceptorProcessor.java
- [ ] request/api/IUrlProviderProcessor.java

Implementierungen (10):
- [ ] request/JsonRequestDispatcherProcessor.java
- [ ] request/RequestDispatcherProcessorChain.java
- [ ] request/AssetRequestDispatcherProcessor.java
- [ ] request/DescriptionRequestProcessor.java
- [ ] request/DefaultMultipartHttpServletRequestDispatcherProcessor.java
- [ ] request/DefaultUrlProviderProcessor.java
- [ ] request/DefaultResponseCreatorProcessor.java
- [ ] request/DownloadXMLResponseCreatorProcessor.java
- [ ] request/ClassPathImageRequestConsumer.java
- [ ] request/domain/AssetHttpRequest.java
```

### Schritt 3.8: Variable-Erweiterung (2)

```
- [ ] variable/api/IVariableProcessor.java
- [ ] variable/DefaultVariableProcessor.java
```

### Schritt 3.9: Condition-Erweiterung — Interface + Referenz-Impl (7)

```
API-Interfaces (4):
- [ ] condition/api/ICondition.java
- [ ] condition/api/IConditionProcessor.java
- [ ] condition/api/IConditionProviderProcessor.java
- [ ] condition/api/IConditionConfigProviderProcessor.java

Referenz-Implementierungen (3):
- [ ] condition/domain/ConditionConfig.java
- [ ] condition/domain/Condition.java
- [ ] condition/DefaultConditionProvider.java

NICHT verschieben (Rcn-spezifisch → verbleibt in pfw-base-alt):
  ✗ DefaultAttributeConditionProcessor
  ✗ AttributeTypeConditionProcessor
  ✗ AttributeSqlTypeConditionProcessor
  ✗ AttributeIsNumericConditionProcessor
```

### Schritt 3.10: Attribute-Erweiterung — NUR Interfaces (5)

```
- [ ] attribute/api/IAttribute.java
- [ ] attribute/api/IAttributeProviderProcessor.java
- [ ] attribute/api/IAttributeProviderSelectorProcessor.java
- [ ] attribute/api/IAttributeModelMapperProcessor.java
- [ ] attribute/api/IAttributeHelperProcessor.java

NICHT verschieben (Rcn-spezifisch, 11 Klassen → verbleibt):
  ✗ RcnAttribute, RcnAttributeProcessor, DefaultAttributeProvider,
  ✗ DefaultAttributeHelperProcessor (602!), AttributeProviderChain,
  ✗ TableAttributeProviderSelector, ReferenceAttributeProvider,
  ✗ Attribute*ModelMapper (3x)
```

### Schritt 3.11: Data-Erweiterung — Interfaces + Pager (5)

```
- [ ] data/api/IDataProviderProcessor.java
- [ ] data/api/ISearchFilterProcessor.java
- [ ] data/api/IPager.java
- [ ] data/api/IDataPage.java
- [ ] data/domain/Pager.java

NICHT verschieben (Rcn-spezifisch):
  ✗ DefaultSearchFilterProcessor
  ✗ DataPage
```

### Schritt 3.12: Parameter-Erweiterungen (3)

```
- [ ] parameter/api/IParameterFormatterProcessor.java
- [ ] parameter/api/IParameterValidatorProcessor.java
- [ ] parameter/api/IBeanTypeMapProcessor.java
```

### Schritt 3.13: Assets-Implementierungen (3)

```
- [ ] assets/domain/Asset.java
- [ ] assets/AbstractAssetProviderProcessor.java
- [ ] assets/DocumentAssetProviderProcessor.java
```

### Schritt 3.14: Locale-Erweiterung (4)

```
- [ ] locale/api/ILocalDateTimeProcessor.java
- [ ] locale/api/ILocalProviderProcessor.java
- [ ] locale/DefaultLocalDateTimeProcessor.java
- [ ] locale/DefaultLocalProviderProcessor.java
```

### Schritt 3.15: Sonstiges (2)

```
- [ ] util/CalculationHelper.java
- [ ] StarimaBaseApplication.java
```

### Kompilierung

```bash
mvn compile -pl pfw-base,pfw-descriptor,pfw-runtime
```


---


## PHASE 4: pfw-doc (4 Klassen)

```
- [ ] doc/api/IDocRendererProcessor.java
- [ ] doc/DocRendererProcessor.java
- [ ] doc/MarkdownDocRendererProcessor.java
- [ ] util/DocUtils.java
```

```bash
mvn compile
```


---


## NICHT VERSCHIEBEN — verbleibt in pfw-base-alt (~17 Klassen)

```
Attribute-Implementierungen (11):
  RcnAttribute, RcnAttributeProcessor, DefaultAttributeProvider,
  DefaultAttributeHelperProcessor (602!), AttributeProviderChain,
  AttributeModelMapperChain, TableAttributeProviderSelector,
  ReferenceAttributeProvider, AttributeExpressionModelMapper,
  AttributeTransformerModelMapper, AttributeBeanWrapperModelMapper

Condition-Implementierungen (4):
  DefaultAttributeConditionProcessor, AttributeTypeConditionProcessor,
  AttributeSqlTypeConditionProcessor, AttributeIsNumericConditionProcessor

Data-Implementierungen (2):
  DefaultSearchFilterProcessor, DataPage
```

→ Später in `pfw-recon` oder die Abgleichs-Anwendung.


---


## Gesamtübersicht

| Modul | Klassen | Inhalt |
|-------|---------|--------|
| **pfw-base** | **~70** | Interfaces + AbstractProcessor + Annotationen + Utils |
| **pfw-descriptor** | **~75** | Default*-Implementierungen, ValueFunctions, Chains |
| **pfw-runtime** | **~85** | Provider, Kernel, Spring + ALLE Erweiterungs-APIs |
| **pfw-doc** | **4** | DocRenderer, MarkdownRenderer, DocUtils |
| **verbleibt** | **~17** | Rcn-spezifische Implementierungen |
| **Gesamt** | **~251** | |


### Die Klarheit des Modells

```
pfw-base:       "WAS das Framework ist"
                 → Ein Entwickler liest pfw-base und VERSTEHT das gesamte Framework
                 → Kein einziges Default*, kein einziger Provider, keine Spring-Klasse

pfw-descriptor: "WIE das Descriptor-System implementiert ist"
                 → Nur Default*-Klassen und *ValueFunctions

pfw-runtime:    "WIE die Laufzeit funktioniert + WAS es an Erweiterungen gibt"
                 → Spring-Integration, Kernel, Provider
                 → Erweiterungs-APIs (Condition, Attribute, Data, Request, ...)
                 → deren Referenz-Implementierungen
```
