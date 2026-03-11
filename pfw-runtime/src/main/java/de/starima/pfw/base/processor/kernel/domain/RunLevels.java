package de.starima.pfw.base.processor.kernel.domain;

/**
 * Vordefinierte RunLevel-Namen und -Ränge.
 *
 * <p>Reine Convenience — RunLevels sind frei konfigurierbar über beanParameterMap.
 * Ein RunLevel ist ein {@code String} (Name) und ein {@code int} (Rang),
 * kein Java-Enum.
 *
 * <p>Neue RunLevels sind ohne Code-Änderung möglich:
 * Einfach einen neuen {@code DefaultRunLevelProcessor} konfigurieren.
 */
public final class RunLevels {

    private RunLevels() {
        // Utility class
    }

    // Standard RunLevel-Namen
    public static final String BOOTSTRAP   = "BOOTSTRAP";
    public static final String INCUBATION  = "INCUBATION";
    public static final String RUNTIME     = "RUNTIME";
    public static final String APPLICATION = "APPLICATION";
    public static final String SHUTDOWN    = "SHUTDOWN";

    // Standard RunLevel-Ränge
    public static final int RANK_BOOTSTRAP   = 0;
    public static final int RANK_INCUBATION  = 10;
    public static final int RANK_RUNTIME     = 20;
    public static final int RANK_APPLICATION = 30;
    public static final int RANK_SHUTDOWN    = 99;
}