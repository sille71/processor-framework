package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.incubator.api.IConstructSession;
import de.starima.pfw.base.processor.description.incubator.domain.IConstructSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Standard-Implementierung von {@link IConstructSession}.
 *
 * <p>Kapselt das Ergebnis einer construct-Operation:
 * <ul>
 *   <li>{@code root} — das erzeugte Root-Objekt (typischerweise IProcessor)</li>
 *   <li>{@code source} — die Eingabe-Quelle (beanParameterMap, rootBeanId, runtimeContext)</li>
 * </ul>
 *
 * <p>Das Root-Objekt entsteht durch den provide-Pfad der InstanceProviderChain:
 * {@code chain.provide(forProvide(clazz, rootBeanId, chain, ctx))}
 *
 * @param <T> Typ des erzeugten Root-Objekts (typischerweise IProcessor oder eine Subklasse)
 */
@Slf4j
@Getter
@Processor(
        description = "Ergebnis einer startConstruct()-Operation. " +
                "Kapselt das erzeugte Root-Objekt und die Eingabe-Quelle.",
        categories = {"incubator", "session"},
        tags = {"construct", "session", "provide", "root"}
)
public class DefaultConstructSession<T> extends AbstractProcessor implements IConstructSession<T> {

    private final T root;
    private final IConstructSource source;

    public DefaultConstructSession(T root, IConstructSource source) {
        this.root = root;
        this.source = source;
    }

    @Override
    public T getRoot() {
        return root;
    }

    @Override
    public IConstructSource getSource() {
        return source;
    }
}