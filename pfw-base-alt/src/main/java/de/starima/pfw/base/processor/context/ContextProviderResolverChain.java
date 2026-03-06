package de.starima.pfw.base.processor.context;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.context.api.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Chain of Responsibility für die Auflösung des zuständigen ContextProviders.
 *
 * <p>Sammelt alle registrierten {@link IContextProviderResolver}-Beans (außer sich selbst)
 * und delegiert an den ersten, der sich zuständig erklärt.
 *
 * <p>Durch {@code @Primary} wird diese Chain als Standard-Resolver injiziert,
 * wenn ein {@link IContextProviderResolver} angefordert wird.
 *
 * <p>Folgt dem gleichen Pattern wie {@code TypeRefProviderChain} und
 * {@code DescriptorConfigProviderChain}.
 */
@Slf4j
@Primary
@Processor(description = "Chain of Responsibility für ContextProvider-Auflösung. " +
        "Delegiert an den ersten zuständigen Resolver.")
public class ContextProviderResolverChain extends AbstractProcessor implements IContextProviderResolver {

    private final List<IContextProviderResolver> resolvers;

    @Autowired
    public ContextProviderResolverChain(ObjectProvider<IContextProviderResolver> resolverProvider) {
        this.resolvers = resolverProvider.orderedStream()
                .filter(r -> r != this) // Sich selbst ausschließen
                .collect(Collectors.toList());
        log.info("ContextProviderResolverChain initialisiert mit {} Resolver(n): {}",
                resolvers.size(),
                resolvers.stream().map(r -> r.getClass().getSimpleName()).collect(Collectors.joining(", ")));
    }

    @Override
    public boolean isResponsibleFor(IContextCreationContext context) {
        return true; // Chain ist immer zuständig — delegiert intern
    }

    @Override
    public IRuntimeContextProviderProcessor resolve(IContextCreationContext context) {
        for (IContextProviderResolver resolver : resolvers) {
            if (resolver.isResponsibleFor(context)) {
                log.debug("Delegiere an Resolver: {}", resolver.getClass().getSimpleName());
                IRuntimeContextProviderProcessor provider = resolver.resolve(context);
                if (provider != null) {
                    return provider;
                }
            }
        }

        // Kein Resolver hat einen Provider gefunden — Fallback auf Parent-ContextProvider
        log.debug("Kein Resolver zuständig, Fallback auf Parent-ContextProvider");
        IProcessorContext parentCtx = context.getRuntimeContext();
        return parentCtx != null ? parentCtx.getContextProviderProcessor() : null;
    }
}
