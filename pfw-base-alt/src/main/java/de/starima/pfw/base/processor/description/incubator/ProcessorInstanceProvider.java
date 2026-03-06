package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.domain.ProcessorScope;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.api.IBeanProvider;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.*;
import de.starima.pfw.base.processor.context.domain.DefaultContextCreationContext;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceCreationContext;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceProvider;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Erzeugt und extrahiert {@link IProcessor}-Instanzen.
 *
 * <p>Dies ist der zentrale Provider für Prozessoren und ersetzt die bisherige
 * Self-Managed-Initialisierung in {@code AbstractProcessor.init(ctx)} sowie
 * die Erzeugungslogik in {@code DefaultProcessorProvider.getProcessorForBeanIdWithType()}
 * und {@code ProcessorValueFunction.transformValue()}.
 *
 * <p><b>provide() — Hydration:</b>
 * <ol>
 *   <li>Scope-Check: Prozessor bereits im Scope vorhanden?</li>
 *   <li>Bean-Lookup: Spring-Bean über BeanProvider holen</li>
 *   <li>Identifier + Scope setzen</li>
 *   <li>Zirkularitätsprüfung: fullBeanId im CreationStack?</li>
 *   <li>Context erstellen: via ContextProviderResolver → RuntimeContextProviderProcessor</li>
 *   <li>Context injizieren via setRuntimeContext()</li>
 *   <li>Pro @ProcessorParameter: rekursiv über InstanceProviderChain auflösen</li>
 *   <li>Scope-Registrierung</li>
 *   <li>processorOnInit()</li>
 * </ol>
 *
 * <p><b>extract() — Dehydration:</b>
 * <ol>
 *   <li>Zykluserkennung: Objekt im VisitedSet?</li>
 *   <li>fullBeanId ermitteln</li>
 *   <li>Pro @ProcessorParameter: rekursiv über InstanceProviderChain extrahieren</li>
 *   <li>Parameter-Map ins ExtractionResult schreiben</li>
 *   <li>fullBeanId als Referenz zurückgeben</li>
 * </ol>
 *
 * @see IInstanceProvider
 * @see IInstanceCreationContext
 */
@Slf4j
@Getter
@Setter
@Order(10) // Vor ValueObject (20), Scalar (30), Collection (25)
@Processor(
        description = """
                Erzeugt und extrahiert IProcessor-Instanzen.
                Ersetzt die bisherige Self-Managed-Initialisierung in AbstractProcessor.init(ctx).
                Orchestriert Context-Erstellung via ContextProviderResolver,
                rekursive Parameter-Auflösung via InstanceProviderChain und
                Zirkularitätserkennung via CreationStack.""",
        categories = {"incubator", "instanceProvider"},
        tags = {"processor", "provide", "extract", "hydration", "dehydration", "context"}
)
public class ProcessorInstanceProvider extends AbstractProcessor implements IInstanceProvider {

    @ProcessorParameter(description = "BeanProvider für Spring-Lookup (ApplicationContext.getBean). " +
            "Kapselt den Zugriff auf die Bean-Registry.")
    private IBeanProvider beanProvider;

    @ProcessorParameter(description = "Resolver-Chain für die Context-Erstellung. " +
            "Findet den zuständigen IRuntimeContextProviderProcessor.")
    private IContextProviderResolver contextProviderResolver;

    // =========================================================================
    // isResponsibleFor
    // =========================================================================

    @Override
    public boolean isResponsibleFor(IInstanceCreationContext context) {
        // provide: typeToResolve ist ein IProcessor
        if (context.getTypeToResolve() instanceof Class<?> clazz
                && IProcessor.class.isAssignableFrom(clazz)) {
            return true;
        }
        // extract: objectToResolve ist ein IProcessor
        if (context.getObjectToResolve() instanceof IProcessor) {
            return true;
        }
        // provide mit String-Input: Feld ist vom Typ IProcessor
        if (context.getParameterValue() instanceof String
                && context.getFieldToResolve() != null
                && IProcessor.class.isAssignableFrom(context.getFieldToResolve().getType())) {
            return true;
        }
        return false;
    }

    // =========================================================================
    // provide() — Hydration
    // =========================================================================

