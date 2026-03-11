package de.starima.pfw.base.processor.kernel.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.ITaskContext;
import de.starima.pfw.base.processor.kernel.domain.RunLevel;

import java.util.List;

/**
 * Verwaltet die RunLevel-Progression des Systems.
 * Kennt alle RunLevelProcessoren, den aktuellen Zustand,
 * und organisiert Transitionen.
 *
 * <p>Der Kernel startet nur den RunLevelManager.
 * Alles andere ist "User Space".
 */
public interface IRunLevelManager extends IProcessor {

    /** Der aktuell aktive RunLevel. */
    RunLevel getCurrentRunLevel();

    /**
     * Bringt das System in den Ziel-RunLevel.
     * Aktiviert alle RunLevelProcessoren zwischen current und target.
     * Bei Rückschritt: deaktiviert in umgekehrter Reihenfolge.
     */
    void advanceTo(RunLevel target, ITaskContext ctx);

    /** Alle konfigurierten RunLevelProcessoren, geordnet nach rank. */
    List<IRunLevelProcessor> getRunLevels();
}