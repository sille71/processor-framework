# Descriptor-System und Typsystem

> *The Class is the Schema.*

## Das Descriptor-System als sprachunabhängiges Typsystem

Jeder Prozessor beschreibt sich selbst vollständig über seinen Descriptor.
Das UI kommuniziert ausschließlich via ProcessorDescriptor — diese Grenze
ist architektonisch tragend.

```
IDescriptorProcessor
  └── IValueDescriptor                    hat: IValueFunction, ITypeRef, rawValue, resolvedValue
        ├── Skalar (String, Integer, Boolean, Enum, ...)
        ├── ICollectionValueDescriptor    hat: elementValueDescriptor
        ├── IMapValueDescriptor           hat: keyValueDescriptor + valueValueDescriptor
        └── IStructureValueDescriptor     hat: List<IParameterDescriptor>
              ├── IProcessorDescriptor
              └── (ValueObjectDescriptor)
```


## ITypeDescriptor — Interface-Segregation

Extrahiert die Typ-Metadaten aus IValueFunction als eigenständiges Interface.
Korrespondiert mit ITypeRef im Config-System.

```
ITypeDescriptor (Beschreibung: Was ist der Typ?)
  ├── getTypeSignature(), getTypeIdentifier()
  ├── isProcessor(), isValueObject(), isGeneric(), isPolymorphic()
  ├── isNumeric(), isBoolean(), isString(), isEnum(), isDate(), isTime()
  ├── isScalar() (default), isStructure() (default)

IValueFunction extends ITypeDescriptor (Transformation: Wie transformiere ich?)
  ├── transformValue(), reverseTransformValue()
  ├── getDomainProcessor(), getCoDomainProcessor(), getImageProcessor()
```

IValueDescriptor hat ITypeRef als primäre Typ-Quelle (Fallback: ValueFunction):

```
Config-System:      IValueConfig → ITypeRef (Beschreibung) + IValueFunctionConfig
Descriptor-System:  IValueDescriptor → ITypeRef (Beschreibung) + IValueFunction
```


## ValueFunctionResolverChain

Formalisiert `ProcessorUtils.createValueFunctionForContext()` als Chain:

```
ValueFunctionResolverChain (@Primary)
  ├── @Order(10): AnnotationVFResolver    → @ProcessorParameter(valueFunctionId)
  ├── @Order(20): DescriptorVFResolver    → valueDescriptor.getValueFunction()
  └── @Order(30): DynamicVFResolver       → isResponsibleForSubject() Suche
```

Return-Typ: `IValueFunction` (raw). Keine elementFunction-Injection —
Collection/MapInstanceProvider machen die Rekursion selbst.


## Descriptor aus UI-Perspektive

| Anwendungsfall | UI-Widget | Descriptor-Abbildung |
|---|---|---|
| String-Parameter | Textfeld | typeSignature="string", required, defaultValue |
| Integer mit Range | Nummernfeld/Slider | typeSignature="integer", Domain-Set |
| Boolean | Toggle | typeSignature="boolean" |
| Enum | Dropdown | typeSignature="enum", getPossibleRawValues() |
| Prozessor (polymorph) | Suchfeld/Dropdown | isProcessor + getPossibleValueDescriptors() |
| Liste von Prozessoren | Wiederholbare Gruppe | elementValueDescriptor.isProcessor() |
| Map<String, Processor> | Key-Value-Tabelle | keyVD + valueVD |
| ValueObject | Inline-Formular | IStructureVD.parameterDescriptors |


## Ausstehend: Werte-Constraints

Leichtgewichtige Ergänzung zum ISetProcessor-Konzept:

```
min/max, minLength/maxLength, pattern, minItems/maxItems, step,
readOnly, nullable
```

Als IValueConstraints-Objekt auf IValueDescriptor oder IParameterDescriptor.
Additiv, bricht nichts. ISetProcessor bleibt für komplexe Wertebereiche.