    @Override
    public Object provide(IInstanceCreationContext context) {
        Object parameterValue = context.getParameterValue();

        // Bereits ein Prozessor? → Durchreichen (analog ProcessorValueFunction)
        if (parameterValue instanceof IProcessor) {
            return parameterValue;
        }

        // Null oder leer? → null
        if (parameterValue == null || "".equals(parameterValue.toString().trim())) {
            return null;
        }

        String fullBeanId = parameterValue.toString().trim();
        IProcessorContext parentCtx = context.getRuntimeContext();

        // 1. Scope-Check: bereits vorhanden?
        IProcessor existing = ProcessorUtils.getProcessorFromScope(fullBeanId, parentCtx);
        if (existing != null) {
            log.debug("Prozessor '{}' aus Scope gefunden", existing.getFullBeanId());
            return existing;
        }

        // 2. Bean-Lookup
        String prototypeId = ProcessorUtils.getPrototypeId(fullBeanId);
        String identifier = ProcessorUtils.getIdentifier(fullBeanId);
        ProcessorScope scope = ProcessorUtils.getProcessorScope(fullBeanId);

        IProcessor processor;
        try {
            processor = (IProcessor) beanProvider.getBeanForId(prototypeId);
        } catch (Exception e) {
            log.warn("Bean '{}' nicht gefunden: {}", prototypeId, e.getMessage());
            return null;
        }

        if (processor == null) {
            log.debug("Kein Bean für prototypeId '{}' gefunden", prototypeId);
            return null;
        }

        // 3. Identifier + Scope setzen
        processor.setIdentifier(identifier);
        processor.setScope(scope);

        // 4. Zirkularitätsprüfung
        String resolvedFullBeanId = processor.getFullBeanId();
        if (context.isInCreationPath(resolvedFullBeanId)) {
            log.info("Zirkularität erkannt: '{}' ist bereits im CreationStack. Abbruch.",
                    resolvedFullBeanId);
            return null;
        }

        // 5. CreationStack PUSH
        context.getCreationStack().add(resolvedFullBeanId);

        try {
            // 6. Context erstellen
            IProcessorContext newCtx = createContextForProcessor(processor, context);

            // 7. Context setzen und Scope-Registrierung
            processor.setRuntimeContext(newCtx);
            ProcessorUtils.registerProcessorInScope(processor, newCtx);

            // 8. Parameter rekursiv auflösen
            initializeParameters(processor, newCtx, context);

            // 9. processorOnInit
            processor.processorOnInit();

            log.debug("Prozessor '{}' erfolgreich erzeugt und initialisiert",
                    processor.getFullBeanId());

            return processor;

        } catch (Exception e) {
            log.error("Fehler bei der Erzeugung von Prozessor '{}': {}",
                    resolvedFullBeanId, e.getMessage(), e);
            return null;
        } finally {
            // CreationStack POP
            context.getCreationStack().remove(resolvedFullBeanId);
        }
    }

    // =========================================================================
    // extract() — Dehydration
    // =========================================================================

    @Override
    public Object extract(IInstanceCreationContext context) {
        IProcessor processor = (IProcessor) context.getObjectToResolve();
        if (processor == null) return null;

        // 1. Zykluserkennung
        if (context.isAlreadyVisited(processor)) {
            log.trace("Zyklus erkannt bei extract(): '{}' bereits besucht",
                    processor.getFullBeanId());
            return processor.getFullBeanId(); // Nur Referenz, keine Rekursion
        }
        context.markVisited(processor);

        // 2. Identifier
        String fullBeanId = processor.getFullBeanId();
        String identifier = ProcessorUtils.getIdentifier(fullBeanId);

        // 3. Parameter extrahieren (rekursiv)
        Map<String, Object> parameters = new HashMap<>();

        for (Map.Entry<Field, ProcessorParameter> entry :
                ProcessorUtils.getAllAnnotatedParameterFields(processor.getClass()).entrySet()) {

            Field field = entry.getKey();
            ProcessorParameter annotation = entry.getValue();

            if (annotation.ignoreExtractParameter()) continue;

            try {
                field.setAccessible(true);
                Object childValue = field.get(processor);

                if (childValue == null) continue;

                // Kind-Context aufbauen
                DefaultInstanceCreationContext childCtx =
                        ((DefaultInstanceCreationContext) context)
                                .createChildForExtraction(field, childValue);

                // REKURSION über die Chain
                Object rawChildValue = context.getRootProvider().extract(childCtx);
                if (rawChildValue != null) {
                    parameters.put(ProcessorUtils.getParameterName(field), rawChildValue);
                }

            } catch (IllegalAccessException e) {
                log.error("Fehler beim Zugriff auf Feld '{}' in '{}': {}",
                        field.getName(), processor.getFullBeanId(), e.getMessage());
            }
        }

        // 4. In die Ergebnis-Map schreiben
        if (!parameters.isEmpty() && context.getExtractionResult() != null) {
            context.getExtractionResult().put(identifier, parameters);
        }

        // 5. Referenz an den Eltern zurückgeben
        return fullBeanId;
    }

