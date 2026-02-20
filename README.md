# Starima Processor Framework (PFW)

Modellgetriebene Architektur für rekonfigurierbare Datenprozesse.

> **Everything is a Processor.**

## Module

| Modul | Beschreibung |
|-------|-------------|
| `pfw-base` | Kern-API: IProcessor, Annotationen, Context-Interfaces |
| `pfw-descriptor` | Selbstbeschreibung: Deskriptoren, ValueFunctions, TypeRef, Config |
| `pfw-runtime` | Laufzeit: ProcessorProvider, AbstractProcessor, Kernel, ParameterProvider |
| `pfw-test` | Deklaratives Testen: TestCaseRunner, Sandbox |
| `pfw-logging` | LogBook-System: Strukturiertes Logging als Prozessorkette |
| `pfw-doc` | Dokumentation: Markdown-Renderer, PDF-Export |
| `pfw-spring-boot-starter` | Auto-Configuration fuer Spring Boot |
| `pfw-sample` | Beispiel-Anwendung |

## Build

```bash
mvn clean install
```

## Architektur-Prinzipien

1. **Everything is a Processor** - Jede Funktion ist ein standardisierter Prozessor
2. **The Class is the Schema** - Die Java-Klasse definiert das Konfigurations-Schema
3. **Self-Describing System** - Automatische Dokumentation aus Metadaten
4. **Declarative Assembly** - Prozesse werden deklariert, nicht programmiert
5. **Dynamic Context Inheritance** - Globale/Cluster/lokale Ebenen verschmelzen dynamisch
6. **Runtime Adaptability** - Prozesse waehrend der Laufzeit aenderbar
