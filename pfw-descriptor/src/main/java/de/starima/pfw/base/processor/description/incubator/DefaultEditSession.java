package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.incubator.api.IDescriptorWorkspace;
import de.starima.pfw.base.processor.description.incubator.api.IEditSession;
import de.starima.pfw.base.processor.description.incubator.api.IPlaceholderDescriptor;
import de.starima.pfw.base.processor.description.incubator.domain.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Standard-Implementierung von {@link IEditSession}.
 *
 * <p>Verwaltet einen {@link DefaultDescriptorWorkspace} und koordiniert
 * expand()- und applyPatch()-Operationen.
 *
 * <p><b>Lebenszyklus:</b>
 * <pre>
 *   startEdit() → expand("title") → applyPatch(SetRawValuePatch) → exportBeanParameterMap()
 * </pre>
 *
 * <p><b>expand()-Logik:</b>
 * <ol>
 *   <li>Placeholder bei {@code path} suchen</li>
 *   <li>Neuen Descriptor aufbauen (Stage-abhängig)</li>
 *   <li>Workspace.replace() aufrufen</li>
 *   <li>Kinder-Placeholders zurückgeben</li>
 * </ol>
 *
 * <p><b>applyPatch()-Logik:</b>
 * <ol>
 *   <li>Patch-Typ erkennen</li>
 *   <li>{@code SetRawValuePatch}: Wert in Workspace-PatchMap schreiben</li>
 *   <li>{@code SelectCandidatePatch}: SUBTREE-Placeholder erstellen</li>
 *   <li>{@code Insert/RemovePatch}: Collection/Map im Workspace modifizieren</li>
 * </ol>
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Langlebiger Workspace für UI-Editing. " +
                "Verwaltet einen partiellen Descriptor-Graph mit Placeholder-Descriptoren. " +
                "Unterstützt inkrementelles Laden (expand) und Editieren (applyPatch). " +
                "Biologisch: Der Brutkasten, in dem der Embryo unter kontrollierten Bedingungen heranwächst.",
        categories = {"incubator", "session"},
        tags = {"edit", "workspace", "lazy", "placeholder", "patch"}
)
public class DefaultEditSession extends AbstractProcessor implements IEditSession {

    @ProcessorParameter(description = "Eindeutige Session-ID für diese Edit-Session (UUID).",
            ignoreInitialization = true)
    private String sessionId;

    @ProcessorParameter(description = "Der Workspace mit dem partiellen Descriptor-Graph dieser Edit-Session.",
            ignoreInitialization = true)
    private IDescriptorWorkspace workspace;

    public DefaultEditSession(String sessionId, IDescriptorWorkspace workspace) {
        this.sessionId = sessionId;
        this.workspace = workspace;
    }

    @Override
    public IEditTaskContext getContext() {
        // Phase 4+: eigener Context mit Workspace-Metadaten
        return null;
    }

    // =========================================================================
    // expand — Placeholder → vollständiger Descriptor
    // =========================================================================

    @Override
    public ExpandResult expand(String path, ExpandPolicy policy) {
        if (path == null || workspace == null) {
            return ExpandResult.notFound(path);
        }

        // Placeholder suchen
        Optional<IDescriptorProcessor> nodeOpt = workspace.find(path);
        if (nodeOpt.isEmpty()) {
            log.debug("expand('{}'): Knoten nicht gefunden", path);
            return ExpandResult.notFound(path);
        }

        IDescriptorProcessor node = nodeOpt.get();

        if (!(node instanceof IPlaceholderDescriptor placeholder)) {
            // Kein Placeholder — bereits vollständig
            log.debug("expand('{}'): Knoten ist kein Placeholder, bereits vollständig", path);
            return ExpandResult.builder()
                    .expandedPath(path)
                    .expandedNode(node)
                    .newPlaceholders(List.of())
                    .build();
        }

        // Stage-basierte Expansion
        IDescriptorProcessor expandedNode = expandPlaceholder(placeholder, policy);
        if (expandedNode == null) {
            log.warn("expand('{}'): Expansion fehlgeschlagen (Stage: {})", path, placeholder.getNextStage());
            return ExpandResult.notFound(path);
        }

        // Knoten im Workspace ersetzen
        workspace.replace(path, expandedNode);

        // Neue Kinder-Placeholders einsammeln
        List<IPlaceholderDescriptor> newPlaceholders = collectNewPlaceholders(expandedNode);

        log.debug("expand('{}'): erfolgreich (Stage: {}), {} neue Placeholders",
                path, placeholder.getNextStage(), newPlaceholders.size());

        return ExpandResult.builder()
                .expandedPath(path)
                .expandedNode(expandedNode)
                .newPlaceholders(newPlaceholders)
                .build();
    }

