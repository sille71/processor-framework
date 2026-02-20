package de.starima.pfw.base.processor.data.api;

public interface IPager {
    public int getFirstRow();
    public int getPageSize();
    public long getRowCount();
    public Integer[] getRowsPerPageOptions();
}