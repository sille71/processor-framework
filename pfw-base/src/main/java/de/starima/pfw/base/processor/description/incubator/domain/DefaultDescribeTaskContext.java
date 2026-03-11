package de.starima.pfw.base.processor.description.incubator.domain;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.domain.DefaultTaskContext;
import de.starima.pfw.base.processor.description.incubator.api.IConstructionManager;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Field;

/**
 * Standard-Implementierung von {@link IDescribeTaskContext}.
 *
 * <p>Kapselt alle Eingaben für eine DESCRIBE-Operation:
 * sourceObject (Instance-Describe) oder sourceType (Blueprint-Describe),
 * optionales sourceField, ConstructionManager.
 */
@Getter
@Setter
@NoArgsConstructor
public class DefaultDescribeTaskContext extends DefaultTaskContext
        implements IDescribeTaskContext {

    @ProcessorParameter(description = "Das zu beschreibende Java-Objekt (Instance-Describe). " +
            "Null bei reinem Schema/Blueprint-Describe.",
            ignoreInitialization = true)
    private Object sourceObject;

    @ProcessorParameter(description = "Der zu beschreibende Java-Typ. " +
            "Bei Instance-Describe: sourceObject.getClass(). " +
            "Bei Blueprint-Describe: die Klasse direkt.",
            ignoreInitialization = true)
    private Class<?> sourceType;

    @ProcessorParameter(description = "Das Quell-Feld (optional, für feld-basierte Beschreibung).",
            ignoreInitialization = true)
    private Field sourceField;

    @ProcessorParameter(description = "ConstructionManager für diesen Build-Lauf.",
            ignoreInitialization = true)
    private IConstructionManager constructionManager;

    /**
     * Leitet sourceType ab, falls nur sourceObject gesetzt ist.
     */
    @Override
    public Class<?> getSourceType() {
        if (sourceType != null) return sourceType;
        return sourceObject != null ? sourceObject.getClass() : null;
    }
}