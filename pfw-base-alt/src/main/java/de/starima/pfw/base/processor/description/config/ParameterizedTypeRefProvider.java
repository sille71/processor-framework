package de.starima.pfw.base.processor.description.config;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.config.api.ITypeRef;
import de.starima.pfw.base.processor.description.config.api.ITypeRefProvider;
import de.starima.pfw.base.processor.description.config.api.ITypeResolutionContext;
import org.springframework.core.annotation.Order;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Order(10) // Sehr hohe PrioritÃ¤t, da es ein struktureller Typ ist
@Processor(description = "Erzeugt eine ITypeRef fÃ¼r parametrisierte Typen wie List<String>.")
public class ParameterizedTypeRefProvider extends AbstractProcessor implements ITypeRefProvider {

    @Override
    public boolean isResponsibleFor(ITypeResolutionContext context) {
        return context.getTypeToResolve() instanceof ParameterizedType;
    }

    @Override
    public ITypeRef provide(ITypeResolutionContext context) {
        ITypeRefProvider rootProvider = context.getRootProvider();
        if (rootProvider == null) {
            throw new IllegalArgumentException("Ein Spezialist wie ParameterizedTypeRefProvider kann nicht ohne einen rootProvider aufgerufen werden.");
        }
        Type type = context.getTypeToResolve();

        ParameterizedType pType = (ParameterizedType) type;
        Class<?> rawClass = (Class<?>) pType.getRawType();

        // Rekursiver Aufruf fÃ¼r jedes Typ-Argument
        List<ITypeRef> typeArguments = Arrays.stream(pType.getActualTypeArguments())
                .map(arg -> rootProvider.provide(new DefaultTypeResolutionContext(context, arg)))
                .collect(Collectors.toList());

        // Erzeuge eine saubere Signatur, z.B. "Map<String, Integer>"
        String typeSignature = rawClass.getSimpleName() + "<" +
                typeArguments.stream()
                        .map(ITypeRef::getTypeSignature)
                        .collect(Collectors.joining(", ")) +
                ">";

        return DefaultTypeRef.builder()
                .kind(ITypeRef.Kind.PARAMETERIZED)
                .rawTypeName(rawClass.getName())
                .typeSignature(typeSignature)
                .typeArguments(typeArguments) // Lombok @Singular kann hier nicht direkt genutzt werden
                .build();
    }
}