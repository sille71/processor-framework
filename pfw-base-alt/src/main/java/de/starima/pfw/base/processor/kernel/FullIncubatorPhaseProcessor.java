package de.starima.pfw.base.processor.kernel;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.context.api.ITaskContext;
import de.starima.pfw.base.processor.description.incubator.api.IIncubator;
import de.starima.pfw.base.processor.kernel.api.IPhaseProcessor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Processor
public class FullIncubatorPhaseProcessor extends AbstractProcessor implements IPhaseProcessor {
    @ProcessorParameter(value = "FullIncubator")
    private String phaseName = "FullIncubator";
    @ProcessorParameter(value = "1")
    private Integer runLevel = 0;
    @ProcessorParameter
    private List<IPhaseProcessor> phaseProcessors;

    @ProcessorParameter(description = "Der vollstÃ¤ndige Incubator, welcher vom kernel durch den schmaleren kernelIncubator erzeugt wird. Sobald dieser initialisiert wurde, " +
            "Ã¼bernimmt er die restliche Initialisierung des Kernels inkl. des serviceProcessor.", value = "", ignoreInitialization = true)
    private IIncubator fullIncubator;


    @Override
    public void initPhase(ITaskContext ctx) {

    }
}