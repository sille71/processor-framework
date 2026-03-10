package de.starima.pfw.base.processor.description.incubator.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Basis-Klasse für alle Änderungen am Descriptor-Graph.
 *
 * <p><b>Biologische Analogie:</b> Ein epigenetisches Signal, das die Genexpression
 * eines Knotens verändert, ohne die DNA (Prototyp-Konfiguration) zu modifizieren.
 *
 * <p>Patches werden über {@link de.starima.pfw.base.processor.description.incubator.api.IEditSession#applyPatch(DescriptorPatch)} angewendet.
 * Das Ergebnis ist ein {@link PatchResult} mit Erfolgs-Status und betroffenen Pfaden.
 *
 * @see SetRawValuePatch
 * @see SelectCandidatePatch
 * @see InsertListItemPatch
 * @see RemoveListItemPatch
 * @see InsertMapEntryPatch
 * @see RemoveMapEntryPatch
 */
@Getter
@RequiredArgsConstructor
public abstract class DescriptorPatch {

    /**
     * Pfad des Descriptor-Knotens, auf den dieser Patch angewendet wird.
     *
     * <p>Beispiele:
     * <ul>
     *   <li>{@code "title"} — Parameter "title" des Root-Prozessors</li>
     *   <li>{@code "formatProcessor.delimiter"} — Parameter "delimiter" des Sub-Prozessors "formatProcessor"</li>
     *   <li>{@code "items[2]"} — drittes Element einer Collection</li>
     * </ul>
     */
    private final String targetPath;
}