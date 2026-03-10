package de.starima.pfw.base.processor.transformator.api;

import de.starima.pfw.base.processor.api.IProcessor;
import org.springframework.batch.item.ItemProcessor;

//TODO: wird der noch benötigt?
public interface IItemTransformerProcessor<I,O>  extends ItemProcessor<I,O>, IProcessor {
    // Is this processor responsoble for the item
    public boolean isResponsibleFor(I item);
}