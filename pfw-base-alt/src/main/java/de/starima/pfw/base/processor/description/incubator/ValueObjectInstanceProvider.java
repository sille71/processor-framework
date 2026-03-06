package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.api.IBeanProvider;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceCreationContext;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceProvider;
import de.starima.pfw.base.util.IdentifierUtils;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Erzeugt und extrahiert {@code @ValueObject}-annotierte Instanzen.
 *
 * <p>Ersetzt die bisherige Erzeugungslogik in:
 * <ul>
 *   <li>{@code ValueObjectValueFunction.transformValue()}</li>
 *   <li>{@code DefaultValueObjectProviderProcessor.getBeanForId()} + {@code initBean()}</li>
 * </ul>
 *
 * <p>Analog zum {@link ProcessorInstanceProvider}, aber ohne Context-Erstellung
 * (ValueObjects haben keinen eigenen IProcessorContext).
 *
 * <p>Identifier werden über {@code IdentifierUtils.getIdentifierForValueObject()} ermittelt.
 *
 * @see ProcessorInstanceProvider
 */
@Slf4j
@Getter
@Setter
@Order(20)
@Processor(
        description = """
                Erzeugt und extrahiert @ValueObject-annotierte Instanzen.
                Ersetzt ValueObjectValueFunction und DefaultValueObjectProviderProcessor.
                Rekursive Parameter-Auflösung über die InstanceProviderChain.""",
        categories = {"incubator", "instanceProvider"},
        tags = {"valueObject", "provide", "extract", "hydration"}
)
public class ValueObjectInstanceProvider extends AbstractProcessor implements IInstanceProvider {

    @ProcessorParameter(value = "defaultValueObjectProviderProcessor",
            description = "BeanProvider für @ValueObject-Instanzen.")
    private IBeanProvider beanProvider;

    @Override
    public boolean isResponsibleFor(IInstanceCreationContext context) {
        Class<?> rawType = resolveRawType(context);
        if (rawType == null) return false;
        // Nicht für Prozessoren (die sind beim ProcessorInstanceProvider)
        if (IProcessor.class.isAssignableFrom(rawType)) return false;
        // Zuständig für @ValueObject
        return ProcessorUtils.isConsideredValueObject(rawType);
    }

    // =========================================================================
    // provide() — Hydration
    // =========================================================================

    @Override
    public Object provide(IInstanceCreationContext context) {
        Object parameterValue = context.getParameterValue();

        // Null oder leer? → null
        if (parameterValue == null || "".equals(parameterValue.toString().trim())) {
            return null;
        }

        String fullBeanId = parameterValue.toString().trim();

        // 1. Bean-Lookup: prototypeId aus fullBeanId extrahieren
        String prototypeId = ProcessorUtils.getPrototypeId(fullBeanId);
        String identifier = ProcessorUtils.getIdentifier(fullBeanId);

        Object valueObject;
        try {
            valueObject = beanProvider.getBeanForId(prototypeId);
        } catch (Exception e) {
            log.warn("ValueObject-Bean '{}' nicht gefunden: {}", prototypeId, e.getMessage());
            return null;
        }

        if (valueObject == null) {
            log.debug("Kein Bean für prototypeId '{}' gefunden", prototypeId);
            return null;
        }

        // 2. Zirkularitätsprüfung (falls creationStack vorhanden)
        if (context.getCreationStack() != null) {
            if (context.isInCreationPath(fullBeanId)) {
                log.info("Zirkularität erkannt: '{}' ist bereits im CreationStack. Abbruch.",
                        fullBeanId);
                return null;
            }
            context.getCreationStack().add(fullBeanId);
        }

        try {
            // 3. Parameter rekursiv auflösen
            Map<String, Object> parameters = context.getRuntimeContext() != null
                    ? context.getRuntimeContext().getContextMergedBeanParameters(identifier)
                    : null;

            initializeParameters(valueObject, parameters, fullBeanId, context);

            log.debug("ValueObject '{}' erfolgreich erzeugt", fullBeanId);
            return valueObject;

        } finally {
            if (context.getCreationStack() != null) {
                context.getCreationStack().remove(fullBeanId);
            }
        }
    }

    // =========================================================================
    // extract() — Dehydration
    // =========================================================================

