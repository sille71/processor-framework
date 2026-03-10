package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.domain.DefaultTaskContext;
import de.starima.pfw.base.processor.description.api.IProcessorDescriptor;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfigProvider;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceCreationContext;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceProvider;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Standard-Implementierung von {@link IInstanceCreationContext}.
 *
 * <p>Unterstützt sowohl Hydration (provide) als auch Dehydration (extract)
 * durch separate Felder für beide Richtungen.
 *
 * <p>Der Konstruktor mit Parent-Context übernimmt:
 * <ul>
 *   <li>rootProvider und descriptorConfigRootProvider (Chain-Referenzen)</li>
 *   <li>creationStack und visitedSet (geteilt über die gesamte Rekursion)</li>
 *   <li>extractionResult (geteilt für die Ergebnis-Map)</li>
 *   <li>runtimeContext (geerbt vom TaskContext)</li>
 * </ul>
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DefaultInstanceCreationContext extends DefaultTaskContext implements IInstanceCreationContext {

    // --- Bestehende Felder ---
    private Type typeToResolve;
    private Object objectToResolve;
    private Field fieldToResolve;
    private ProcessorParameter processorParameter;
    private IInstanceProvider rootProvider;
    private IDescriptorConfigProvider descriptorConfigRootProvider;

    // --- NEU: Für provide() ---
    private Object parameterValue;
    private Set<String> creationStack;

    // --- NEU: Für extract() ---
    private Set<Object> visitedSet;
    private Map<String, Map<String, Object>> extractionResult;

    /**
     * Leerer Konstruktor für Builder-Pattern oder manuelles Setup.
     */
    public DefaultInstanceCreationContext() {
        // creationStack und visitedSet werden lazy initialisiert
    }

    /**
     * Kind-Konstruktor: Übernimmt geteilte Ressourcen vom Eltern-Kontext.
     *
     * <p>CreationStack, VisitedSet und ExtractionResult werden GETEILT (nicht kopiert),
     * damit die Zirkularitätserkennung über die gesamte Rekursionstiefe funktioniert.
     */
    public DefaultInstanceCreationContext(IInstanceCreationContext parent) {
        super(parent); // erbt runtimeContext, parentContext, ownBeanParameterMap
        this.rootProvider = parent.getRootProvider();
        this.descriptorConfigRootProvider = parent.getDescriptorConfigRootProvider();

        // Geteilte Ressourcen — NICHT kopieren, sondern REFERENZ übernehmen
        this.creationStack = parent.getCreationStack();
        this.visitedSet = parent.getVisitedSet();
        this.extractionResult = parent.getExtractionResult();
    }

    // =========================================================================
    // Factory-Methoden für typische Anwendungsfälle
    // =========================================================================

    /**
     * Erstellt einen Root-Kontext für eine provide()-Operation.
     *
     * <p>Initialisiert den CreationStack als leeres LinkedHashSet
     * (geordnet für debugging-freundliche Stack-Traces).
     */
    public static DefaultInstanceCreationContext forProvide(
            Type typeToResolve, Object parameterValue,
            IInstanceProvider rootProvider,
            de.starima.pfw.base.processor.context.api.IProcessorContext runtimeContext) {

        DefaultInstanceCreationContext ctx = new DefaultInstanceCreationContext();
        ctx.setTypeToResolve(typeToResolve);
        ctx.setParameterValue(parameterValue);
        ctx.setRootProvider(rootProvider);
        ctx.setRuntimeContext(runtimeContext);
        ctx.setCreationStack(new LinkedHashSet<>()); // Geordnet für Debug
        return ctx;
    }

    /**
     * Erstellt einen Root-Kontext für eine extract()-Operation.
     *
     * <p>Initialisiert das VisitedSet als IdentityHashMap-basiertes Set
     * (Identität, nicht equals) und die ExtractionResult-Map.
     */
    public static DefaultInstanceCreationContext forExtract(
            Object objectToResolve,
            IInstanceProvider rootProvider,
            de.starima.pfw.base.processor.context.api.IProcessorContext runtimeContext) {

        DefaultInstanceCreationContext ctx = new DefaultInstanceCreationContext();
        ctx.setObjectToResolve(objectToResolve);
        if (objectToResolve != null) {
            ctx.setTypeToResolve(objectToResolve.getClass());
        }
        ctx.setRootProvider(rootProvider);
        ctx.setRuntimeContext(runtimeContext);
        ctx.setVisitedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
        ctx.setExtractionResult(new LinkedHashMap<>()); // Geordnet für deterministische Ausgabe
        return ctx;
    }

    /**
     * Erstellt einen Kind-Kontext für die Rekursion über ein @ProcessorParameter-Feld.
     *
     * <p>Typischer Einsatz:
     * <pre>
     *   for (Field field : annotatedFields) {
     *       IInstanceCreationContext childCtx = parentCtx.createChildForField(field, paramValue);
     *       Object value = rootProvider.provide(childCtx);
     *       field.set(bean, value);
     *   }
     * </pre>
     */
    public DefaultInstanceCreationContext createChildForField(
            Field field, Object parameterValue) {

        DefaultInstanceCreationContext child = new DefaultInstanceCreationContext(this);
        child.setFieldToResolve(field);
        child.setTypeToResolve(field.getGenericType());
        child.setParameterValue(parameterValue);
        child.setObjectToResolve(null); // Wird vom Provider gesetzt
        child.setProcessorParameter(
                field.isAnnotationPresent(ProcessorParameter.class)
                        ? field.getAnnotation(ProcessorParameter.class)
                        : null);
        return child;
    }

    /**
     * Erstellt einen Kind-Kontext für die Extraktion eines Feld-Wertes.
     */
    public DefaultInstanceCreationContext createChildForExtraction(
            Field field, Object childObject) {

        DefaultInstanceCreationContext child = new DefaultInstanceCreationContext(this);
        child.setFieldToResolve(field);
        child.setTypeToResolve(field.getGenericType());
        child.setObjectToResolve(childObject);
        child.setParameterValue(null); // Bei extract nicht relevant
        child.setProcessorParameter(
                field.isAnnotationPresent(ProcessorParameter.class)
                        ? field.getAnnotation(ProcessorParameter.class)
                        : null);
        return child;
    }

    // =========================================================================
    // Spezialregel: Rekursionstiefe für ProcessorDescriptoren
    // =========================================================================

    @Override
    public int getMaxRecursionDepth(Class<?> type) {
        if (type != null && IProcessorDescriptor.class.isAssignableFrom(type)) {
            return 2; // ProcessorDescriptoren dürfen 2 Ebenen tief sein
        }
        return 1; // Default: keine Rekursion für gleiche fullBeanId
    }
}