    // =========================================================================
    // Interne Hilfsmethoden
    // =========================================================================

    /**
     * Erstellt den ProcessorContext für einen neuen Prozessor.
     * Delegiert an den ContextProviderResolver.
     */
    private IProcessorContext createContextForProcessor(
            IProcessor processor, IInstanceCreationContext instanceContext) {

        // ContextCreationContext aufbauen (Brücken-Konstruktor)
        DefaultContextCreationContext ctxCreation = new DefaultContextCreationContext(instanceContext);
        ctxCreation.setTypeToResolve(processor.getClass());
        ctxCreation.setObjectToResolve(processor);

        // Zuständigen ContextProvider finden
        IRuntimeContextProviderProcessor provider =
                contextProviderResolver.resolve(ctxCreation);

        // Context erstellen
        if (provider != null) {
            return provider.createContext(ctxCreation);
        }

        // Fallback: Parent-Context verwenden
        log.warn("Kein ContextProvider für '{}' gefunden, verwende Parent-Context",
                processor.getFullBeanId());
        return instanceContext.getRuntimeContext();
    }

    /**
     * Initialisiert alle @ProcessorParameter-Felder eines Prozessors.
     * Iteriert über die annotierten Felder und ruft rekursiv die InstanceProviderChain auf.
     *
     * <p>Wertauflösung via {@link ProcessorUtils#getParameterValue(Object, Field, Map)}:
     * <ol>
     *   <li>Primärer Parametername (aus {@code annotation.name()} oder Feldname)</li>
     *   <li>Fallback: Name + {@code "Identifier"}-Suffix (Konvention für beanId-Parameter)</li>
     *   <li>Fallback: {@code @Processor(defaultValues)} in der Klassenhierarchie
     *       (erlaubt Subklassen, Defaults der Superklasse zu überschreiben)</li>
     *   <li>Fallback: {@code annotation.value()} der @ProcessorParameter-Annotation</li>
     * </ol>
     */
    private void initializeParameters(IProcessor processor, IProcessorContext processorCtx,
                                      IInstanceCreationContext parentContext) {

        Map<String, Object> parameters =
                processorCtx.getContextMergedBeanParameters(processor.getIdentifier());

        if (parameters == null) parameters = new HashMap<>();

        for (Map.Entry<Field, ProcessorParameter> entry :
                ProcessorUtils.getAllAnnotatedParameterFields(processor.getClass()).entrySet()) {

            Field field = entry.getKey();
            ProcessorParameter annotation = entry.getValue();

            // ignoreInitialization: contextProvider und processorDescriptor werden separat behandelt
            if (annotation.ignoreInitialization()) continue;

            // Vollständige Wertauflösung inkl. "Identifier"-Suffix und @Processor(defaultValues)
            Object paramValue = ProcessorUtils.getParameterValue(processor, field, parameters);

            if (paramValue == null && !annotation.required()) continue;

            String paramName = ProcessorUtils.getParameterName(field);

            try {
                // Kind-Context aufbauen
                DefaultInstanceCreationContext childCtx =
                        new DefaultInstanceCreationContext(parentContext);
                childCtx.setFieldToResolve(field);
                childCtx.setTypeToResolve(field.getGenericType());
                childCtx.setParameterValue(paramValue);
                childCtx.setProcessorParameter(annotation);
                childCtx.setRuntimeContext(processorCtx); // WICHTIG: der neue Context!

                // REKURSION über die Chain
                Object value = parentContext.getRootProvider().provide(childCtx);

                // Injection
                if (value != null) {
                    field.setAccessible(true);
                    field.set(processor, value);
                } else if (annotation.required()) {
                    log.error("Required parameter '{}' in '{}' konnte nicht aufgelöst werden",
                            paramName, processor.getFullBeanId());
                }

            } catch (Exception e) {
                log.error("Fehler bei Parameter '{}' in '{}': {}",
                        paramName, processor.getFullBeanId(), e.getMessage(), e);
            }
        }
    }

}
