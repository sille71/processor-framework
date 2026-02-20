package de.starima.pfw.base.processor.description.config;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.config.api.ITypeRef;
import de.starima.pfw.base.processor.description.config.api.ITypeRefProvider;
import de.starima.pfw.base.processor.description.config.api.ITypeResolutionContext;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

@Processor(description = "Erzeugt eine ITypeRef fÃ¼r Array-Typen wie String[] oder List<T>[].")
public class ArrayTypeRefProvider extends AbstractProcessor implements ITypeRefProvider {

    // Kein Zustand, keine injizierten Felder!

    @Override
    public boolean isResponsibleFor(ITypeResolutionContext context) {
        return context.getTypeToResolve() instanceof GenericArrayType || (context.getTypeToResolve() instanceof Class && ((Class<?>) context.getTypeToResolve()).isArray());
    }

    @Override
    public ITypeRef provide(ITypeResolutionContext context) {
        ITypeRefProvider rootProvider = context.getRootProvider();
        if (context.getRootProvider() == null) {
            throw new IllegalArgumentException("Ein Spezialist wie ArrayTypeRefProvider kann nicht ohne einen rootProvider aufgerufen werden.");
        }

        Type type = context.getTypeToResolve();

        final Type componentType = (type instanceof GenericArrayType)
                ? ((GenericArrayType) type).getGenericComponentType()
                : ((Class<?>) type).getComponentType();

        // 1. Erzeuge einen neuen Kind-Kontext fÃ¼r die rekursive Aufgabe.
        ITypeResolutionContext subContext = new DefaultTypeResolutionContext(context, componentType);
        // Delegiere die AuflÃ¶sung des Komponententyps zurÃ¼ck an den Root-Provider.
        ITypeRef componentRef = rootProvider.provide(subContext);

        return DefaultTypeRef.builder()
                .kind(ITypeRef.Kind.ARRAY)
                .rawTypeName(componentRef.getRawTypeName() + "[]")
                .typeSignature(componentRef.getTypeSignature() + "[]")
                .typeArgument(componentRef)
                .build();
    }
}