package de.starima.pfw.base.processor.data.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.attribute.api.IAttribute;

import java.util.List;

public interface IDataProviderProcessor<H extends IAttribute,R> extends IProcessor {
    public List<H> getPageHeader();
    public List<R> getPageRows();
    public IPager getPager();
    public String getLabel(H attribute);
    public String getProviderLabel();
    public ISearchFilterProcessor<H> getSearchFilterProcessor();
    public IDataPage<H,R> loadData(ISearchFilterProcessor<H> searchFilter);
}