package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceCreationContext;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceProvider;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Erzeugt und extrahiert Collection-Typen (List, Set) und Arrays.
 *
 * <p>Bestimmt den Element-Typ via Java Generics ({@code ParameterizedType})
 * und ruft für jedes Element rekursiv {@code rootProvider.provide/extract(elementContext)} auf.
 *
 * <p>Ersetzt die bisherige Logik in {@code DefaultListValueFunction} und
 * {@code DefaultArrayValueFunction}, einschließlich der rekursiven
 * {@code elementFunction}-Setzung in {@code ProcessorUtils.createValueFunctionForContext()}.
 *
 * @see IInstanceProvider
 */
@Slf4j
@Getter
@Setter
@Order(25)
@Processor(
        description = """
                Erzeugt und extrahiert Collection-Typen (List, Set) und Arrays.
                Bestimmt den Element-Typ via Generics und ruft pro Element rekursiv
                die InstanceProviderChain auf.""",
        categories = {"incubator", "instanceProvider"},
        tags = {"collection", "list", "array", "provide", "extract", "recursive"}
)
public class CollectionInstanceProvider extends AbstractProcessor implements IInstanceProvider {

    @Override
    public boolean isResponsibleFor(IInstanceCreationContext context) {
        Class<?> rawType = resolveRawType(context);
        if (rawType == null) return false;
        return Collection.class.isAssignableFrom(rawType) || rawType.isArray();
    }

    // =========================================================================
    // provide() — Hydration
    // =========================================================================

    @Override
    public Object provide(IInstanceCreationContext context) {
        Object parameterValue = context.getParameterValue();
        if (parameterValue == null) return null;

        // 1. Element-Typ bestimmen
        Class<?> elementType = resolveElementType(context);

        // 2. parameterValue als Collection interpretieren
        Collection<?> sourceCollection;
        if (parameterValue instanceof Collection<?> col) {
            sourceCollection = col;
        } else if (parameterValue instanceof String str) {
            // Komma-separierter String als Fallback
            sourceCollection = Arrays.asList(str.split(","));
        } else {
            log.warn("Unbekanntes Collection-Format für parameterValue: {}",
                    parameterValue.getClass().getSimpleName());
            return null;
        }

        // 3. Für jedes Element rekursiv provide() aufrufen
        List<Object> result = new ArrayList<>();
        for (Object element : sourceCollection) {
            DefaultInstanceCreationContext childCtx = new DefaultInstanceCreationContext(context);
            childCtx.setParameterValue(element);
            if (elementType != null) {
                childCtx.setTypeToResolve(elementType);
            } else if (element != null) {
                childCtx.setTypeToResolve(element.getClass());
            }
            childCtx.setFieldToResolve(null); // Kein Feld für Collection-Elemente

            Object provided = context.getRootProvider().provide(childCtx);
            if (provided != null) {
                result.add(provided);
            }
        }

        // 4. Zieltyp: Array oder Collection
        Class<?> rawType = resolveRawType(context);
        if (rawType != null && rawType.isArray()) {
            return result.toArray();
        }
        return result;
    }

    // =========================================================================
    // extract() — Dehydration
    // =========================================================================

    @Override
    public Object extract(IInstanceCreationContext context) {
        Object objectToResolve = context.getObjectToResolve();
        if (objectToResolve == null) return null;

        // 1. objectToResolve als Collection/Array interpretieren
        Iterable<?> iterable;
        if (objectToResolve instanceof Collection<?> col) {
            iterable = col;
        } else if (objectToResolve.getClass().isArray()) {
            iterable = Arrays.asList((Object[]) objectToResolve);
        } else {
            log.warn("Unbekanntes Collection-Objekt für extract(): {}",
                    objectToResolve.getClass().getSimpleName());
            return null;
        }

        // 2. Für jedes Element rekursiv extract() aufrufen
        List<Object> rawValues = new ArrayList<>();
        for (Object element : iterable) {
            if (element == null) continue;

            DefaultInstanceCreationContext childCtx = new DefaultInstanceCreationContext(context);
            childCtx.setObjectToResolve(element);
            childCtx.setTypeToResolve(element.getClass());
            childCtx.setFieldToResolve(null);

            Object rawValue = context.getRootProvider().extract(childCtx);
            if (rawValue != null) {
                rawValues.add(rawValue);
            }
        }

        return rawValues;
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    private Class<?> resolveElementType(IInstanceCreationContext context) {
        // Über Feld-Generics
        if (context.getFieldToResolve() != null) {
            return ProcessorUtils.getElementType(context.getFieldToResolve().getGenericType());
        }
        // Über typeToResolve direkt (ParameterizedType)
        if (context.getTypeToResolve() instanceof ParameterizedType pt) {
            return ProcessorUtils.getElementType(pt);
        }
        return null;
    }

    private Class<?> resolveRawType(IInstanceCreationContext context) {
        if (context.getTypeToResolve() instanceof Class<?> clazz) return clazz;
        if (context.getTypeToResolve() instanceof ParameterizedType pt
                && pt.getRawType() instanceof Class<?> raw) return raw;
        if (context.getFieldToResolve() != null) return context.getFieldToResolve().getType();
        if (context.getObjectToResolve() != null) return context.getObjectToResolve().getClass();
        return null;
    }
}
