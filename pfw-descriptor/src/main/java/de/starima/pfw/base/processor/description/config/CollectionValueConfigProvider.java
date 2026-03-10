package de.starima.pfw.base.processor.description.config;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfig;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfigCreationContext;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfigProvider;
import de.starima.pfw.base.processor.description.config.api.ITypeRef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

@Slf4j
@Order(20) // Hohe PrioritÃ¤t, damit er vor dem ValueObject-Provider geprÃ¼ft wird
@Processor(description = "Erzeugt eine ICollectionValueConfig fÃ¼r Typen wie List, Set oder Arrays.")
public class CollectionValueConfigProvider extends AbstractProcessor implements IDescriptorConfigProvider {

    @Override
    public boolean isResponsibleFor(IDescriptorConfigCreationContext context) {
        Type type = context.getTypeToResolve();
        if (type == null) return false;

        // Fall 1: Ist es ein Array? (z.B. String[] oder MyObject[])
        if (type instanceof Class && ((Class<?>) type).isArray()) {
            return true;
        }
        if (type instanceof GenericArrayType) {
            return true;
        }

        // Fall 2: Ist es eine Collection? (z.B. List<String> oder Set<MyObject>)
        if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();
            if (rawType instanceof Class) {
                return Collection.class.isAssignableFrom((Class<?>) rawType);
            }
        }

        // Fall 3: Ist es eine rohe Collection? (z.B. List ohne Generics)
        if (type instanceof Class) {
            return Collection.class.isAssignableFrom((Class<?>) type);
        }

        return false;
    }

    @Override
    public IDescriptorConfig provide(IDescriptorConfigCreationContext context) {
        if (!isResponsibleFor(context)) return null;

        Type type = context.getTypeToResolve();
        log.debug("CollectionValueConfigProvider ist zustÃ¤ndig fÃ¼r Typ: {}", type.getTypeName());

        // 1. Erzeuge die Container-Konfiguration fÃ¼r die Collection selbst.
        CollectionValueConfig collectionConfig = new CollectionValueConfig();

        // 2. Setze die TypeRef fÃ¼r die Collection (z.B. "list<string>").
        // Wir nutzen die bestehende TypeRef-Kette dafÃ¼r.
        DefaultTypeResolutionContext typeResolutionContext = new DefaultTypeResolutionContext();
        typeResolutionContext.setParentContext(context);
        typeResolutionContext.setRuntimeContext(context.getRuntimeContext());
        typeResolutionContext.setTypeToResolve(type);
        ITypeRef typeRef = context.getTypeRefRootProvider().provide(typeResolutionContext);

        collectionConfig.setTypeRef(typeRef);

        // 3. Ermittle den Typ der Elemente in der Collection.
        Type elementType = getElementType(type);

        // 4. Der rekursive Schritt: Erzeuge die Konfiguration fÃ¼r das Element.
        // Wir fragen die rootProvider-Kette, den passenden Provider fÃ¼r den Element-Typ zu finden.
        DefaultDescriptorConfigCreationContext subContext = new DefaultDescriptorConfigCreationContext(context);
        subContext.setTypeToResolve(elementType);
        subContext.setFieldToResolve(null); // Wichtig: Kontext fÃ¼r die Rekursion bereinigen
        subContext.setObjectToResolve(null);

        IDescriptorConfig elementDescriptor = context.getRootProvider().provide(subContext);

        // 5. Setze die Element-Konfiguration (typsicher).
        if (elementDescriptor != null) {
            elementDescriptor.asValueConfig().ifPresent(collectionConfig::setElementConfig);
        } else {
            log.warn("FÃ¼r den Element-Typ '{}' der Collection konnte keine IValueConfig erzeugt werden.", elementType.getTypeName());
        }

        return collectionConfig;
    }

    /**
     * Private Hilfsmethode, um den Typ der Elemente aus einem Collection- oder Array-Typ zu extrahieren.
     */
    private Type getElementType(Type collectionType) {
        // Fall 1: Parametrisierte Collection (z.B. List<String>)
        if (collectionType instanceof ParameterizedType) {
            Type[] typeArguments = ((ParameterizedType) collectionType).getActualTypeArguments();
            if (typeArguments.length > 0) {
                return typeArguments[0];
            }
        }

        // Fall 2: Array (z.B. String[])
        if (collectionType instanceof Class && ((Class<?>) collectionType).isArray()) {
            return ((Class<?>) collectionType).getComponentType();
        }
        if (collectionType instanceof GenericArrayType) {
            return ((GenericArrayType) collectionType).getGenericComponentType();
        }

        // Fallback fÃ¼r rohe Collections (z.B. List ohne Generics)
        log.debug("Typ '{}' ist eine rohe Collection. Element-Typ wird als Object angenommen.", collectionType.getTypeName());
        return Object.class;
    }
}