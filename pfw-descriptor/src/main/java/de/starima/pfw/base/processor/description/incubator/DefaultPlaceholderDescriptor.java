package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.incubator.api.IPlaceholderDescriptor;
import de.starima.pfw.base.processor.assets.api.IAssetProviderProcessor;
import de.starima.pfw.base.processor.description.incubator.domain.BuildStage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Standard-Implementierung eines {@link IPlaceholderDescriptor}.
 *
 * <p>Repräsentiert einen noch nicht geladenen Knoten im Descriptor-Graph.
 * Das UI kann Placeholders über {@link de.starima.pfw.base.processor.description.incubator.api.IEditSession#expand(String, Object)}
 * durch vollständige Descriptor-Knoten ersetzen lassen.
 *
 * <p><b>Biologische Analogie:</b> Eine undifferenzierte Stammzelle.
 * Sie weiß, an welcher Position sie sitzt ({@code targetPath}), was sie
 * werden soll ({@code kind}) und welchen nächsten Entwicklungsschritt
 * sie durchlaufen soll ({@code nextStage}).
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Unvollständiger Knoten im Descriptor-Graph (Lazy-Loading-Placeholder). " +
                "Wird durch expand() durch den vollständigen Descriptor ersetzt. " +
                "Biologisch: Eine noch nicht differenzierte Stammzelle.",
        categories = {"incubator", "placeholder"},
        tags = {"placeholder", "lazy", "expand", "cursor-stage"}
)
public class DefaultPlaceholderDescriptor extends AbstractProcessor implements IPlaceholderDescriptor {

    private PlaceholderKind kind;
    private String targetPath;
    private BuildStage nextStage;
    private Map<String, Object> expandHints = Collections.emptyMap();
    private IDescriptorProcessor parentDescriptor;

    public DefaultPlaceholderDescriptor(PlaceholderKind kind, String targetPath, BuildStage nextStage) {
        this.kind = kind;
        this.targetPath = targetPath;
        this.nextStage = nextStage;
        setIdentifier(targetPath);
    }

    // =========================================================================
    // IDescriptorProcessor — path navigation
    // =========================================================================

    @Override
    public String getPath() {
        return targetPath;
    }

    @Override
    public Optional<IDescriptorProcessor> findDescriptor(String path) {
        // Placeholder hat keine Kinder
        if (targetPath != null && targetPath.equals(path)) {
            return Optional.of(this);
        }
        return Optional.empty();
    }

    // =========================================================================
    // IDescriptorProcessor — description (stubs for placeholder)
    // =========================================================================

    @Override
    public String getGeneralDescription() {
        return "[Placeholder: " + kind + " @ " + targetPath + "]";
    }

    @Override
    public void setGeneralDescription(String description) { /* no-op */ }

    @Override
    public IAssetProviderProcessor getGeneralAssetProviderProcessor() { return null; }

    @Override
    public String getGeneralAssetName() { return null; }

    @Override
    public String getGeneralAssetType() { return null; }

    @Override
    public String getGeneralAsset() { return null; }

    @Override
    public String getGeneralAssetUrl() { return null; }

    @Override
    public String getSpecialDescription() { return null; }

    @Override
    public void setSpecialDescription(String description) { /* no-op */ }

    @Override
    public IAssetProviderProcessor getSpecialAssetProviderProcessor() { return null; }

    @Override
    public String getSpecialAssetName() { return null; }

    @Override
    public String getSpecialAssetType() { return null; }

    @Override
    public String getSpecialAsset() { return null; }

    @Override
    public String getSpecialAssetUrl() { return null; }

    // =========================================================================
    // IDescriptorProcessor — extraction (no-ops for placeholder)
    // =========================================================================

    @Override
    public Map<String, Map<String, Object>> extractEffectiveParameterMap(Object bean) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Map<String, Object>> extractEffectiveParameterMap(
            ITransformationContext transformationContext, Object bean) {
        return Collections.emptyMap();
    }

    @Override
    public void extractEffectiveParameterMap(
            ITransformationContext context,
            Map<String, Map<String, Object>> beanParameterMap,
            Set<Object> visited) {
        // Placeholder hat keine Parameter
    }

    // =========================================================================
    // toString
    // =========================================================================

    @Override
    public String toString() {
        return "DefaultPlaceholderDescriptor[kind=" + kind +
                ", path=" + targetPath +
                ", nextStage=" + nextStage + "]";
    }
}