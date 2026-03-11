package de.starima.pfw.base.processor.kernel.domain;

/**
 * RunLevels sind SYSTEMZUSTÄNDE, keine Abläufe.
 * Ein RunLevel garantiert, dass bestimmte Prozessoren verfügbar sind.
 *
 * <p>Biologisch: Entwicklungsstadien eines Organismus —
 * jedes Stadium garantiert bestimmte Fähigkeiten.
 *
 * <p>Unix-Analogie: Kernel → PID 1 → systemd → Services
 */
public enum RunLevel {
    BOOTSTRAP(0),       // KernelContext, BeanProvider, Logging
    INCUBATION(1),      // FrameworkIncubator, InstanceProviderChain, Descriptoren
    RUNTIME(2),         // Gateway, Dispatcher, Security
    APPLICATION(3),     // Fachliche Services
    SHUTDOWN(99);       // Geordnetes Herunterfahren

    private final int rank;

    RunLevel(int rank) { this.rank = rank; }

    public int getRank() { return this.rank; }

    public boolean atLeast(RunLevel other) { return this.rank >= other.rank; }
}