    /**
     * Stage-basierte Expansion eines Placeholders.
     *
     * <p>In Phase 4: Basisimplementierung — erzeugt einen neuen Placeholder
     * für die nächste Stage. Vollständige Stage-Logik ist Aufgabe des ConstructionManager (Phase 5+).
     */
    private IDescriptorProcessor expandPlaceholder(IPlaceholderDescriptor placeholder,
                                                    ExpandPolicy policy) {
        BuildStage nextStage = placeholder.getNextStage();
        String path = placeholder.getTargetPath();

        // Basis-Expansion: Placeholder durch leeren Descriptor-Knoten ersetzen.
        // In Phase 5+ wird hier der ConstructionManager aufgerufen.
        switch (placeholder.getKind()) {
            case SLOT -> {
                // SLOT: ParameterDescriptor-Placeholder → Expanded mit VALUE-Placeholder als Kind
                DefaultPlaceholderDescriptor expanded = new DefaultPlaceholderDescriptor(
                        IPlaceholderDescriptor.PlaceholderKind.VALUE,
                        path,
                        BuildStage.VALUE_HEADER);
                return expanded;
            }
            case VALUE -> {
                // VALUE: ValueDescriptor-Placeholder → Scalar-Placeholder
                DefaultPlaceholderDescriptor expanded = new DefaultPlaceholderDescriptor(
                        IPlaceholderDescriptor.PlaceholderKind.VALUE,
                        path,
                        BuildStage.SCALAR_RAWVALUE);
                return expanded;
            }
            case CHILDREN -> {
                // CHILDREN: Slots noch nicht geladen
                DefaultPlaceholderDescriptor expanded = new DefaultPlaceholderDescriptor(
                        IPlaceholderDescriptor.PlaceholderKind.SLOT,
                        path,
                        BuildStage.SLOT_DESCRIPTOR);
                return expanded;
            }
            default -> {
                log.debug("expand(): PlaceholderKind '{}' nicht implementiert (Phase 5+)",
                        placeholder.getKind());
                return placeholder; // Bleibt Placeholder für jetzt
            }
        }
    }

    private List<IPlaceholderDescriptor> collectNewPlaceholders(IDescriptorProcessor node) {
        List<IPlaceholderDescriptor> result = new ArrayList<>();
        if (node instanceof IPlaceholderDescriptor p) {
            result.add(p);
        }
        return result;
    }

    // =========================================================================
    // applyPatch — Descriptor-Graph modifizieren
    // =========================================================================

    @Override
    public PatchResult applyPatch(DescriptorPatch patch) {
        if (patch == null || workspace == null) {
            return PatchResult.failure(null, "Patch oder Workspace ist null");
        }

        String path = patch.getTargetPath();

        if (patch instanceof SetRawValuePatch rawValuePatch) {
            return applySetRawValue(path, rawValuePatch);
        }

        if (patch instanceof SelectCandidatePatch candidatePatch) {
            return applySelectCandidate(path, candidatePatch);
        }

        if (patch instanceof InsertListItemPatch insertPatch) {
            return applyInsertListItem(path, insertPatch);
        }

        if (patch instanceof RemoveListItemPatch removePatch) {
            return applyRemoveListItem(path, removePatch);
        }

        if (patch instanceof InsertMapEntryPatch insertMapPatch) {
            return applyInsertMapEntry(path, insertMapPatch);
        }

        if (patch instanceof RemoveMapEntryPatch removeMapPatch) {
            return applyRemoveMapEntry(path, removeMapPatch);
        }

        return PatchResult.failure(path, "Unbekannter Patch-Typ: " + patch.getClass().getSimpleName());
    }

    // =========================================================================
    // Patch-Implementierungen
    // =========================================================================

