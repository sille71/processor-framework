package de.starima.pfw.base.processor.kernel;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.ITaskContext;
import de.starima.pfw.base.processor.kernel.api.IRunLevelProcessor;
import de.starima.pfw.base.processor.kernel.domain.RunLevel;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Standard-Implementierung eines RunLevel-Prozessors.
 *
 * <p>Repräsentiert einen Systemzustand und verwaltet die
 * Prozessoren (Targets), die für diesen Zustand erzeugt werden.
 *
 * <p>Die Targets werden beim Aktivieren im Kontext registriert
 * und beim Deaktivieren in umgekehrter Reihenfolge heruntergefahren.
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Standard-RunLevel-Prozessor. Konfiguriert über Targets, " +
                "die beim Aktivieren erzeugt und im Kontext registriert werden.",
        categories = {"kernel", "lifecycle"},
        tags = {"runlevel", "target", "lifecycle"}
)
public class DefaultRunLevelProcessor extends AbstractProcessor implements IRunLevelProcessor {

    @ProcessorParameter(description = "Der RunLevel, den dieser Prozessor repräsentiert.")
    private RunLevel runLevel;

    @ProcessorParameter(description = "Die zu erzeugenden Target-Prozessoren. " +
            "Werden beim Aktivieren über den Incubator erzeugt (ab RunLevel >= INCUBATION) " +
            "oder direkt über den BeanProvider (RunLevel BOOTSTRAP).")
    private List<IProcessor> targets;

    @Override
    public void activate(ITaskContext ctx) {
        if (targets == null || targets.isEmpty()) {
            log.info("RunLevel {}: keine Targets konfiguriert", runLevel);
            return;
        }
        for (IProcessor target : targets) {
            if (target != null && ctx.getRuntimeContext() != null) {
                ProcessorUtils.registerProcessorInScope(target, ctx.getRuntimeContext());
                log.info("RunLevel {}: Target '{}' aktiviert", runLevel, target.getFullBeanId());
            }
        }
    }

    @Override
    public void deactivate(ITaskContext ctx) {
        if (targets == null || targets.isEmpty()) return;
        List<IProcessor> reversed = new ArrayList<>(targets);
        Collections.reverse(reversed);
        for (IProcessor target : reversed) {
            if (target != null) {
                target.processorOnDestroy();
                log.info("RunLevel {}: Target '{}' deaktiviert", runLevel, target.getFullBeanId());
            }
        }
    }
}