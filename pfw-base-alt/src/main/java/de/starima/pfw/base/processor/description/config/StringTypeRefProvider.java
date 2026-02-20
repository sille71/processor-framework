package de.starima.pfw.base.processor.description.config;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.config.api.ITypeRef;
import de.starima.pfw.base.processor.description.config.api.ITypeRefProvider;
import de.starima.pfw.base.processor.description.config.api.ITypeResolutionContext;
import org.springframework.core.annotation.Order;

@Order(100) // Standard-PrioritÃ¤t fÃ¼r semantische Provider
@Processor(description = "Erkennt java.lang.String und setzt das 'string'-Flag.")
public class StringTypeRefProvider extends AbstractProcessor implements ITypeRefProvider {

    @Override
    public boolean isResponsibleFor(ITypeResolutionContext context) {
        return String.class.equals(context.getTypeToResolve());
    }

    @Override
    public ITypeRef provide(ITypeResolutionContext context) {
        return DefaultTypeRef.builder()
                .kind(ITypeRef.Kind.CLASS)
                .rawTypeName(String.class.getName())
                .typeSignature(String.class.getSimpleName())
                .string(true)
                .build();
    }
}