    private PatchResult applySetRawValue(String path, SetRawValuePatch patch) {
        // Pfad in beanId + parameterName aufteilen
        // Format: "beanId.parameterName" oder nur "parameterName" (dann Root-beanId)
        String beanId;
        String parameterName;

        int lastDot = path.lastIndexOf('.');
        if (lastDot >= 0) {
            beanId = path.substring(0, lastDot);
            parameterName = path.substring(lastDot + 1);
        } else {
            // Root-Processor: beanId aus dem Root-Descriptor ermitteln
            beanId = workspace.getRoot() != null
                    ? workspace.getRoot().getIdentifier()
                    : "root";
            parameterName = path;
        }

        // Patch in den Workspace-PatchMap schreiben
        if (workspace instanceof DefaultDescriptorWorkspace descriptorWorkspace) {
            descriptorWorkspace.applyRawValuePatch(beanId, parameterName, patch.getNewRawValue());
        }

        // Betroffenen Knoten für das Ergebnis finden
        IDescriptorProcessor affectedNode = workspace.find(path).orElse(workspace.getRoot());

        log.debug("SetRawValuePatch: {}.{} = {}", beanId, parameterName, patch.getNewRawValue());
        return PatchResult.success(path, affectedNode);
    }

    private PatchResult applySelectCandidate(String path, SelectCandidatePatch patch) {
        // SUBTREE-Placeholder für den gewählten Typ erstellen
        DefaultPlaceholderDescriptor subtreePlaceholder = new DefaultPlaceholderDescriptor(
                IPlaceholderDescriptor.PlaceholderKind.SUBTREE,
                path,
                BuildStage.PROCESSOR_SLOTS);
        subtreePlaceholder.getExpandHints().put("selectedPrototypeId", patch.getSelectedPrototypeId());

        workspace.replace(path, subtreePlaceholder);

        log.debug("SelectCandidatePatch: {} → prototypeId '{}'", path, patch.getSelectedPrototypeId());
        return PatchResult.builder()
                .success(true)
                .affectedPath(path)
                .affectedNode(subtreePlaceholder)
                .sideEffects(List.of(path))
                .build();
    }

    private PatchResult applyInsertListItem(String path, InsertListItemPatch patch) {
        // Phase 4: Collection-Modifikation — in Workspace-PatchMap registrieren
        if (workspace instanceof DefaultDescriptorWorkspace descriptorWorkspace) {
            String beanId = resolveBeanId(path);
            String paramName = resolveParamName(path);
            // Einfacher Placeholder-Wert für das neue Element
            descriptorWorkspace.applyRawValuePatch(beanId, paramName + "[insert@" + patch.getIndex() + "]",
                    patch.getItemRawValue());
        }
        log.debug("InsertListItemPatch: {}[{}] = {}", path, patch.getIndex(), patch.getItemRawValue());
        return PatchResult.success(path, workspace.find(path).orElse(null));
    }

    private PatchResult applyRemoveListItem(String path, RemoveListItemPatch patch) {
        if (workspace instanceof DefaultDescriptorWorkspace descriptorWorkspace) {
            String beanId = resolveBeanId(path);
            String paramName = resolveParamName(path);
            descriptorWorkspace.applyRawValuePatch(beanId, paramName + "[remove@" + patch.getIndex() + "]",
                    null);
        }
        log.debug("RemoveListItemPatch: {}[{}]", path, patch.getIndex());
        return PatchResult.success(path, workspace.find(path).orElse(null));
    }

    private PatchResult applyInsertMapEntry(String path, InsertMapEntryPatch patch) {
        if (workspace instanceof DefaultDescriptorWorkspace descriptorWorkspace) {
            String beanId = resolveBeanId(path);
            String paramName = resolveParamName(path);
            descriptorWorkspace.applyRawValuePatch(beanId, paramName + "[key=" + patch.getKey() + "]",
                    patch.getValue());
        }
        log.debug("InsertMapEntryPatch: {} key='{}' = {}", path, patch.getKey(), patch.getValue());
        return PatchResult.success(path, workspace.find(path).orElse(null));
    }

    private PatchResult applyRemoveMapEntry(String path, RemoveMapEntryPatch patch) {
        if (workspace instanceof DefaultDescriptorWorkspace descriptorWorkspace) {
            String beanId = resolveBeanId(path);
            String paramName = resolveParamName(path);
            descriptorWorkspace.applyRawValuePatch(beanId, paramName + "[remove-key=" + patch.getKey() + "]",
                    null);
        }
        log.debug("RemoveMapEntryPatch: {} key='{}'", path, patch.getKey());
        return PatchResult.success(path, workspace.find(path).orElse(null));
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    private String resolveBeanId(String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot >= 0) return path.substring(0, lastDot);
        return workspace.getRoot() != null ? workspace.getRoot().getIdentifier() : "root";
    }

    private String resolveParamName(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }
}