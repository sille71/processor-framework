package de.starima.pfw.base.processor.condition.api;

import de.starima.pfw.base.processor.api.IProcessor;
import org.springframework.batch.item.ItemProcessor;

public interface IConditionProcessor<Item> extends ItemProcessor<Item, Item>, IProcessor {
    public String getDescription();
}