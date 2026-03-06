package de.starima.pfw.base.processor.context;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.*;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standard-Resolver: Findet den zuständigen {@link IRuntimeContextProviderProcessor}
 * für einen Prozessor, basierend auf dessen Konfiguration im Parent-Kontext.
 *
 * <h3>Annotation-basierte Suche</h3>
 * <p>Statt nach einem fest codierten Parameternamen {@code "contextProviderProcessor"}
 * zu suchen, wird die Prozessorklasse nach einem Feld mit
 * {@code @ProcessorParameter(contextProvider = true)} inspiziert.
 * Der Feldname (oder {@code @ProcessorParameter.name()}) wird dann als Schlüssel
 * in der beanParameterMap verwendet.
 *
 * <p>Dadurch kann jeder Prozessor seinen ContextProvider-Parameter frei benennen.
 * Das Framework findet ihn über die semantische Markierung, nicht über den Namen.
 *
 * <h3>Auflösungsreihenfolge:</h3>
 * <ol>
 *   <li><b>Explizite Konfiguration:</b> Parameter des Prozessors aus dem Parent-Kontext
 *       (via {@code getContextMergedBeanParameters(identifier)}), Schlüssel ist der
 *       annotierte Feldname.</li>
 *   <li><b>Default beanParameterMap:</b> Klassenspezifische Defaults
 *       (via {@code ProcessorUtils.loadProcessorDefaults(Class)}).</li>
 *   <li><b>Fallback:</b> ContextProvider des Parent-Kontextes
 *       ({@code parentCtx.getContextProviderProcessor()}).</li>
 * </ol>
 */
@Slf4j
@Getter
@Setter
@Processor(description = "Löst den zuständigen ContextProvider für einen Prozessor auf. " +
        "Sucht via @ProcessorParameter(contextProvider=true) statt über fest codierten Namen.")
public class DefaultContextProviderResolver extends AbstractProcessor implements IContextProviderResolver {

    /**
     * Cache: Klasse → Info über das ContextProvider-Feld.
     * Introspection einmal pro Klasse, danach aus dem Cache.
     */
    private static final Map<Class<?>, ContextProviderFieldInfo> FIELD_INFO_CACHE = new ConcurrentHashMap<>();

    /**
     * Sentinel-Objekt für "kein Feld gefunden" im Cache.
     * Unterscheidet von null (= noch nicht gesucht).
     */
    private static final ContextProviderFieldInfo NO_FIELD = new ContextProviderFieldInfo("", "", void.class);

    // -------------------------------------------------------------------------
    // IContextProviderResolver
    // -------------------------------------------------------------------------

    @Override
    public boolean isResponsibleFor(IContextCreationContext context) {
        return context.getTypeToResolve() instanceof Class<?> clazz
                && IProcessor.class.isAssignableFrom(clazz)
                && context.getRuntimeContext() != null;
    }

    @Override
    public IRuntimeContextProviderProcessor resolve(IContextCreationContext context) {
        IProcessorContext parentCtx = context.getRuntimeContext();
        IProcessor processorInstance = asProcessor(context.getObjectToResolve());
        Class<? extends IProcessor> processorClass = resolveProcessorClass(context);

        // 0. Welches Feld ist als contextProvider markiert?
        ContextProviderFieldInfo fieldInfo = resolveFieldInfo(processorClass);
        if (fieldInfo == null) {
            log.debug("Kein @ProcessorParameter(contextProvider=true) in {} — verwende Parent-ContextProvider",
                    processorClass != null ? processorClass.getSimpleName() : "?");
            return parentCtx.getContextProviderProcessor();
        }

        String parameterName = fieldInfo.parameterName();

        // 1. Parameter des Prozessors aus dem Parent-Kontext laden
        String identifier = processorInstance != null ? processorInstance.getIdentifier() : null;
        Map<String, Object> configuredParams = identifier != null
                ? parentCtx.getContextMergedBeanParameters(identifier)
                : null;

        // 2. Suche nach explizit konfiguriertem ContextProvider
        IRuntimeContextProviderProcessor provider = lookupContextProvider(configuredParams, parameterName, parentCtx);

        if (provider != null) {
            log.debug("Explizit konfigurierter ContextProvider für '{}' (Parameter '{}'): {}",
                    identifier, parameterName, provider.getIdentifier());
            return provider;
        }

        // 3. Fallback: Default-beanParameterMap der Prozessorklasse prüfen
        if (processorClass != null) {
            Map<String, Map<String, Object>> defaults = ProcessorUtils.loadProcessorDefaults(processorClass);
            if (defaults != null) {
                String prototypeId = processorInstance != null
                        ? processorInstance.getProtoTypeIdentifier()
                        : processorClass.getSimpleName();
                provider = lookupContextProvider(defaults.get(prototypeId), parameterName, parentCtx);

                if (provider != null) {
                    log.debug("ContextProvider aus Default-beanParameterMap für '{}' (Parameter '{}'): {}",
                            identifier, parameterName, provider.getIdentifier());
                    return provider;
                }
            }
        }

        // 4. Letzter Fallback: ContextProvider des Parent-Kontextes
        IRuntimeContextProviderProcessor parentProvider = parentCtx.getContextProviderProcessor();
        log.debug("Kein dedizierter ContextProvider für '{}', verwende Parent-ContextProvider: {}",
                identifier, parentProvider != null ? parentProvider.getIdentifier() : "null");
        return parentProvider;
    }

