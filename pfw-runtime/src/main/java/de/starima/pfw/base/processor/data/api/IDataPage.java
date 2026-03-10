package de.starima.pfw.base.processor.data.api;


import java.util.List;

public interface IDataPage<H,R> {
    public List<R> getPageRows();
    public List<H> getPageHeader();
    public IPager getPager();
}