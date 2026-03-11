package de.starima.pfw.base.processor.kernel.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.ITaskContext;

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

    /** Name des aktuell aktiven RunLevels. */
    String getCurrentRunLevelName();

    /** Rang des aktuell aktiven RunLevels. */
    int getCurrentRank();

    /**
     * Bringt das System bis zum RunLevel mit dem angegebenen Namen.
     * Aktiviert alle RunLevelProcessoren zwischen current und target.
     */
    void advanceTo(String targetRunLevelName, ITaskContext ctx);

    /**
     * Bringt das System bis zum RunLevel mit dem angegebenen Rang.
     * Nützlich für numerische CLI-Parameter.
     */
    void advanceToRank(int targetRank, ITaskContext ctx);

    /** Alle konfigurierten RunLevelProcessoren, geordnet nach Rang. */
    List<IRunLevelProcessor> getRunLevels();
}