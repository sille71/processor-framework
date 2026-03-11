package de.starima.pfw.base.processor.description.incubator.domain;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.domain.DefaultTaskContext;
import de.starima.pfw.base.processor.description.incubator.api.IConstructionManager;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Standard-Implementierung von {@link IConstructTaskContext}.
 *
 * <p>Kapselt alle Eingaben für eine CONSTRUCT-Operation:
 * rootBeanId, targetType, draftProcessor, ConstructionManager.
 *
 * <p>Die beanParameterMap wird standardmäßig aus dem RuntimeContext gelesen
 * ({@code context.getRuntimeContext().getContextMergedBeanParameterMap()}).
 */
@Getter
@Setter
@NoArgsConstructor
public class DefaultConstructTaskContext extends DefaultTaskContext
        implements IConstructTaskContext {

    @ProcessorParameter(description = "Die Root-BeanId des zu erzeugenden Prozessors. " +
            "Beispiel: \"csvReconService:instance1@instance\"")
    private String rootBeanId;

    @ProcessorParameter(description = "Der aufzulösende Ziel-Typ für typisierte Erzeugung. " +
            "Null = untypisiert (Object.class).",
            ignoreInitialization = true)
    private Class<?> targetType;

    @ProcessorParameter(description = "Vorab erzeugter Draft-Prozessor (optional). " +
            "Wird verwendet, wenn die Bean bereits existiert und nur initialisiert werden muss.",
            ignoreInitialization = true)
    private IProcessor draftProcessor;

    @ProcessorParameter(description = "ConstructionManager für diesen Build-Lauf.",
            ignoreInitialization = true)
    private IConstructionManager constructionManager;
}