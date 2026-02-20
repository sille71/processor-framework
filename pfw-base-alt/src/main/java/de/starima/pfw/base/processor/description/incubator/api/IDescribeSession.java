package de.starima.pfw.base.processor.description.incubator.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.incubator.domain.IDescribeTaskContext;

public interface IDescribeSession extends IProcessor {
    IDescriptorProcessor getRoot();
    IDescribeTaskContext getContext();
    //hier path und policy noch modellieren
    IDescriptorProcessor expand(String path, String policy);
}