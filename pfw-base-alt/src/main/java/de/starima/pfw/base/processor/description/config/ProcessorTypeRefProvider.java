package de.starima.pfw.base.processor.description.config;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.config.api.ITypeRef;
import de.starima.pfw.base.processor.description.config.api.ITypeRefProvider;
import de.starima.pfw.base.processor.description.config.api.ITypeResolutionContext;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Processor(description = "Erkennt Typen, die IProcessor implementieren, und setzt das 'processor'-Flag in der ITypeRef.")
public class ProcessorTypeRefProvider extends AbstractProcessor implements ITypeRefProvider {

    @Override
    public boolean isResponsibleFor(ITypeResolutionContext context) {
        return (context.getTypeToResolve() instanceof Class) && ProcessorUtils.isConsideredProcessor((Class<?>) context.getTypeToResolve());
    }

    @Override
    public ITypeRef provide(ITypeResolutionContext context) {
        if (!isResponsibleFor(context)) {
            return null;
        }
        Class<?> clazz = (Class<?>) context.getTypeToResolve();

        return DefaultTypeRef.builder().kind(ITypeRef.Kind.CLASS).processor(true).typeSignature(clazz.getSimpleName()).rawTypeName(clazz.getName()).build();
    }
}