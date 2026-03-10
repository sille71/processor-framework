package de.starima.pfw.base.processor.transformator.api;

import de.starima.pfw.base.processor.api.IProcessor;

//TODO: unter package api verschieben und Transformator zu Transformer Ã¤ndern
public interface IItemTransformatorProcessor<I,O>  extends ItemProcessor<I,O>, IProcessor {
    // Is this processor responsoble for the item
    public boolean isResponsibleFor(I item);
}