package de.starima.pfw.base.processor.kernel;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.context.api.ITaskContext;
import de.starima.pfw.base.processor.kernel.api.IRunLevelManager;
import de.starima.pfw.base.processor.kernel.api.IRunLevelProcessor;
import de.starima.pfw.base.processor.kernel.domain.RunLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Standard-Implementierung des RunLevel-Managers.
 *
 * <p>Verwaltet die RunLevel-Progression des Systems.
 * Kennt alle konfigurierten RunLevelProcessoren und
 * organisiert Transitionen zwischen Systemzuständen.
 *
 * <p>Der Kernel startet nur den RunLevelManager.
 * Alles weitere ist "User Space".
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Verwaltet die RunLevel-Progression. " +
                "Organisiert Transitionen zwischen Systemzuständen.",
        categories = {"kernel", "lifecycle"},
        tags = {"runlevel", "manager", "lifecycle", "kernel"}
)
public class DefaultRunLevelManager extends AbstractProcessor implements IRunLevelManager {

    @ProcessorParameter(description = "Alle konfigurierten RunLevel-Prozessoren, geordnet nach RunLevel-Rank.")
    private List<IRunLevelProcessor> runLevels;

    @ProcessorParameter(value = "BOOTSTRAP",
            description = "Der aktuell aktive RunLevel.",
            ignoreInitialization = true)
    private RunLevel currentRunLevel = RunLevel.BOOTSTRAP;

    @Override
    public void advanceTo(RunLevel target, ITaskContext ctx) {
        if (target == null) {
            log.warn("DefaultRunLevelManager.advanceTo: target ist null");
            return;
        }
        log.info("=== RunLevel-Transition: {} → {} ===", currentRunLevel, target);

        for (IRunLevelProcessor rlp : getRunLevelsSorted()) {
            RunLevel level = rlp.getRunLevel();
            if (level == null) {
                log.warn("DefaultRunLevelManager: RunLevelProcessor '{}' hat keinen RunLevel", rlp.getFullBeanId());
                continue;
            }
            if (level.getRank() > currentRunLevel.getRank()
                    && level.getRank() <= target.getRank()) {
                log.info("=== Transition → RunLevel {} ===", level);
                rlp.activate(ctx);
                this.currentRunLevel = level;
                log.info("=== RunLevel {} erreicht ===", level);
            }
        }
    }

    private List<IRunLevelProcessor> getRunLevelsSorted() {
        if (runLevels == null) return Collections.emptyList();
        return runLevels.stream()
                .filter(r -> r.getRunLevel() != null)
                .sorted(Comparator.comparingInt(r -> r.getRunLevel().getRank()))
                .collect(Collectors.toList());
    }
}