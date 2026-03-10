package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceCreationContext;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceProvider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Erzeugt und extrahiert Map-Typen ({@code Map<K, V>}).
 *
 * <p>Bestimmt Key- und Value-Typ via Java Generics und ruft für jeden Entry
 * rekursiv {@code rootProvider.provide/extract()} für Key und Value auf.
 *
 * <p>Ersetzt die bisherige Logik in {@code DefaultMapValueFunction} und
 * {@code ProcessorMapValueFunction}.
 *
 * @see IInstanceProvider
 */
@Slf4j
@Getter
@Setter
@Order(25)
@Processor(
        description = """
                Erzeugt und extrahiert Map-Typen (Map<K, V>).
                Bestimmt Key- und Value-Typ via Generics und ruft pro Entry
                rekursiv die InstanceProviderChain auf.""",
        categories = {"incubator", "instanceProvider"},
        tags = {"map", "provide", "extract", "recursive"}
)
public class MapInstanceProvider extends AbstractProcessor implements IInstanceProvider {

    @Override
    public boolean isResponsibleFor(IInstanceCreationContext context) {
        Class<?> rawType = resolveRawType(context);
        if (rawType == null) return false;
        return Map.class.isAssignableFrom(rawType);
    }

    // =========================================================================
    // provide() — Hydration
    // =========================================================================

    @Override
    public Object provide(IInstanceCreationContext context) {
        Object parameterValue = context.getParameterValue();
        if (parameterValue == null) return null;

        if (!(parameterValue instanceof Map<?, ?> sourceMap)) {
            log.warn("MapInstanceProvider.provide(): parameterValue ist kein Map, sondern: {}",
                    parameterValue.getClass().getSimpleName());
            return null;
        }

        // 1. Key- und Value-Typ bestimmen
        Type[] typeArgs = resolveMapTypeArguments(context);
        Type keyType = typeArgs != null ? typeArgs[0] : null;
        Type valueType = typeArgs != null ? typeArgs[1] : null;

        // 2. Für jeden Entry Key und Value rekursiv provide()
        Map<Object, Object> result = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
            // Key
            DefaultInstanceCreationContext keyCtx = new DefaultInstanceCreationContext(context);
            keyCtx.setParameterValue(entry.getKey());
            keyCtx.setTypeToResolve(keyType != null ? keyType
                    : (entry.getKey() != null ? entry.getKey().getClass() : null));
            keyCtx.setFieldToResolve(null);

            // Value
            DefaultInstanceCreationContext valueCtx = new DefaultInstanceCreationContext(context);
            valueCtx.setParameterValue(entry.getValue());
            valueCtx.setTypeToResolve(valueType != null ? valueType
                    : (entry.getValue() != null ? entry.getValue().getClass() : null));
            valueCtx.setFieldToResolve(null);

            Object providedKey = context.getRootProvider().provide(keyCtx);
            Object providedValue = context.getRootProvider().provide(valueCtx);

            if (providedKey != null) {
                result.put(providedKey, providedValue);
            }
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

        if (!(objectToResolve instanceof Map<?, ?> sourceMap)) {
            log.warn("MapInstanceProvider.extract(): objectToResolve ist kein Map, sondern: {}",
                    objectToResolve.getClass().getSimpleName());
            return null;
        }

        // Für jeden Entry Key und Value rekursiv extract()
        Map<Object, Object> result = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
            if (entry.getKey() == null) continue;

            DefaultInstanceCreationContext keyCtx = new DefaultInstanceCreationContext(context);
            keyCtx.setObjectToResolve(entry.getKey());
            keyCtx.setTypeToResolve(entry.getKey().getClass());
            keyCtx.setFieldToResolve(null);

            DefaultInstanceCreationContext valueCtx = new DefaultInstanceCreationContext(context);
            valueCtx.setObjectToResolve(entry.getValue());
            if (entry.getValue() != null) {
                valueCtx.setTypeToResolve(entry.getValue().getClass());
            }
            valueCtx.setFieldToResolve(null);

            Object rawKey = context.getRootProvider().extract(keyCtx);
            Object rawValue = entry.getValue() != null
                    ? context.getRootProvider().extract(valueCtx)
                    : null;

            if (rawKey != null) {
                result.put(rawKey, rawValue);
            }
        }

        return result;
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    /**
     * Ermittelt Key- und Value-Typ aus dem ParameterizedType (z.B. Map<String, IProcessor>).
     * Gibt ein Array mit [keyType, valueType] zurück, oder null falls nicht bestimmbar.
     */
    private Type[] resolveMapTypeArguments(IInstanceCreationContext context) {
        Type genericType = null;

        if (context.getFieldToResolve() != null) {
            genericType = context.getFieldToResolve().getGenericType();
        } else if (context.getTypeToResolve() instanceof ParameterizedType) {
            genericType = context.getTypeToResolve();
        }

        if (genericType instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length == 2) {
                return args;
            }
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
