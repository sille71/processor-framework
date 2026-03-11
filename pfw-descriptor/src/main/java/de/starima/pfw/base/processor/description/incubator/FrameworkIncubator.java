package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.context.api.LoadStrategy;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.api.IProcessorDescriptor;
import de.starima.pfw.base.processor.description.incubator.api.IConstructSession;
import de.starima.pfw.base.processor.description.incubator.api.IDescribeSession;
import de.starima.pfw.base.processor.description.incubator.api.IEditSession;
import de.starima.pfw.base.processor.description.incubator.api.IIncubator;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceProvider;
import de.starima.pfw.base.processor.description.incubator.api.IPlaceholderDescriptor;
import de.starima.pfw.base.processor.description.incubator.domain.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

/**
 * Standard-Implementierung des {@link IIncubator}.
 *
 * <p>Verbindet den provide/extract-Pfad der {@link IInstanceProvider}-Chain mit
 * den Session-Objekten. Alle Operationen erhalten einen {@code IBuildTaskContext}
 * statt separater Source/Policy-Objekte.
 *
 * <h3>startDescribe — extract-Pfad</h3>
 * <ol>
 *   <li>{@code context.getSourceObject()} liefert das zu beschreibende Objekt</li>
 *   <li>{@code DefaultInstanceCreationContext.forExtract()} erstellt den Root-Context</li>
 *   <li>{@code instanceProviderChain.extract()} befüllt {@code extractionResult}</li>
 *   <li>{@link DefaultDescribeSession} kapselt Root-Descriptor + extractionResult</li>
 * </ol>
 *
 * <h3>startConstruct — provide-Pfad</h3>
 * <ol>
 *   <li>{@code context.getRootBeanId()} bestimmt den Einstiegspunkt</li>
 *   <li>{@code context.getRuntimeContext()} liefert den Context mit beanParameterMap</li>
 *   <li>{@code DefaultInstanceCreationContext.forProvide()} erstellt den Root-Context</li>
 *   <li>{@code instanceProviderChain.provide()} erzeugt den lebendigen Objektgraph</li>
 *   <li>{@link DefaultConstructSession} kapselt Root-Objekt + Context</li>
 * </ol>
 *
 * <h3>startEdit — Workspace + Lazy Loading</h3>
 * <ol>
 *   <li>{@code context.getEditTarget()} liefert den zu editierenden Prozessor/Descriptor</li>
 *   <li>Root-Descriptor aufbauen (ROOT_HEADER Stage)</li>
 *   <li>Workspace mit Slot-Placeholders initialisieren</li>
 *   <li>{@link DefaultEditSession} verwaltet den Workspace-Lebenszyklus</li>
 * </ol>
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Standard-Implementierung des IIncubator. " +
                "Verbindet provide/extract-Pfad der InstanceProviderChain mit Session-Objekten. " +
                "startDescribe: Java-Objekt → Descriptor-Graph (extract-Pfad). " +
                "startConstruct: beanParameterMap → lebendiger Objektgraph (provide-Pfad). " +
                "startEdit: Langlebiger Workspace + Lazy Loading + Patches.",
        categories = {"incubator"},
        tags = {"describe", "construct", "edit", "session", "provide", "extract"}
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
    public IDescribeSession startDescribe(IDescribeTaskContext context) {
        if (instanceProviderChain == null) {
            log.warn("FrameworkIncubator: instanceProviderChain nicht konfiguriert");
            return null;
        }

        Object sourceObject = context != null ? context.getSourceObject() : null;
        if (sourceObject == null) {
            log.debug("startDescribe: context.getSourceObject() ist null");
            return null;
        }

        IProcessorContext ctx = context.getRuntimeContext() != null
                ? context.getRuntimeContext()
                : getRuntimeContext();

        // extract-Pfad: Objekt → extractionResult (beanId → parameter-Map)
        DefaultInstanceCreationContext instanceCtx = DefaultInstanceCreationContext.forExtract(
                sourceObject, instanceProviderChain, ctx);
        instanceProviderChain.extract(instanceCtx);

        Map<String, Map<String, Object>> extractionResult = instanceCtx.getExtractionResult();
        log.debug("startDescribe: {} Einträge in extractionResult", extractionResult.size());

        // Root-Descriptor aufbauen
        IDescriptorProcessor root = resolveDescriptorRoot(sourceObject);

        return new DefaultDescribeSession(root, extractionResult);
    }

    // =========================================================================
    // startConstruct — provide-Pfad
    // =========================================================================

    @SuppressWarnings("unchecked")
    @Override
    public <T> IConstructSession<T> startConstruct(IConstructTaskContext context) {
        if (instanceProviderChain == null) {
            log.warn("FrameworkIncubator: instanceProviderChain nicht konfiguriert");
            return null;
        }

        if (context == null) {
            log.debug("startConstruct: context ist null");
            return null;
        }

        String rootBeanId = context.getRootBeanId();
        Class<?> targetType = context.getTargetType() != null ? context.getTargetType() : Object.class;
        IProcessorContext ctx = context.getRuntimeContext() != null
                ? context.getRuntimeContext()
                : getRuntimeContext();

        // provide-Pfad: rootBeanId → lebendiger Objektgraph
        DefaultInstanceCreationContext instanceCtx = DefaultInstanceCreationContext.forProvide(
                targetType, rootBeanId, instanceProviderChain, ctx);

        T root = (T) instanceProviderChain.provide(instanceCtx);

        if (root != null) {
            log.debug("startConstruct: Objekt '{}' erfolgreich erzeugt (Typ: {})",
                    rootBeanId, root.getClass().getSimpleName());
        } else {
            log.warn("startConstruct: provide() lieferte null für rootBeanId '{}'", rootBeanId);
        }

        return new DefaultConstructSession<>(root, context);
    }

    // =========================================================================
    // startEdit — Workspace + Lazy Loading
    // =========================================================================

    @Override
    public IEditSession startEdit(IEditTaskContext context) {
        if (instanceProviderChain == null) {
            log.warn("FrameworkIncubator: instanceProviderChain nicht konfiguriert");
            return null;
        }

        Object editTarget = context != null ? context.getEditTarget() : null;

        // 1. Root-Descriptor aufbauen (ROOT_HEADER Stage)
        IDescriptorProcessor rootDescriptor = buildRootHeaderForEdit(editTarget);

        // 2. Workspace aufbauen
        DefaultDescriptorWorkspace workspace = new DefaultDescriptorWorkspace(
                rootDescriptor, instanceProviderChain);

        // 3. Slot-Placeholders für alle bekannten Parameter erzeugen
        addSlotPlaceholders(workspace, rootDescriptor);

        // 4. Session erstellen
        String sessionId = UUID.randomUUID().toString();
        DefaultEditSession session = new DefaultEditSession(sessionId, workspace);

        log.debug("startEdit: Session '{}' gestartet, Root: {}, {} Placeholders",
                sessionId,
                rootDescriptor != null ? rootDescriptor.getClass().getSimpleName() : "null",
                workspace.getPlaceholders().size());

        return session;
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    /**
     * Löst den Root-Descriptor aus dem Quell-Objekt auf.
     *
     * <p>Auflösungsreihenfolge:
     * <ol>
     *   <li>IDescriptorProcessor → direkt zurückgeben</li>
     *   <li>IProcessor → {@code generateProcessorDescriptorInstance(SHALLOW)}</li>
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
        if (sourceObject != null) {
            log.debug("Kein IDescriptorProcessor für Quell-Objekt {} ermittelbar",
                    sourceObject.getClass().getName());
        }
        return null;
    }

    /**
     * Baut den Root-Descriptor für eine Edit-Session auf (ROOT_HEADER Stage).
     */
    private IDescriptorProcessor buildRootHeaderForEdit(Object editTarget) {
        if (editTarget instanceof IDescriptorProcessor descriptorProcessor) {
            return descriptorProcessor;
        }
        if (editTarget instanceof IProcessor processor) {
            try {
                IProcessorDescriptor descriptor =
                        processor.generateProcessorDescriptorInstance(LoadStrategy.SHALLOW);
                if (descriptor instanceof IDescriptorProcessor descriptorProcessor) {
                    return descriptorProcessor;
                }
            } catch (Exception e) {
                log.warn("startEdit: generateProcessorDescriptorInstance fehlgeschlagen: {}", e.getMessage());
            }
        }
        // Fallback: ROOT_HEADER-Placeholder
        log.debug("startEdit: Kein Descriptor ermittelbar — verwende ROOT_HEADER-Placeholder");
        return new DefaultPlaceholderDescriptor(
                IPlaceholderDescriptor.PlaceholderKind.CHILDREN,
                "root",
                BuildStage.ROOT_HEADER);
    }

    /**
     * Fügt einen CHILDREN-Placeholder als Stellvertreter für alle Slots hinzu.
     */
    private void addSlotPlaceholders(DefaultDescriptorWorkspace workspace,
                                     IDescriptorProcessor root) {
        if (root == null || root instanceof IPlaceholderDescriptor) return;

        DefaultPlaceholderDescriptor slotsPlaceholder = new DefaultPlaceholderDescriptor(
                IPlaceholderDescriptor.PlaceholderKind.CHILDREN,
                (root.getPath() != null ? root.getPath() : root.getIdentifier()) + ".__slots__",
                BuildStage.ROOT_SLOTS_ENUM);

        workspace.replace(slotsPlaceholder.getTargetPath(), slotsPlaceholder);
        log.debug("addSlotPlaceholders: CHILDREN-Placeholder für Root '{}' hinzugefügt",
                root.getIdentifier());
    }
}