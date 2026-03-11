package de.starima.pfw.base.processor.kernel.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.ITaskContext;

import java.util.List;

/**
 * Ein RunLevelProcessor beschreibt einen Systemzustand und die
 * Prozessoren (Targets), die dafür erzeugt werden müssen.
 *
 * <p>RunLevels sind frei konfigurierbar — Name und Rang sind
 * ProcessorParameter, kein Enum.
 *
 * <p>Entspricht einem systemd-Target: deklarativ, nicht imperativ.
 * Der RunLevelProcessor kennt keine anderen RunLevels — das ist
 * Sache des RunLevelManagers.
 *
 * <p>Biologisch: Ein Differenzierungsprogramm — "wenn dieses
 * Programm aktiv ist, werden diese Gene exprimiert."
 */
public interface IRunLevelProcessor extends IProcessor {

    /** Name des RunLevels (z.B. "RUNTIME", "DATA_IMPORT"). Frei wählbar. */
    String getRunLevelName();

    /** Rang für die Sortierung. Niedrigere Werte werden zuerst aktiviert. */
    int getRank();

    /**
     * Die Prozessoren, die für diesen RunLevel erzeugt und
     * im Kontext registriert werden müssen.
     * Konfiguriert über beanParameterMap — NICHT hardcoded.
     */
    List<IProcessor> getTargets();

    /**
     * Aktiviert diesen RunLevel: Erzeugt alle Targets und
     * registriert sie im Kontext.
     */
    void activate(ITaskContext ctx);

    /**
     * Deaktiviert diesen RunLevel: Fährt Targets geordnet herunter.
     */
    void deactivate(ITaskContext ctx);
}