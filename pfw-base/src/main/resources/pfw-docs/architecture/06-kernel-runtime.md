# Kernel, Runtime und Communication Gateway

## RunLevel-Modell

```
RunLevel 0:   Bootstrap
              BeanProvider, KernelContext, KernelConstructionManager
              Nur Blatt-Prozessoren (keine Descriptoren)

RunLevel 0.5: Framework Adoption
              Spring-Beans → Framework-Mitglieder im RootContext
              FrameworkAdoptionPhaseProcessor

RunLevel 1:   Incubator
              FrameworkIncubator, InstanceProviderChain, Descriptoren
              Ab hier: vollständige provide/extract-Fähigkeit

RunLevel 2:   Communication Gateway
              IRequestGatewayProcessor mit Dispatcher-Chain
              SecurityInterceptor (optional)
              REST-Controller (EINE Stelle, nicht 3x kopiert)

RunLevel 3:   Application
              Fachlicher Microservice wird vom Incubator erzeugt
              Service findet Gateway via @provided-Parameter
```


## Der fachliche Microservice

Ein Microservice ist ein ganz normaler Prozessor:

```java
@Processor(description = "CSV-Abgleichsservice")
public class CsvReconService extends AbstractProcessor {
    @ProcessorParameter(contextProvider = true)
    private IRuntimeContextProviderProcessor contextProvider;
    
    @ProcessorParameter(description = "Quell-Prozessor")
    private IProcessor sourceProcessor;
    
    @ProcessorParameter(description = "Ziel-Prozessor")
    private IProcessor targetProcessor;
    
    @ProcessorParameter(value = "requestGateway@provided",
        description = "Das Communication Gateway — muss vom Kernel bereitgestellt sein")
    private IRequestGatewayProcessor gateway;
}
```

Kein @PostConstruct, kein init(null), keine Sonderbehandlung.
Der Kernel erzeugt ihn über den Incubator (RunLevel 3).
Das Gateway findet er via `@provided` — der Kernel hat es in
RunLevel 2 bereitgestellt.


## Communication Gateway

```java
@Processor(description = "Zentrale Kommunikationsschnittstelle")
public interface IRequestGatewayProcessor extends IProcessor {
    Object processRequest(Object request);
    IRequestDispatcherProcessor getRequestDispatcher();
    IResponseDispatcherProcessor getResponseDispatcher();
}
```

Der RequestDispatcher ist eine Chain:

```
RequestDispatcherChain
  ├── JsonRequestDispatcher         → beanParameterMap-Requests
  ├── AssetRequestDispatcher        → Doku/Assets
  ├── MultipartRequestDispatcher    → Upload
  └── DescriptorRequestDispatcher   → Descriptor-Inspektion (NEU)
```

Jeder Dispatcher: `isResponsibleForRequest()` + `dispatchRequest()`.
Alles Prozessoren, alles über `@ProcessorParameter` konfiguriert,
alles inspizierbar.


## AbstractInstanceProcessorRuntime — wird obsolet

```
VORHER: Context + Parameter + Kommunikation + @PostConstruct
NACHHER: Ein normaler Prozessor. Context kommt vom ContextProviderResolver.
         Parameter vom InstanceProvider. Kommunikation vom Gateway.
         Start über den Incubator.
```

Optional als Convenience-Layer mit Lifecycle-Hooks:

```java
public interface IRuntimeServiceProcessor extends IProcessor {
    default void onServiceStart() {}
    default void onServiceStop() {}
    default void onHealthCheck() {}
}
```
