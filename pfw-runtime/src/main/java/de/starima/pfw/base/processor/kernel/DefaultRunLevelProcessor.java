package de.starima.pfw.base.processor.kernel;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.ITaskContext;
import de.starima.pfw.base.processor.kernel.api.IRunLevelProcessor;
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
 * <p>Name und Rang sind frei konfigurierbar über beanParameterMap —
 * kein Java-Enum. Neue RunLevels sind ohne Code-Änderung möglich.
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Standard-RunLevel-Prozessor. RunLevel-Name und Rang " +
                "sind frei konfigurierbar über beanParameterMap.",
        categories = {"kernel", "lifecycle"},
        tags = {"runlevel", "target", "lifecycle"}
)
public class DefaultRunLevelProcessor extends AbstractProcessor implements IRunLevelProcessor {

    @ProcessorParameter(description = "Name des RunLevels (z.B. 'RUNTIME', 'DATA_IMPORT'). Frei wählbar.")
    private String runLevelName;

    @ProcessorParameter(description = "Rang für die Sortierung. Niedrigere Werte werden zuerst aktiviert.",
            value = "0")
    private int rank;

    @ProcessorParameter(description = "Die zu erzeugenden Target-Prozessoren.")
    private List<IProcessor> targets;

    @Override
    public void activate(ITaskContext ctx) {
        if (targets == null || targets.isEmpty()) {
            log.info("RunLevel '{}' (rank={}): keine Targets konfiguriert", runLevelName, rank);
            return;
        }
        for (IProcessor target : targets) {
            if (target != null && ctx.getRuntimeContext() != null) {
                ProcessorUtils.registerProcessorInScope(target, ctx.getRuntimeContext());
                log.info("RunLevel '{}': Target '{}' aktiviert", runLevelName, target.getFullBeanId());
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
                log.info("RunLevel '{}': Target '{}' deaktiviert", runLevelName, target.getFullBeanId());
            }
        }
    }
}