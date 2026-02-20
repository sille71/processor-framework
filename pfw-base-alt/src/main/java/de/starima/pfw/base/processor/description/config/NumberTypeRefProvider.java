package de.starima.pfw.base.processor.description.config;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.config.api.ITypeRef;
import de.starima.pfw.base.processor.description.config.api.ITypeRefProvider;
import de.starima.pfw.base.processor.description.config.api.ITypeResolutionContext;
import org.springframework.core.annotation.Order;

@Order(100) // Standard-PrioritÃ¤t fÃ¼r semantische Provider
@Processor(description = "Erkennt alle Subklassen von java.lang.Number und setzt das 'numeric'-Flag.")
public class NumberTypeRefProvider extends AbstractProcessor implements ITypeRefProvider {

    @Override
    public boolean isResponsibleFor(ITypeResolutionContext context) {
        return (context.getTypeToResolve() instanceof Class) && Number.class.isAssignableFrom((Class<?>) context.getTypeToResolve());
    }

    @Override
    public ITypeRef provide(ITypeResolutionContext context) {
        Class<?> clazz = (Class<?>) context.getTypeToResolve();
        return DefaultTypeRef.builder()
                .kind(ITypeRef.Kind.CLASS)
                .rawTypeName(clazz.getName())
                .typeSignature(clazz.getSimpleName())
                .numeric(true)
                .build();
    }
}