package de.starima.pfw.base.util;

/**
 * Definiert die strukturellen Modelle, in denen eine Prozessor-Hierarchie
 * in einer beanParameterMap abgebildet werden kann.
 */
public enum ConfigurationModel {
    /**
     * Top-Down-Modell: Ein Eltern-Prozessor verweist auf seine Kinder Ã¼ber
     * Listen von Identifiern (z.B. in einem 'deploymentTransformators'-Parameter).
     * Typisch fÃ¼r UI-Konfigurationen oder GesamtÃ¼bersichten.
     */
    COMPOSITION,

    /**
     * Bottom-Up-Modell: Ein Kind-Prozessor, z.B. Recon, deklariert seine AbhÃ¤ngigkeit
     * von einem Eltern-Kontext Ã¼ber den 'contextProviderProcessor'-Parameter. D.H. der contextProviderProcessor des Kindes
     * erhÃ¤lt den contextProviderProcessor des Parent Kontextes (cluster).
     * Typisch fÃ¼r autarke, einzelne Deployment-Dateien.
     */
    INHERITANCE
}