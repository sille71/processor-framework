package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.processor.description.api.IValueDescriptor;

import java.util.List;

public class AnyValueDescriptor extends DefaultValueDescriptor {

    public AnyValueDescriptor() {
        // Wichtig: Dieser Prototyp hat keine spezifische ValueFunction.
        setValueFunction(null);
    }

    @Override
    public IValueDescriptor getPrototypeValueDescriptor() {
        // Er ist selbst ein Prototyp.
        return null;
    }

    @Override
    public List<IValueDescriptor> getPossibleValueDescriptors() {
        // HIER PASSIERT DIE MAGIE!
        // Er fragt den Provider nach ALLEN anderen verfÃ¼gbaren Prototypen.
        //return getRuntimeContext().getProcessorProvider().getAllValueDescriptorPrototypes();
        return null;
    }
}