package de.starima.pfw.base.processor.description.config;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.config.api.ITypeRef;
import de.starima.pfw.base.processor.description.config.api.ITypeRefProvider;
import de.starima.pfw.base.processor.description.config.api.ITypeResolutionContext;
import de.starima.pfw.base.util.ProcessorUtils;

@Processor(description = "Erkennt Klassen mit der @ValueObject-Annotation.")
public class ValueObjectTypeRefProvider extends AbstractProcessor implements ITypeRefProvider {

    @Override
    public boolean isResponsibleFor(ITypeResolutionContext context) {
        return (context.getTypeToResolve() instanceof Class) && ProcessorUtils.isConsideredValueObject((Class<?>) context.getTypeToResolve());
    }

    @Override
    public ITypeRef provide(ITypeResolutionContext context) {
        Class<?> clazz = (Class<?>) context.getTypeToResolve();
        return DefaultTypeRef.builder()
                .kind(ITypeRef.Kind.CLASS)
                .rawTypeName(clazz.getName())
                .typeSignature(clazz.getSimpleName())
                .valueObject(true)
                .build();
    }
}