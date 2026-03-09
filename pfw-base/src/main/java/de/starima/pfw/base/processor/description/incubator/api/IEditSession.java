package de.starima.pfw.base.processor.description.incubator.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.incubator.domain.IEditTaskContext;

public interface IEditSession extends IProcessor {
    String getSessionId();
    IEditTaskContext getContext();
    //DescriptorWorkspace getWorkspace();
    //expand(...
    //applyPatch(...
}