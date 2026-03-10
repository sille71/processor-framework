package de.starima.pfw.base.processor.description.incubator.domain;

import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Ergebnis einer applyPatch()-Operation.
 *
 * <p>Enthält Erfolgs-Status, den betroffenen Knoten und eventuelle
 * Nebeneffekte (Pfade, die sich durch den Patch verändert haben).
 */
@Getter
@Builder
public class PatchResult {

    /** War der Patch erfolgreich? */
    private final boolean success;

    /** Pfad des betroffenen Knotens. */
    private final String affectedPath;

    /** Der betroffene Knoten nach dem Patch. */
    private final IDescriptorProcessor affectedNode;

    /**
     * Pfade, die sich als Nebeneffekt des Patches verändert haben.
     *
     * <p>Beispiel: SelectCandidatePatch → neue SUBTREE-Placeholders entstehen.
     */
    private final List<String> sideEffects;

    /** Fehlermeldung, falls success=false. */
    private final String errorMessage;

    public static PatchResult success(String path, IDescriptorProcessor node) {
        return PatchResult.builder()
                .success(true)
                .affectedPath(path)
                .affectedNode(node)
                .sideEffects(List.of())
                .build();
    }

    public static PatchResult failure(String path, String errorMessage) {
        return PatchResult.builder()
                .success(false)
                .affectedPath(path)
                .errorMessage(errorMessage)
                .sideEffects(List.of())
                .build();
    }
}