package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceCreationContext;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Chain of Responsibility für Objekt-Erzeugung und -Extraktion.
 *
 * <p>Sammelt alle registrierten {@link IInstanceProvider}-Beans (außer sich selbst)
 * und delegiert {@code provide()} und {@code extract()} an den ersten, der sich
 * für den gegebenen Kontext zuständig erklärt.
 *
 * <p>Durch {@code @Primary} wird diese Chain als Standard-InstanceProvider injiziert,
 * wenn ein {@link IInstanceProvider} angefordert wird. Der {@code rootProvider}
 * im {@link IInstanceCreationContext} zeigt immer auf diese Chain, damit
 * rekursive Aufrufe wieder durch die Chain laufen.
 *
 * <p>Folgt dem gleichen Pattern wie:
 * <ul>
 *   <li>{@code TypeRefProviderChain} — Typ-Auflösung</li>
 *   <li>{@code DescriptorConfigProviderChain} — Config-Auflösung</li>
 *   <li>{@code ContextProviderResolverChain} — Context-Auflösung</li>
 * </ul>
 *
 * @see IInstanceProvider
 */
@Slf4j
@Primary
@Processor(
        description = """
                Chain of Responsibility für Objekt-Erzeugung (provide) und -Extraktion (extract).
                Delegiert an den ersten zuständigen IInstanceProvider.
                Vereinheitlicht die bisherigen drei Erzeugungswege
                (ProcessorProvider, ValueFunction, BeanProvider) zu einem einzigen Einstiegspunkt.""",
        categories = {"incubator", "chain"},
        tags = {"provide", "extract", "hydration", "dehydration", "chain"}
)
public class InstanceProviderChain extends AbstractProcessor implements IInstanceProvider {

    private final List<IInstanceProvider> providers;

    @Autowired
    public InstanceProviderChain(ObjectProvider<IInstanceProvider> providerBeans) {
        this.providers = providerBeans.orderedStream()
                .filter(p -> p != this) // Sich selbst ausschließen
                .collect(Collectors.toList());

        log.info("InstanceProviderChain initialisiert mit {} Provider(n): {}",
                providers.size(),
                providers.stream()
                        .map(p -> p.getClass().getSimpleName())
                        .collect(Collectors.joining(", ")));
    }

    @Override
    public boolean isResponsibleFor(IInstanceCreationContext context) {
        return true; // Chain ist immer zuständig — delegiert intern
    }

    @Override
    public Object provide(IInstanceCreationContext context) {
        // Setze die Chain als rootProvider, damit rekursive Aufrufe hierher zurückkommen
        context.setRootProvider(this);

        for (IInstanceProvider provider : providers) {
            if (provider.isResponsibleFor(context)) {
                log.trace("provide: Delegiere an {} für Typ {}",
                        provider.getClass().getSimpleName(), context.getTypeToResolve());
                return provider.provide(context);
            }
        }

        log.warn("Kein IInstanceProvider zuständig für provide(). Typ: {}, ParameterValue: {}",
                context.getTypeToResolve(),
                context.getParameterValue() != null ? context.getParameterValue().getClass().getSimpleName() : "null");
        return null;
    }

    @Override
    public Object extract(IInstanceCreationContext context) {
        // Setze die Chain als rootProvider, damit rekursive Aufrufe hierher zurückkommen
        context.setRootProvider(this);

        for (IInstanceProvider provider : providers) {
            if (provider.isResponsibleFor(context)) {
                log.trace("extract: Delegiere an {} für Objekt {}",
                        provider.getClass().getSimpleName(),
                        context.getObjectToResolve() != null
                                ? context.getObjectToResolve().getClass().getSimpleName()
                                : "null");
                return provider.extract(context);
            }
        }

        log.warn("Kein IInstanceProvider zuständig für extract(). Objekt: {}",
                context.getObjectToResolve() != null
                        ? context.getObjectToResolve().getClass().getSimpleName()
                        : "null");
        return null;
    }
}
