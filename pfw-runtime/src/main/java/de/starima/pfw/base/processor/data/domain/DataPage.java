package de.starima.pfw.base.processor.data.domain;

import de.starima.pfw.base.processor.attribute.api.IAttribute;
import de.starima.pfw.base.processor.data.api.IDataPage;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

//TODO: liegt hier noch nicht richtig! Ist nur fÃ¼r Testzwecke hier angesiedelt. GehÃ¶rt in eine allgemeine Bibliothek!

/**
 * Beschreibt eine Datenstruktur (z.B. im csv Format, wenn R vom Typ String[] ist), wie sie beispielsweise von Tabellenabfragen kommt.
 *  - H beschreibt das Headerobjekt (z.b. Column), kann aber auch eine einfache Mapp sein
 *  - R beschreibt das Datenobjekt
 */
@Getter
@Setter
public class DataPage implements IDataPage<IAttribute, String[]> {
    private List<String[]> pageRows;
    private List<IAttribute> pageHeader;
    private Pager pager;
}