package de.starima.pfw.base.processor.description.incubator.domain;

import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.incubator.api.IPlaceholderDescriptor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Ergebnis einer expand()-Operation.
 *
 * <p>Enthält den expandierten Descriptor-Knoten und die neu entstandenen
 * Kinder-Placeholders (die noch nicht geladen sind).
 */
@Getter
@Builder
public class ExpandResult {

    /** Pfad des expandierten Knotens. */
    private final String expandedPath;

    /** Der vollständig expandierte Knoten (ersetzt den Placeholder). */
    private final IDescriptorProcessor expandedNode;

    /**
     * Neue Kinder-Placeholders, die beim Expand entstanden sind.
     *
     * <p>Das UI kann diese auflisten und gezielt nachladen.
     */
    private final List<IPlaceholderDescriptor> newPlaceholders;

    public static ExpandResult notFound(String path) {
        return ExpandResult.builder()
                .expandedPath(path)
                .expandedNode(null)
                .newPlaceholders(List.of())
                .build();
    }
}