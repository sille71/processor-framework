package de.starima.pfw.base.processor.description.incubator.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.EnumSet;
import java.util.Set;

/**
 * Konfiguriert, wie ein Placeholder expandiert wird.
 *
 * <p>Steuert Rekursionstiefe, auszuführende Stages und Seitengröße
 * für paginierte Collections und Maps.
 */
@Getter
@Setter
public class ExpandPolicy {

    /** Wie tief soll expandiert werden? (1 = nur direkte Kinder) */
    private int maxDepth = 1;

    /**
     * Welche Stages sollen beim Expand ausgeführt werden?
     * Leer = alle relevanten Stages für den Placeholder-Typ.
     */
    private Set<BuildStage> stages = EnumSet.noneOf(BuildStage.class);

    /** Seitengröße für Collection/Map-Items (COLLECTION_ITEMS_PAGE, MAP_ENTRIES_PAGE). */
    private int pageSize = 20;

    /** Seitenoffset für Collection/Map-Items. */
    private int pageOffset = 0;

    public static ExpandPolicy defaultPolicy() {
        return new ExpandPolicy();
    }
}