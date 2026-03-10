package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.LoadStrategy;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.api.IProcessorDescriptor;
import de.starima.pfw.base.processor.description.incubator.api.IConstructSession;
import de.starima.pfw.base.processor.description.incubator.api.IDescribeSession;
import de.starima.pfw.base.processor.description.incubator.api.IEditSession;
import de.starima.pfw.base.processor.description.incubator.api.IIncubator;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceProvider;
import de.starima.pfw.base.processor.description.incubator.domain.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Standard-Implementierung des {@link IIncubator}.
 *
 * <p>Verbindet den provide/extract-Pfad der {@link IInstanceProvider}-Chain mit
 * den Session-Objekten ({@link IDescribeSession}, {@link IConstructSession}).
 *
 * <h3>startDescribe — extract-Pfad</h3>
 * <ol>
 *   <li>source.getObject() liefert das zu beschreibende Java-Objekt</li>
 *   <li>{@code DefaultInstanceCreationContext.forExtract()} erstellt den Root-Context</li>
 *   <li>{@code instanceProviderChain.extract()} traversiert das Objekt rekursiv
 *       und befüllt {@code context.getExtractionResult()} (beanId → parameter-Map)</li>
 *   <li>{@link DefaultDescribeSession} kapselt Root-Descriptor + extractionResult</li>
 * </ol>
 *
 * <h3>startConstruct — provide-Pfad</h3>
 * <ol>
 *   <li>source.getRootBeanId() bestimmt den Einstiegspunkt</li>
 *   <li>source.getRuntimeContext() liefert den Context mit der beanParameterMap</li>
 *   <li>{@code DefaultInstanceCreationContext.forProvide()} erstellt den Root-Context</li>
 *   <li>{@code instanceProviderChain.provide()} traversiert die beanParameterMap rekursiv
 *       und erzeugt den lebendigen Objektgraph</li>
 *   <li>{@link DefaultConstructSession} kapselt das Root-Objekt + die Quelle</li>
 * </ol>
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Standard-Implementierung des IIncubator. " +
                "Verbindet provide/extract-Pfad der InstanceProviderChain mit Session-Objekten. " +
                "startDescribe: Java-Objekt → beanParameterMap (extract-Pfad). " +
                "startConstruct: beanParameterMap → lebendiger Objektgraph (provide-Pfad).",
        categories = {"incubator"},
        tags = {"describe", "construct", "session", "provide", "extract"}
)
public class FrameworkIncubator extends AbstractProcessor implements IIncubator {

    @ProcessorParameter(
            description = "Die InstanceProvider-Chain für provide()- und extract()-Operationen. " +
                    "Typischerweise eine InstanceProviderChain mit ProcessorInstanceProvider, " +
                    "CollectionInstanceProvider, MapInstanceProvider, ScalarInstanceProvider.",
            ignoreInitialization = true
    )
    private IInstanceProvider instanceProviderChain;

    // =========================================================================
    // startDescribe — extract-Pfad
    // =========================================================================

    @Override
    public IDescribeSession startDescribe(IDescribeSource source, IDescribePolicy policy) {
        if (instanceProviderChain == null) {
            log.warn("FrameworkIncubator: instanceProviderChain nicht konfiguriert");
            return null;
        }

        Object sourceObject = source != null ? source.getObject() : null;
        if (sourceObject == null) {
            log.debug("startDescribe: source.getObject() ist null");
            return null;
        }

        IProcessorContext ctx = getRuntimeContext();

        // extract-Pfad: Objekt → extractionResult (beanId → parameter-Map)
        DefaultInstanceCreationContext context = DefaultInstanceCreationContext.forExtract(
                sourceObject, instanceProviderChain, ctx);
        instanceProviderChain.extract(context);

        Map<String, Map<String, Object>> extractionResult = context.getExtractionResult();
        log.debug("startDescribe: {} Einträge in extractionResult", extractionResult.size());

        // Root-Descriptor aufbauen
        IDescriptorProcessor root = resolveDescriptorRoot(sourceObject);

        return new DefaultDescribeSession(root, extractionResult);
    }

    // =========================================================================
    // startConstruct — provide-Pfad
    // =========================================================================

    @Override
    public IConstructSession<Object> startConstruct(IConstructSource source, IConstructPolicy policy) {
        return startConstruct(Object.class, source, policy);
    }

    @Override
    public <T> IConstructSession<T> startConstruct(Class<T> clazz, IConstructSource source,
                                                    IConstructPolicy policy) {
        if (instanceProviderChain == null) {
            log.warn("FrameworkIncubator: instanceProviderChain nicht konfiguriert");
            return null;
        }

        if (source == null) {
            log.debug("startConstruct: source ist null");
            return null;
        }

        String rootBeanId = source.getRootBeanId();
        IProcessorContext ctx = source.getRuntimeContext() != null
                ? source.getRuntimeContext()
                : getRuntimeContext();

        // provide-Pfad: rootBeanId → lebendiger Objektgraph
        DefaultInstanceCreationContext context = DefaultInstanceCreationContext.forProvide(
                clazz, rootBeanId, instanceProviderChain, ctx);

        @SuppressWarnings("unchecked")
        T root = (T) instanceProviderChain.provide(context);

        if (root != null) {
            log.debug("startConstruct: Objekt '{}' erfolgreich erzeugt (Typ: {})",
                    rootBeanId, root.getClass().getSimpleName());
        } else {
            log.warn("startConstruct: provide() lieferte null für rootBeanId '{}'", rootBeanId);
        }

        return new DefaultConstructSession<>(root, source);
    }

    // =========================================================================
    // startEdit — Phase 4
    // =========================================================================

    @Override
    public IEditSession startEdit(IEditSource source, IEditPolicy policy) {
        // Phase 4: Edit-Sessions (Workspace, Cursor-Stages)
        log.debug("startEdit() — noch nicht implementiert (Phase 4)");
        return null;
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    /**
     * Versucht, den Root-Descriptor aus dem Quell-Objekt zu ermitteln.
     *
     * <p>Auflösungsreihenfolge:
     * <ol>
     *   <li>Quell-Objekt ist selbst ein {@link IDescriptorProcessor} → direkt zurückgeben</li>
     *   <li>Quell-Objekt ist ein {@link IProcessor} →
     *       {@link IProcessor#generateProcessorDescriptorInstance(LoadStrategy)} aufrufen,
     *       falls das Ergebnis ein {@link IDescriptorProcessor} ist</li>
     *   <li>Fallback: null</li>
     * </ol>
     */
    private IDescriptorProcessor resolveDescriptorRoot(Object sourceObject) {
        if (sourceObject instanceof IDescriptorProcessor descriptorProcessor) {
            return descriptorProcessor;
        }
        if (sourceObject instanceof IProcessor processor) {
            try {
                IProcessorDescriptor descriptor =
                        processor.generateProcessorDescriptorInstance(LoadStrategy.SHALLOW);
                if (descriptor instanceof IDescriptorProcessor descriptorProcessor) {
                    return descriptorProcessor;
                }
                log.debug("generateProcessorDescriptorInstance() lieferte keinen IDescriptorProcessor: {}",
                        descriptor != null ? descriptor.getClass().getName() : "null");
            } catch (Exception e) {
                log.warn("Descriptor-Generierung fehlgeschlagen für '{}': {}",
                        processor.getFullBeanId(), e.getMessage());
            }
        }
        log.debug("Kein IDescriptorProcessor für Quell-Objekt {} ermittelbar",
                sourceObject.getClass().getName());
        return null;
    }
}