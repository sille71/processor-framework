package de.starima.pfw.base.processor.parameter;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum PropertyType {
    ARCHIVE("archive"),
    BUCKET("bucket");

    @Getter private String value;
}