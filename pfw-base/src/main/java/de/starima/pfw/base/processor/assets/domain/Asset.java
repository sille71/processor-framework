package de.starima.pfw.base.processor.assets.domain;


import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;

@Getter
@Setter
public class Asset {
    private byte[] data;
    private InputStream inputStream;
    private String mimeType;
    private String name;
    private String description;
    private long size;
}