    @Override
    public Object extract(IInstanceCreationContext context) {
        Object valueObject = context.getObjectToResolve();
        if (valueObject == null) return null;

        // 1. Zykluserkennung
        if (context.isAlreadyVisited(valueObject)) {
            log.trace("Zyklus erkannt bei extract(): ValueObject '{}' bereits besucht",
                    valueObject.getClass().getSimpleName());
            String id = IdentifierUtils.getIdentifierForValueObject(valueObject);
            return id != null ? id : valueObject.toString();
        }
        context.markVisited(valueObject);

        // 2. Identifier ermitteln
        String identifier = IdentifierUtils.getIdentifierForValueObject(valueObject);

        // 3. Parameter extrahieren (rekursiv)
        Map<String, Object> parameters = new HashMap<>();

        for (Map.Entry<Field, ProcessorParameter> entry :
                ProcessorUtils.getAllAnnotatedParameterFields(valueObject.getClass()).entrySet()) {

            Field field = entry.getKey();
            ProcessorParameter annotation = entry.getValue();

            if (annotation.ignoreExtractParameter()) continue;

            try {
                field.setAccessible(true);
                Object childValue = field.get(valueObject);

                if (childValue == null) continue;

                DefaultInstanceCreationContext childCtx =
                        ((DefaultInstanceCreationContext) context)
                                .createChildForExtraction(field, childValue);

                // REKURSION über die Chain
                Object rawChildValue = context.getRootProvider().extract(childCtx);
                if (rawChildValue != null) {
                    parameters.put(ProcessorUtils.getParameterName(field), rawChildValue);
                }

            } catch (IllegalAccessException e) {
                log.error("Fehler beim Zugriff auf Feld '{}' in ValueObject '{}': {}",
                        field.getName(), valueObject.getClass().getSimpleName(), e.getMessage());
            }
        }

        // 4. In die Ergebnis-Map schreiben (falls Identifier vorhanden)
        if (identifier != null && !parameters.isEmpty() && context.getExtractionResult() != null) {
            context.getExtractionResult().put(identifier, parameters);
        }

        // 5. Identifier (oder toString-Fallback) als Referenz zurückgeben
        return identifier != null ? identifier : valueObject.toString();
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    /**
     * Initialisiert alle @ProcessorParameter-Felder eines ValueObjects.
     *
     * <p>Wertauflösung via {@link ProcessorUtils#getParameterValue(Object, Field, Map)}:
     * <ol>
     *   <li>Primärer Parametername (aus {@code annotation.name()} oder Feldname)</li>
     *   <li>Fallback: Name + {@code "Identifier"}-Suffix</li>
     *   <li>Fallback: {@code @Processor(defaultValues)} in der Klassenhierarchie</li>
     *   <li>Fallback: {@code annotation.value()}</li>
     * </ol>
     */
    private void initializeParameters(Object valueObject, Map<String, Object> parameters,
                                      String fullBeanId, IInstanceCreationContext context) {
        if (parameters == null) parameters = new HashMap<>();

        for (Map.Entry<Field, ProcessorParameter> entry :
                ProcessorUtils.getAllAnnotatedParameterFields(valueObject.getClass()).entrySet()) {

            Field field = entry.getKey();
            ProcessorParameter annotation = entry.getValue();

            if (annotation.ignoreInitialization()) continue;

            // Vollständige Wertauflösung inkl. "Identifier"-Suffix und @Processor(defaultValues)
            Object paramValue = ProcessorUtils.getParameterValue(valueObject, field, parameters);

            if (paramValue == null && !annotation.required()) continue;

            String paramName = ProcessorUtils.getParameterName(field);
            try {
                DefaultInstanceCreationContext childCtx =
                        new DefaultInstanceCreationContext(context);
                childCtx.setFieldToResolve(field);
                childCtx.setTypeToResolve(field.getGenericType());
                childCtx.setParameterValue(paramValue);
                childCtx.setProcessorParameter(annotation);

                Object value = context.getRootProvider().provide(childCtx);

                if (value != null) {
                    field.setAccessible(true);
                    field.set(valueObject, value);
                } else if (annotation.required()) {
                    log.error("Required parameter '{}' in ValueObject '{}' konnte nicht aufgelöst werden",
                            paramName, fullBeanId);
                }
            } catch (Exception e) {
                log.error("Fehler bei Parameter '{}' in ValueObject '{}': {}",
                        paramName, fullBeanId, e.getMessage(), e);
            }
        }
    }

    private Class<?> resolveRawType(IInstanceCreationContext context) {
        if (context.getTypeToResolve() instanceof Class<?> clazz) return clazz;
        if (context.getFieldToResolve() != null) return context.getFieldToResolve().getType();
        if (context.getObjectToResolve() != null) return context.getObjectToResolve().getClass();
        return null;
    }
}
