package de.starima.pfw.base.processor.description.api;

import java.util.List;

public interface IParameterGroupDescriptor extends IParameterDescriptor {
    public List<IParameterDescriptor> getParameterDescriptors();
    public IParameterDescriptor getParameterDescriptor(String parameterName);

    public void addParameterDescriptor(IParameterDescriptor parameterDescriptor);
}