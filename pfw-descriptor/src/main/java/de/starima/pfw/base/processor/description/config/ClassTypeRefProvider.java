package de.starima.pfw.base.processor.description.config;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.config.api.ITypeRef;
import de.starima.pfw.base.processor.description.config.api.ITypeRefProvider;
import de.starima.pfw.base.processor.description.config.api.ITypeResolutionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Order(Ordered.LOWEST_PRECEDENCE)
@Processor(description = "Erzeugt eine generische ITypeRef fÃ¼r jeden einfachen Klassentyp. Dient als Fallback.")
public class ClassTypeRefProvider extends AbstractProcessor implements ITypeRefProvider {

    @Override
    public boolean isResponsibleFor(ITypeResolutionContext context) {
        return context.getTypeToResolve() instanceof Class;
    }

    @Override
    public ITypeRef provide(ITypeResolutionContext context) {
        Class<?> clazz = (Class<?>) context.getTypeToResolve();
        return DefaultTypeRef.builder()
                .kind(ITypeRef.Kind.CLASS)
                .rawTypeName(clazz.getName())
                .typeSignature(clazz.getSimpleName())
                // Setzt keine speziellen Flags, da es ein generischer Typ ist
                .build();
    }
}