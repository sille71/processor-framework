package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.api.IValueFunction;
import de.starima.pfw.base.processor.description.api.IValueFunctionResolver;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceCreationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Chain of Responsibility für die Auflösung der zuständigen {@link IValueFunction}.
 *
 * <p>Ersetzt die statische Methode {@code ProcessorUtils.createValueFunctionForContext()}
 * durch eine formale, erweiterbare Chain — konsistent mit allen anderen Chains
 * im Framework (TypeRefProvider, ConfigProvider, ContextProviderResolver, InstanceProvider).
 *
 * <p><b>Resolution-Reihenfolge:</b>
 * <ol>
 *   <li>{@code AnnotationValueFunctionResolver} (@Order 10) — explizit konfiguriert</li>
 *   <li>{@code DescriptorValueFunctionResolver} (@Order 20) — aus dem Descriptor-System</li>
 *   <li>{@code DynamicValueFunctionResolver} (@Order 30) — dynamische Suche</li>
 * </ol>
 *
 * <p>Durch {@code @Primary} wird diese Chain als Standard-Resolver injiziert.
 *
 * <p><b>Keine Komposition:</b> Im Gegensatz zur bisherigen
 * {@code createValueFunctionForContext()} wird hier KEINE elementFunction/keyFunction
 * in die gefundene ValueFunction injiziert. Die Komposition (Iteration über
 * Collection-Elemente, Map-Keys/Values) ist Aufgabe der InstanceProvider.
 */
@Slf4j
@Primary
@Processor(
        description = "Chain of Responsibility für ValueFunction-Auflösung. " +
                "Ersetzt ProcessorUtils.createValueFunctionForContext(). " +
                "Delegiert an: AnnotationVFResolver → DescriptorVFResolver → DynamicVFResolver. " +
                "Keine elementFunction/keyFunction-Komposition — das machen die InstanceProvider.",
        categories = {"incubator", "chain"},
        tags = {"valueFunction", "resolver", "chain"}
)
public class ValueFunctionResolverChain extends AbstractProcessor implements IValueFunctionResolver {

    private final List<IValueFunctionResolver> resolvers;

    @Autowired
    public ValueFunctionResolverChain(ObjectProvider<IValueFunctionResolver> resolverBeans) {
        this.resolvers = resolverBeans.orderedStream()
                .filter(r -> r != this)
                .collect(Collectors.toList());

        log.info("ValueFunctionResolverChain initialisiert mit {} Resolver(n): {}",
                resolvers.size(),
                resolvers.stream()
                        .map(r -> r.getClass().getSimpleName())
                        .collect(Collectors.joining(", ")));
    }

    @Override
    public boolean isResponsibleFor(IInstanceCreationContext context) {
        return true; // Chain ist immer zuständig
    }

    @SuppressWarnings("rawtypes")
    @Override
    public IValueFunction resolve(IInstanceCreationContext context) {
        for (IValueFunctionResolver resolver : resolvers) {
            if (resolver.isResponsibleFor(context)) {
                IValueFunction result = resolver.resolve(context);
                if (result != null) {
                    log.trace("ValueFunction aufgelöst durch {}: {}",
                            resolver.getClass().getSimpleName(),
                            result.getClass().getSimpleName());
                    return result;
                }
            }
        }

        log.debug("Keine ValueFunction gefunden für Typ: {}, Feld: {}",
                context.getTypeToResolve(),
                context.getFieldToResolve() != null ? context.getFieldToResolve().getName() : "?");
        return null;
    }
}