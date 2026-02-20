package de.starima.pfw.base.processor.description.config;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.config.api.ITypeRef;
import de.starima.pfw.base.processor.description.config.api.ITypeRefProvider;
import de.starima.pfw.base.processor.description.config.api.ITypeResolutionContext;
import org.springframework.core.annotation.Order;

@Order(100) // Standard-PrioritÃ¤t fÃ¼r semantische Provider
@Processor(description = "Erkennt Boolean/boolean und setzt das 'boolean'-Flag.")
public class BooleanTypeRefProvider extends AbstractProcessor implements ITypeRefProvider {

    @Override
    public boolean isResponsibleFor(ITypeResolutionContext context) {
        return Boolean.class.equals(context.getTypeToResolve()) || boolean.class.equals(context.getTypeToResolve());
    }

    @Override
    public ITypeRef provide(ITypeResolutionContext context) {
        Class<?> clazz = (context.getTypeToResolve() == boolean.class) ? boolean.class : Boolean.class;
        return DefaultTypeRef.builder()
                .kind(ITypeRef.Kind.CLASS)
                .rawTypeName(clazz.getName())
                .typeSignature(clazz.getSimpleName())
                .bool(true)
                .build();
    }
}