package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.incubator.api.IConstructSession;
import de.starima.pfw.base.processor.description.incubator.domain.IConstructTaskContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Standard-Implementierung von {@link IConstructSession}.
 *
 * <p>Kapselt das Ergebnis einer construct-Operation:
 * <ul>
 *   <li>{@code root} — das erzeugte Root-Objekt (typischerweise IProcessor)</li>
 *   <li>{@code context} — der IConstructTaskContext (rootBeanId, targetType, runtimeContext)</li>
 * </ul>
 *
 * <p>Das Root-Objekt entsteht durch den provide-Pfad der InstanceProviderChain:
 * {@code chain.provide(forProvide(targetType, rootBeanId, chain, ctx))}
 *
 * @param <T> Typ des erzeugten Root-Objekts (typischerweise IProcessor oder eine Subklasse)
 */
@Slf4j
@Getter
@Processor(
        description = "Ergebnis einer startConstruct()-Operation. " +
                "Kapselt das erzeugte Root-Objekt und den IConstructTaskContext.",
        categories = {"incubator", "session"},
        tags = {"construct", "session", "provide", "root"}
)
public class DefaultConstructSession<T> extends AbstractProcessor implements IConstructSession<T> {

    @ProcessorParameter(description = "Das erzeugte Root-Objekt (typischerweise IProcessor oder eine Subklasse).",
            ignoreInitialization = true)
    private final T root;

    @ProcessorParameter(description = "Der IConstructTaskContext (rootBeanId, targetType, runtimeContext) dieser Construct-Session.",
            ignoreInitialization = true)
    private final IConstructTaskContext context;

    public DefaultConstructSession(T root, IConstructTaskContext context) {
        this.root = root;
        this.context = context;
    }

    @Override
    public T getRoot() {
        return root;
    }

    @Override
    public IConstructTaskContext getContext() {
        return context;
    }
}