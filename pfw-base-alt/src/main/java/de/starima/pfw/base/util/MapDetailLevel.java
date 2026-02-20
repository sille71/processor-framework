package de.starima.pfw.base.util;

/**
 * Definiert den Detaillierungsgrad fÃ¼r eine aus einer Deskriptor-Hierarchie
 * serialisierte beanParameterMap.
 */
public enum MapDetailLevel {
    /**
     * Erzeugt eine vollstÃ¤ndige Map, die sowohl die Zielobjekte (Prozessoren oder Deskriptoren)
     * als auch deren zugehÃ¶rige Deskriptoren als EintrÃ¤ge enthÃ¤lt.
     * Dies ist nÃ¼tzlich fÃ¼r UIs oder vollstÃ¤ndige System-Snapshots.
     */
    FULL_WITH_DESCRIPTORS,

    /**
     * Erzeugt eine "schlanke" Map, die nur die Zielobjekte und ihre Parameterwerte enthÃ¤lt.
     * Die Deskriptoren werden weggelassen.
     * Dies ist nÃ¼tzlich fÃ¼r lesbare, manuelle Konfigurationsdateien.
     */
    PROCESSORS_ONLY
}