    // -------------------------------------------------------------------------
    // Annotation-basierte Feld-Introspection
    // -------------------------------------------------------------------------

    /**
     * Findet das Feld mit {@code @ProcessorParameter(contextProvider = true)}
     * in der Klassen-Hierarchie des Prozessors.
     *
     * <p>Das Ergebnis wird gecacht — die Introspection erfolgt nur einmal pro Klasse.
     *
     * @return Info über das ContextProvider-Feld, oder {@code null} wenn keines markiert ist.
     */
    private ContextProviderFieldInfo resolveFieldInfo(Class<? extends IProcessor> processorClass) {
        if (processorClass == null) return null;

        ContextProviderFieldInfo cached = FIELD_INFO_CACHE.computeIfAbsent(processorClass, clazz ->
                ProcessorUtils.getAllAnnotatedParameterFields(clazz).entrySet().stream()
                        .filter(e -> e.getValue().contextProvider())
                        .findFirst()
                        .map(e -> {
                            Field field = e.getKey();
                            ProcessorParameter annotation = e.getValue();
                            String paramName = !annotation.name().isEmpty()
                                    ? annotation.name()
                                    : field.getName();
                            log.debug("ContextProvider-Feld gefunden in {}: '{}' (Parametername: '{}')",
                                    clazz.getSimpleName(), field.getName(), paramName);
                            return new ContextProviderFieldInfo(paramName, field.getName(), field.getType());
                        })
                        .orElse(NO_FIELD)
        );

        return cached == NO_FIELD ? null : cached;
    }

    // -------------------------------------------------------------------------
    // Lookup im Parameter-Map
    // -------------------------------------------------------------------------

    /**
     * Sucht den konfigurierten ContextProvider in der Parameter-Map.
     *
     * <p>Der Schlüssel ist der annotierte Parametername (Feldname oder expliziter name()).
     * Zusätzlich wird das Legacy-Suffix "Identifier" geprüft (Abwärtskompatibilität).
     */
    private IRuntimeContextProviderProcessor lookupContextProvider(
            Map<String, Object> parameters, String parameterName, IProcessorContext ctx) {

        if (parameters == null || parameterName == null) return null;

        // Primär: über den annotierten Parameternamen
        Object cpParam = parameters.get(parameterName);

        // Legacy-Fallback: Feld + "Identifier" Suffix
        if (cpParam == null) {
            cpParam = parameters.get(parameterName + "Identifier");
        }

        if (cpParam == null) return null;

        // Den konfigurierten ContextProvider über den ProcessorProvider erzeugen
        return ctx.getProcessorProvider().getProcessorForBeanId(
                IRuntimeContextProviderProcessor.class,
                cpParam.toString(),
                ctx,
                null
        );
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Class<? extends IProcessor> resolveProcessorClass(IContextCreationContext context) {
        if (context.getTypeToResolve() instanceof Class<?> clazz
                && IProcessor.class.isAssignableFrom(clazz)) {
            return (Class<? extends IProcessor>) clazz;
        }
        // processorInstance bereits aufgelöst — einfach .getClass() nutzen
        IProcessor proc = asProcessor(context.getObjectToResolve());
        return proc != null ? proc.getClass() : null;
    }

    private IProcessor asProcessor(Object obj) {
        return obj instanceof IProcessor proc ? proc : null;
    }

    // -------------------------------------------------------------------------
    // Feld-Info Record
    // -------------------------------------------------------------------------

    /**
     * Gecachte Information über das ContextProvider-Feld einer Prozessorklasse.
     *
     * @param parameterName  Der Schlüssel in der beanParameterMap (aus name() oder Feldname)
     * @param fieldName      Der Java-Feldname (für Reflection-Zugriff)
     * @param fieldType      Der Typ des Feldes (zur Validierung)
     */
    record ContextProviderFieldInfo(String parameterName, String fieldName, Class<?> fieldType) {}
}
