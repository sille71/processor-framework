# Scope-Modell, Provided und Parameter-Refresh

## Scope `provided` — deklarierte Abhängigkeit

```
prototype:     Immer neu erzeugt, nie registriert
context:       Im eigenen Kontext
parentcontext: In der Hierarchie sichtbar
instance:      Im Root, erzeugt wenn nötig (Lazy Singleton)
provided:      MUSS bereits existieren — KEINE Erzeugung
```

Der Unterschied zwischen `instance` und `provided` ist fundamental:

```
@instance:   "Ich brauche X. Falls es X noch nicht gibt, erzeuge es."
@provided:   "Ich brauche X. Es MUSS schon da sein."
```

> Biologisch: `@instance` = "Dieses Organ kann selbst Insulin produzieren."
> `@provided` = "Dieses Organ BRAUCHT Insulin aus dem Blutkreislauf."

Der Kernel stellt Infrastruktur-Prozessoren im Kontext bereit.
Services finden sie über `@provided`-Parameter. Keine explizite
Registrierung — die Kontext-Hierarchie IST die Registry.


## Parameter-Refresh — Patches statt Rebuild

### Problem mit dem aktuellen refreshParameters()

```
refreshParameters(beanParameterMap)
  → Komplettes Re-Init ALLER Parameter
  → Sub-Prozessoren werden NEU ERZEUGT
  → Kein Diff, kein Rollback
```

### Lösung: Patch-basierter Refresh

Die Edit-Session-Patches können direkt als Refresh-Operationen
auf lebendigen Prozessoren verwendet werden:

```
Skalar-Refresh:    field.set(processor, vf.transformValue(newRawValue))
                   → schnell, keine Seiteneffekte

Struktur-Refresh:  old.processorOnDestroy() + instanceProviderChain.provide(newConfig)
                   → neuer Sub-Baum, alter wird bereinigt
```

### Fehlender Lifecycle-Hook

```java
default void processorOnDestroy() {}  // Cleanup bei Struktur-Refresh
default void processorOnParameterChanged(String name, Object old, Object new) {}
```


## Konfigurierter vs. effektiver Zustand

```
extract(CONFIGURED):  Die persistierbare beanParameterMap
                      (nur was ohne zeitliche Abhängigkeiten erzeugbar ist)

extract(EFFECTIVE):   Der aktuelle Zustand
                      (inkl. transienter Laufzeit-Registrierungen)
```

Relevant für: Service registriert sich zur Laufzeit beim Gateway.
Die Registrierung ist transient — bei Neustart registriert er sich erneut.
Die persistierte beanParameterMap enthält keine RunLevel-Abhängigkeiten.
