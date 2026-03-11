package de.starima.pfw.base.processor.kernel;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.context.api.ITaskContext;
import de.starima.pfw.base.processor.kernel.api.IRunLevelManager;
import de.starima.pfw.base.processor.kernel.api.IRunLevelProcessor;
import de.starima.pfw.base.processor.kernel.domain.RunLevels;
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
 * <p>RunLevels sind frei konfigurierbar — Name und Rang als Parameter,
 * kein Java-Enum. Neue RunLevels sind ohne Code-Änderung möglich.
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Verwaltet die RunLevel-Progression. " +
                "RunLevels sind frei konfigurierbar — Name und Rang als Parameter.",
        categories = {"kernel", "lifecycle"},
        tags = {"runlevel", "manager", "lifecycle", "kernel"}
)
public class DefaultRunLevelManager extends AbstractProcessor implements IRunLevelManager {

    @ProcessorParameter(description = "Alle konfigurierten RunLevel-Prozessoren.")
    private List<IRunLevelProcessor> runLevels;

    @ProcessorParameter(value = RunLevels.BOOTSTRAP,
            description = "Name des aktuell aktiven RunLevels.",
            ignoreInitialization = true)
    private String currentRunLevelName = RunLevels.BOOTSTRAP;

    @ProcessorParameter(value = "0",
            description = "Rang des aktuell aktiven RunLevels.",
            ignoreInitialization = true)
    private int currentRank = RunLevels.RANK_BOOTSTRAP;

    @Override
    public void advanceTo(String targetRunLevelName, ITaskContext ctx) {
        if (targetRunLevelName == null) {
            log.warn("DefaultRunLevelManager.advanceTo: targetRunLevelName ist null");
            return;
        }

        List<IRunLevelProcessor> sorted = getRunLevelsSorted();

        IRunLevelProcessor target = sorted.stream()
                .filter(r -> targetRunLevelName.equals(r.getRunLevelName()))
                .findFirst()
                .orElse(null);

        if (target == null) {
            String available = sorted.stream()
                    .map(IRunLevelProcessor::getRunLevelName)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(
                    "RunLevel '" + targetRunLevelName + "' nicht konfiguriert. " +
                    "Verfügbar: " + available);
        }

        advanceToRank(target.getRank(), ctx);
    }

    @Override
    public void advanceToRank(int targetRank, ITaskContext ctx) {
        log.info("=== RunLevel-Transition: '{}' (rank={}) → rank={} ===",
                currentRunLevelName, currentRank, targetRank);

        for (IRunLevelProcessor rlp : getRunLevelsSorted()) {
            if (rlp.getRank() > currentRank && rlp.getRank() <= targetRank) {
                log.info("=== Aktiviere RunLevel '{}' (rank={}) ===",
                        rlp.getRunLevelName(), rlp.getRank());
                rlp.activate(ctx);
                this.currentRunLevelName = rlp.getRunLevelName();
                this.currentRank = rlp.getRank();
                log.info("=== RunLevel '{}' erreicht ===", currentRunLevelName);
            }
        }
    }

    private List<IRunLevelProcessor> getRunLevelsSorted() {
        if (runLevels == null) return Collections.emptyList();
        return runLevels.stream()
                .sorted(Comparator.comparingInt(IRunLevelProcessor::getRank))
                .collect(Collectors.toList());
    }
}