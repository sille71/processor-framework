# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build all modules
mvn clean install

# Build skipping tests
mvn clean install -DskipTests

# Run tests for the entire project
mvn test

# Run tests for a specific module
mvn test -pl pfw-base-alt

# Run a single test class
mvn test -pl pfw-base-alt -Dtest=MyTestClass

# Run a single test method
mvn test -pl pfw-base-alt -Dtest=MyTestClass#myMethod
```

The Surefire plugin is configured with `--add-opens java.base/java.lang=ALL-UNNAMED` globally (no need to add it manually). Spring Boot requires `spring.main.allow-bean-definition-overriding=true` (set in `application.properties`).

## Module Overview

| Module | Role |
|--------|------|
| `pfw-base-alt` | **Primary active module.** Contains all concrete implementations (~277 classes). This is the source being refactored into the clean modules below. |
| `pfw-base` | New clean Kern-API (interfaces, annotations, `IProcessor`, `ProcessorScope`) — minimal, no Spring Runtime dep. |
| `pfw-descriptor` | Descriptor & ValueFunction system (self-description) |
| `pfw-runtime` | Runtime: `ProcessorProvider`, `AbstractProcessor`, `Kernel`, `ParameterProvider` |
| `pfw-test` | Declarative testing: `TestCaseRunner`, Sandbox |
| `pfw-logging` | Structured LogBook logging as processor chains |
| `pfw-doc` | Markdown renderer + PDF export (flexmark + openhtmltopdf) |
| `pfw-spring-boot-starter` | Single-dependency Spring Boot auto-configuration (transitively includes runtime, logging, doc) |
| `pfw-sample` | Reference application demonstrating minimal PFW setup |

**Note:** `pfw-base-alt` is the source of truth for business logic during the ongoing refactoring. New modules (`pfw-base`, `pfw-descriptor`, `pfw-runtime`) are being extracted from it.

## Architecture: Everything is a Processor

### Core Abstractions

**`IProcessor`** — the universal unit. Every component is a Processor. Key responsibilities:
- `init(IProcessorContext ctx)` — lifecycle initialization
- `refreshParameters(Map<String,Map<String,Object>>)` — hot-reload of config
- `generatePrototypeProcessorDescriptor()` / `generateProcessorDescriptorInstance(LoadStrategy)` — self-description
- `createProcessor(Class<T>, identifier, type, parameterMaps)` — dynamic child creation

**`AbstractProcessor`** — base implementation. Subclass this for all processors. The `init()` flow is:
1. `initContextProvider(ctx)` — builds context hierarchy, loads default parameter JSON
2. `initProcessorDescriptor()` — creates self-describing metadata from `@ProcessorParameter` annotations
3. `initParameters()` — populates fields from the active context's parameter map
4. `processorOnInit()` — hook for subclass customization

### Annotations

**`@Processor`** — marks a class as a Spring `@Component` with `SCOPE_PROTOTYPE`. Also carries metadata (description, categories, tags, defaultBeanParameterMapFileName) used for self-description and UI generation.

**`@ProcessorParameter`** — placed on fields to declare configurable parameters. Key attributes:
- `value`: default bean ID to inject
- `required`: fail fast if missing
- `ignoreInitialization` / `ignoreExtractParameter`: skip during init or export
- `aliases`: backward-compatible parameter name aliases
- `valueFunctionPrototypeIdentifier`: transform the raw value before injection

### Configuration: Bean Parameter Maps

Configuration is passed as nested maps throughout the framework:
```
Map<String, Map<String, Object>>
  └─ key: beanId (e.g. "myProcessor:instance1@parentcontext")
     └─ value: Map<parameterName, parameterValue>
```

Default parameter values are loaded from classpath JSON files named after the processor class (via `defaultBeanParameterMapFileName` in `@Processor`).

### Context Hierarchy

`IProcessorContext` forms a parent→child tree. Parameters are merged upward: child overrides parent. Three logical levels: **global** → **cluster** → **local**.

Key context operations:
- `getContextMergedBeanParameterMap()` — full merged view including parents
- `getContextMergedBeanIdForType(type)` — resolves a type alias to a concrete beanId
- `getProcessorFromHierarchy(beanId)` — looks up registered processor by scope

### Processor Scope

**`ProcessorScope`** controls processor lifetime and where it is registered:

| Scope | BeanId Suffix | Behavior |
|-------|--------------|----------|
| `prototype` | (none or `@prototype`) | New instance every call, not registered |
| `context` | `@context` | Registered in current context only |
| `parentcontext` | `@parentcontext` | Inherited by child contexts |
| `instance` | `@instance` | Global singleton in root context |

**Full beanId format:** `prototypeId[:identifier][@scope]`
Example: `myProcessor:myInstance@parentcontext`

`prototypeId` is derived from the class name by lowercasing the first letter (e.g. `MyProcessor` → `myProcessor`).

### Composition Pattern

Processors compose by injecting lists of other processors via `@ProcessorParameter`:
```java
@ProcessorParameter
private List<IParameterProviderProcessor> parameterProviderProcessors;
```
Chain classes (e.g. `DefaultParameterProviderChainProcessor`, `AttributeProviderChain`) delegate to their list members and merge or route results.

### Entry Point for Services

`AbstractInstanceProcessorRuntime` is the Spring Boot `@Service` + `@RestController` base for deployable instances. It is `SCOPE_SINGLETON`, bootstraps itself via `@PostConstruct`, exposes REST endpoints (`/processBeanParameterMapRequest`, `/processMultipartRequest`), and manages its own root context. Concrete service implementations extend this class and configure a `requestDispatcherProcessor` and `responseDispatcherProcessor`.

### Parameter Providers

`IParameterProviderProcessor` abstractions for loading `Map<String,Map<String,Object>>` from various sources:
- `ClassPathParameterProvider` — JSON on classpath
- `FileSystemParameterProviderProcessor` — JSON from filesystem
- `PropertiesParameterProviderProcessor` — `.properties` files
- `StringConfigurationParameterProvider` — inline string config
- `DefaultParameterProviderChainProcessor` — merges multiple providers

### Descriptor & LoadStrategy

Descriptors are self-generated metadata objects built from `@ProcessorParameter` annotations. Used for initialization, UI blueprint generation, and documentation.

`LoadStrategy` controls recursion depth when generating descriptors:
- `DEEP` — full recursive expansion
- `SHALLOW` — current level complete, children are lazy stubs (default for UI)
- `LAZY` — minimal stub, no recursion
