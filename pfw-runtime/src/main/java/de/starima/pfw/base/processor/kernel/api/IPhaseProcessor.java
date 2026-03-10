package de.starima.pfw.base.processor.kernel.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.ITaskContext;

import java.util.List;

public interface IPhaseProcessor extends IProcessor {
    String getPhaseName();
    Integer getRunLevel();
    void initPhase(ITaskContext ctx);
    List<IPhaseProcessor> getPhaseProcessors();
}