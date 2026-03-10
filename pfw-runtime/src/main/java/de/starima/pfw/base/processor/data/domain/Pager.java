package de.starima.pfw.base.processor.data.domain;

import de.starima.pfw.base.processor.data.api.IPager;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Pager implements IPager {
    private int firstRow = 1;
    private int pageSize = 5;
    private long rowCount = 0L;
    private Integer[] rowsPerPageOptions = new Integer[]{5,15,20};
}