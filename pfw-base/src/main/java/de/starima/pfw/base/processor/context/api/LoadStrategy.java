package de.starima.pfw.base.processor.context.api;

public enum LoadStrategy {
    /**
     * LÃ¤dt die gesamte Objekthierarchie rekursiv und vollstÃ¤ndig.
     * (FÃ¼r Exporte, Tests oder vollstÃ¤ndige Detailansichten)
     */
    DEEP,

    /**
     * LÃ¤dt die aktuelle Ebene vollstÃ¤ndig, aber erzeugt fÃ¼r alle Kind-Strukturen
     * nur "lazy" Stubs. (Standard fÃ¼r die initiale UI-Anzeige)
     */
    SHALLOW,

    /**
     * Erzeugt nur einen minimalen "lazy" Stub und stoppt die Rekursion sofort.
     * (Wird intern von SHALLOW fÃ¼r die Kind-Elemente verwendet)
     */
    LAZY
}