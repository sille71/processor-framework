# InstanceProviderChain

## Überblick
Die `InstanceProviderChain` ist der primäre Einstiegspunkt für Objekt-Erzeugung (Hydration) und -Extraktion (Dehydration). Sie sammelt alle registrierten `IInstanceProvider`-Beans und delegiert an den ersten zuständigen.

## Verantwortlichkeit
`isResponsibleFor()` gibt immer `true` zurück — die Chain ist für alle Typen zuständig und delegiert intern. Kein direkter Ausschluss.

## provide() — Hydration
1. Setzt sich selbst als `rootProvider` im Kontext (damit rekursive Aufrufe zurück zur Chain laufen).
2. Iteriert über alle Provider in `@Order`-Reihenfolge.
3. Delegiert an den ersten, der `isResponsibleFor()` zurückgibt.
4. Gibt `null` zurück und loggt eine Warnung, wenn kein Provider zuständig ist.

## extract() — Dehydration
Analoger Ablauf: setzt `rootProvider`, iteriert, delegiert an ersten zuständigen.

## Parameter
| Parameter | Typ | Default | Beschreibung |
|---|---|---|---|
| — | — | — | Keine eigenen Parameter. Provider werden via Spring `ObjectProvider` injiziert. |

## Zusammenspiel
```
InstanceProviderChain (rootProvider)
  └── ProcessorInstanceProvider   (@Order 10)
  └── ValueObjectInstanceProvider (@Order 20)
  └── CollectionInstanceProvider  (@Order 25)
  └── MapInstanceProvider         (@Order 25)
  └── ScalarInstanceProvider      (@Order 30)
```
Die Chain ist `@Primary` und wird injiziert, wenn `IInstanceProvider` angefordert wird. Jeder strukturelle Provider ruft für seine Kind-Objekte `context.getRootProvider().provide(childCtx)` auf — das ist immer diese Chain.

## Beispiel
```java
// Einstieg in eine provide-Operation:
DefaultInstanceCreationContext ctx = DefaultInstanceCreationContext.forProvide(
    MyProcessor.class, "myProcessor:instance1@context", chain, runtimeContext);
Object result = chain.provide(ctx); // → MyProcessor-Instanz
```
