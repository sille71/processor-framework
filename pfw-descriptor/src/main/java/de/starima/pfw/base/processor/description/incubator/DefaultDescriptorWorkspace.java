package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.incubator.api.IDescriptorWorkspace;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceProvider;
import de.starima.pfw.base.processor.description.incubator.api.IPlaceholderDescriptor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Standard-Implementierung des {@link IDescriptorWorkspace}.
 *
 * <p>Verwaltet den partiellen Descriptor-Graph einer Edit-Session.
 * Hält zusätzlich eine interne Patch-Map, die durch applyPatch()-Aufrufe
 * befüllt wird und den Zustand für exportBeanParameterMap() bereitstellt.
 *
 * <p><b>Biologische Analogie:</b> Der Brutkasten. Enthält den sich entwickelnden
 * Organismus (Descriptor-Graph) und kontrolliert die Bedingungen (Patches).
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Partieller Descriptor-Graph einer Edit-Session. " +
                "Verwaltet Placeholder-Knoten, replace()-Operationen und den Patch-Zustand. " +
                "exportBeanParameterMap() liefert den aktuellen Workspace-Zustand als beanParameterMap.",
        categories = {"incubator", "workspace"},
        tags = {"workspace", "descriptor", "placeholder", "edit", "export"}
)
public class DefaultDescriptorWorkspace extends AbstractProcessor implements IDescriptorWorkspace {

    /** Root-Descriptor des Workspace. */
    private IDescriptorProcessor root;

    /** InstanceProvider-Chain für exportBeanParameterMap(). */
    private IInstanceProvider instanceProviderChain;

    /**
     * Interne Patch-Map: beanId → (parameterName → rawValue).
     *
     * <p>Wird durch applyPatch()-Aufrufe befüllt.
     * Bei exportBeanParameterMap() wird diese Map mit dem extract-Ergebnis gemischt.
     */
    private final Map<String, Map<String, Object>> patchMap = new LinkedHashMap<>();

    public DefaultDescriptorWorkspace(IDescriptorProcessor root, IInstanceProvider instanceProviderChain) {
        this.root = root;
        this.instanceProviderChain = instanceProviderChain;
    }

    // =========================================================================
    // find — Pfad-Navigation
    // =========================================================================

    @Override
    public Optional<IDescriptorProcessor> find(String path) {
        if (root == null || path == null) return Optional.empty();

        // Root selbst?
        if (path.equals(root.getPath()) || path.equals(root.getIdentifier())) {
            return Optional.of(root);
        }

        // Delegation an findDescriptor() des Root-Knotens
        return root.findDescriptor(path);
    }

    // =========================================================================
    // replace — Knoten ersetzen
    // =========================================================================

    @Override
    public void replace(String path, IDescriptorProcessor newNode) {
        if (path == null || newNode == null) return;

        // Root ersetzen?
        if (path.equals(root.getPath()) || path.equals(root.getIdentifier())) {
            log.debug("Workspace: Root-Knoten '{}' ersetzt durch {}", path, newNode.getClass().getSimpleName());
            this.root = newNode;
            return;
        }

        // Eltern-Knoten finden: Pfad ohne letztes Segment
        int lastDot = path.lastIndexOf('.');
        if (lastDot < 0) {
            // Direktes Kind des Root
            log.debug("Workspace: Knoten '{}' im Root-Descriptor ersetzt", path);
            // IDescriptorProcessor hat keine setChild()-Methode —
            // Kinder werden über den Processor-Graph verwaltet.
            // Für Phase 4 reicht es, den Knoten in einer internen Map zu merken.
            nodeOverrides.put(path, newNode);
            return;
        }

        // Tiefere Hierarchie
        nodeOverrides.put(path, newNode);
        log.debug("Workspace: Knoten '{}' in Override-Map registriert", path);
    }

    /** Lokale Override-Map für ersetzte Knoten (Phase 4 Basis-Implementierung). */
    private final Map<String, IDescriptorProcessor> nodeOverrides = new LinkedHashMap<>();

    // =========================================================================
    // getPlaceholders — alle Placeholders sammeln
    // =========================================================================

    @Override
    public List<IPlaceholderDescriptor> getPlaceholders() {
        List<IPlaceholderDescriptor> result = new ArrayList<>();
        collectPlaceholders(root, result, new HashSet<>());

        // Auch aus nodeOverrides
        for (IDescriptorProcessor override : nodeOverrides.values()) {
            if (override instanceof IPlaceholderDescriptor placeholder) {
                result.add(placeholder);
            }
        }

        return result;
    }

    private void collectPlaceholders(IDescriptorProcessor node,
                                     List<IPlaceholderDescriptor> result,
                                     Set<IDescriptorProcessor> visited) {
        if (node == null || !visited.add(node)) return;

        if (node instanceof IPlaceholderDescriptor placeholder) {
            result.add(placeholder);
            return; // Placeholder haben keine Kinder
        }

        // Kinder über den Descriptor-Graph traversieren
        // IDescriptorProcessor hat findDescriptor() aber kein getChildren().
        // Für Phase 4: nodeOverrides durchsuchen.
        for (Map.Entry<String, IDescriptorProcessor> entry : nodeOverrides.entrySet()) {
            collectPlaceholders(entry.getValue(), result, visited);
        }
    }

    // =========================================================================
    // exportBeanParameterMap
    // =========================================================================

    @Override
    public Map<String, Map<String, Object>> exportBeanParameterMap() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();

        // 1. Basis: extract-Pfad über den Root-Descriptor (falls InstanceProvider vorhanden)
        if (instanceProviderChain != null && root != null
                && !(root instanceof IPlaceholderDescriptor)) {
            try {
                DefaultInstanceCreationContext ctx = DefaultInstanceCreationContext.forExtract(
                        root, instanceProviderChain, getRuntimeContext());
                instanceProviderChain.extract(ctx);
                result.putAll(ctx.getExtractionResult());
                log.debug("exportBeanParameterMap: {} Einträge via extract-Pfad", result.size());
            } catch (Exception e) {
                log.warn("exportBeanParameterMap: extract fehlgeschlagen: {}", e.getMessage());
            }
        }

        // 2. Patches überschreiben die extrahierten Werte (deep merge)
        for (Map.Entry<String, Map<String, Object>> patchEntry : patchMap.entrySet()) {
            result.computeIfAbsent(patchEntry.getKey(), k -> new LinkedHashMap<>())
                    .putAll(patchEntry.getValue());
        }

        return result;
    }

    /**
     * Trägt einen Patch-Wert in die interne Patch-Map ein.
     *
     * <p>Wird von {@link DefaultEditSession#applyPatch(de.starima.pfw.base.processor.description.incubator.domain.DescriptorPatch)} aufgerufen.
     *
     * @param beanId        Bean-Identifier (Processor-Identifier, z.B. "rootProcessor")
     * @param parameterName Parametername
     * @param rawValue      Rohwert
     */
    public void applyRawValuePatch(String beanId, String parameterName, Object rawValue) {
        patchMap.computeIfAbsent(beanId, k -> new LinkedHashMap<>())
                .put(parameterName, rawValue);
        log.debug("Patch angewendet: {}.{} = {}", beanId, parameterName, rawValue);
    }
}