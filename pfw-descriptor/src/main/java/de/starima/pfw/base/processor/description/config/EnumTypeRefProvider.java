package de.starima.pfw.base.processor.description.config;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.config.api.ITypeRef;
import de.starima.pfw.base.processor.description.config.api.ITypeRefProvider;
import de.starima.pfw.base.processor.description.config.api.ITypeResolutionContext;
import org.springframework.core.annotation.Order;

@Processor(description = "Erkennt alle Enums und setzt das 'enum'-Flag.")
@Order(100) // Semantische Provider haben eine mittlere PrioritÃ¤t
public class EnumTypeRefProvider extends AbstractProcessor implements ITypeRefProvider {

    @Override
    public boolean isResponsibleFor(ITypeResolutionContext context) {
        return (context.getTypeToResolve() instanceof Class) && ((Class<?>) context.getTypeToResolve()).isEnum();
    }

    @Override
    public ITypeRef provide(ITypeResolutionContext context) {
        Class<?> clazz = (Class<?>) context.getTypeToResolve();
        return DefaultTypeRef.builder()
                .kind(ITypeRef.Kind.CLASS)
                .rawTypeName(clazz.getName())
                .typeSignature(clazz.getSimpleName())
                .anEnum(true) // Korrekt: Setzt das semantische Flag
                .build();
    }
}