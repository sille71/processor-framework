package de.starima.pfw.base.processor.data.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.attribute.api.IAttribute;
import de.starima.pfw.base.processor.data.domain.Pager;

import java.util.List;

public interface ISearchFilterProcessor<S extends IAttribute> extends IProcessor {
    public List<S> getRequestedSearchAttributes();
    public List<S> getRequestedSortAttributes();
    public List<S> getDefaultSortAttributes();
    public List<S> getRequestedDisplayAttributes();
    public Pager getRequestedPager();
}