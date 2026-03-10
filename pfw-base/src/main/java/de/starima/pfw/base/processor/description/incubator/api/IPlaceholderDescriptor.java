package de.starima.pfw.base.processor.description.incubator.api;

import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.incubator.domain.BuildStage;

import java.util.Map;

/**
 * Ein explizit unvollständiger Knoten im Descriptor-Graph.
 *
 * <p>Wird vom {@link IDescriptorWorkspace} verwaltet und von {@link IEditSession#expand(String, Object)}
 * durch den vollständigen Descriptor ersetzt.
 *
 * <p><b>Biologische Analogie:</b> Eine Stammzelle, die noch nicht differenziert ist.
 * Sie weiß, zu was sie werden soll ({@link #getNextStage()}), aber hat ihre
 * endgültige Form noch nicht angenommen.
 *
 * <p>Placeholder können durch das UI gezielt geladen werden (Lazy Loading):
 * Das UI iteriert über {@link IDescriptorWorkspace#getPlaceholders()} und
 * ruft für jeden Placeholder {@link IEditSession#expand(String, Object)} auf.
 */
public interface IPlaceholderDescriptor extends IDescriptorProcessor {

    /**
     * Art des fehlenden Inhalts.
     */
    enum PlaceholderKind {
        /** ParameterDescriptor fehlt (Slot noch nicht geladen). */
        SLOT,
        /** ValueDescriptor fehlt (Wert-Typ noch nicht bestimmt). */
        VALUE,
        /** Slots/Items/Entries fehlen als Block (ROOT_SLOTS_ENUM noch nicht geladen). */
        CHILDREN,
        /** getPossibleValueDescriptors() nicht geladen (Polymorphie-Kandidaten). */
        CANDIDATES,
        /** Polymorphe Auswahl: Subtree für gewählten Typ fehlt. */
        SUBTREE
    }

    /** Art des fehlenden Inhalts. */
    PlaceholderKind getKind();

    /** Pfad des Knotens, der durch diesen Placeholder repräsentiert wird. */
    String getTargetPath();

    /** Die nächste Stage, die beim Expand ausgeführt werden soll. */
    BuildStage getNextStage();

    /**
     * Optionale Hints für die Expansion.
     *
     * <p>Beispiele:
     * <ul>
     *   <li>{@code selectedPrototypeId}: bei POLY_SELECTION_SET</li>
     *   <li>{@code pageOffset}, {@code pageSize}: bei Collection/Map-Items</li>
     * </ul>
     */
    Map<String, Object> getExpandHints();
}