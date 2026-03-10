package de.starima.pfw.base.processor.description.incubator;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.*;

/**
 * Erzeugt und extrahiert Map-Typen ({@code Map<K, V>}).
 *
 * <p>Vereinheitlicht die bisherige Logik aus drei separaten ValueFunctions:
 * <ul>
 *   <li>{@code DefaultMapValueFunction} — generischer Fall mit keyElementFunction + valueElementFunction</li>
 *   <li>{@code SimpleMapValueFunction} — Spezialfall Map&lt;String, String&gt;</li>
 *   <li>{@code ProcessorMapValueFunction} — deaktivierter Spezialfall für Map&lt;String, IProcessor&gt;</li>
 * </ul>
 *
 * <p><b>Kernkonzept:</b> Key und Value werden als <b>zwei separate Rekursionspfade</b>
 * über die InstanceProviderChain aufgelöst. Damit kann eine {@code Map<String, IProcessor>}
 * genauso behandelt werden wie eine {@code Map<String, String>} — der Key geht zum
 * ScalarInstanceProvider, der Value zum ProcessorInstanceProvider bzw. ScalarInstanceProvider.
 *
 * <h3>provide() — Hydration</h3>
 * <ol>
 *   <li>Input als Map interpretieren (direkte Map oder JSON-String)</li>
 *   <li>Key- und Value-Typ via {@code ParameterizedType} bestimmen</li>
 *   <li>Für jeden Entry:
 *       <ul>
 *         <li>keyContext: {@code typeToResolve = keyType, parameterValue = rawKey}</li>
 *         <li>valueContext: {@code typeToResolve = valueType, parameterValue = rawValue}</li>
 *         <li>Beide rekursiv über {@code rootProvider.provide()} auflösen</li>
 *       </ul>
 *   </li>
 *   <li>Ergebnis als {@code LinkedHashMap} (geordnet) zurückgeben</li>
 * </ol>
 *
 * <h3>extract() — Dehydration</h3>
 * <ol>
 *   <li>Für jeden Entry der lebendigen Map:
 *       <ul>
 *         <li>Key und Value rekursiv über {@code rootProvider.extract()} serialisieren</li>
 *       </ul>
 *   </li>
 *   <li>Ergebnis als Map von Raw-Werten zurückgeben</li>
 * </ol>
 *
 * <h3>Abgrenzung zu Collections:</h3>
 * <p>Im Gegensatz zum {@code CollectionInstanceProvider}, der nur <b>eine</b> elementFunction
 * hat, arbeitet der MapInstanceProvider mit <b>zwei</b> getrennten Rekursionspfaden:
 * einem für den Key-Typ und einem für den Value-Typ.
 */
@Slf4j
@Getter
@Setter
@Order(25) // Gleiche Priorität wie CollectionInstanceProvider
@Processor(
        description = "Erzeugt und extrahiert Map-Typen (Map<K, V>). " +
                "Key und Value werden als separate Rekursionspfade über die InstanceProviderChain aufgelöst. " +
                "Vereinheitlicht DefaultMapValueFunction, SimpleMapValueFunction und ProcessorMapValueFunction.",
        categories = {"incubator", "instanceProvider"},
        tags = {"map", "provide", "extract", "recursive", "key", "value"}
)
public class MapInstanceProvider extends AbstractProcessor implements IInstanceProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // =========================================================================
    // isResponsibleFor
    // =========================================================================

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

        // 1. Bereits eine Map? Direkt verwenden. Ansonsten aus JSON/String konvertieren.
        Map<?, ?> rawMap = convertInputToMap(parameterValue);
        if (rawMap == null || rawMap.isEmpty()) {
            log.trace("Map-Input ist null oder leer für Feld {}",
                    context.getFieldToResolve() != null ? context.getFieldToResolve().getName() : "?");
            return rawMap; // leere Map oder null durchreichen
        }

        // 2. Key- und Value-Typ bestimmen
        GenericMapTypes mapTypes = resolveGenericMapTypes(context);

        // 3. Für jeden Entry: Key und Value rekursiv auflösen
        Map<Object, Object> result = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            Object rawKey = entry.getKey();
            Object rawValue = entry.getValue();

            // Key-Context aufbauen
            DefaultInstanceCreationContext keyCtx = createEntryContext(context, mapTypes.keyType(), rawKey);
            Object resolvedKey = context.getRootProvider().provide(keyCtx);

            // Value-Context aufbauen
            DefaultInstanceCreationContext valueCtx = createEntryContext(context, mapTypes.valueType(), rawValue);
            Object resolvedValue = context.getRootProvider().provide(valueCtx);

            if (resolvedKey != null) {
                result.put(resolvedKey, resolvedValue);
            } else {
                log.warn("Key konnte nicht aufgelöst werden: {} (Typ: {})", rawKey, mapTypes.keyType());
            }
        }

        log.debug("Map mit {} Entries erzeugt (Feld: {})", result.size(),
                context.getFieldToResolve() != null ? context.getFieldToResolve().getName() : "?");
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
            log.warn("extract() erwartet eine Map, bekam: {}", objectToResolve.getClass().getName());
            return null;
        }

        if (sourceMap.isEmpty()) {
            return new LinkedHashMap<>();
        }

        // Key- und Value-Typ bestimmen
        GenericMapTypes mapTypes = resolveGenericMapTypes(context);

        // Für jeden Entry: Key und Value rekursiv extrahieren
        Map<Object, Object> rawMap = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            // Key extrahieren
            DefaultInstanceCreationContext keyCtx = createExtractionEntryContext(context, mapTypes.keyType(), key);
            Object rawKey = context.getRootProvider().extract(keyCtx);

            // Value extrahieren
            DefaultInstanceCreationContext valueCtx = createExtractionEntryContext(context, mapTypes.valueType(), value);
            Object rawValue = context.getRootProvider().extract(valueCtx);

            if (rawKey != null) {
                rawMap.put(rawKey, rawValue);
            }
        }

        return rawMap;
    }

    // =========================================================================
    // Generics-Auflösung
    // =========================================================================

    /**
     * Löst die Key- und Value-Typen einer Map aus den Generics auf.
     *
     * <p>Für {@code Map<String, IProcessor>} gibt dies zurück:
     * {@code keyType = String.class, valueType = IProcessor.class}
     *
     * <p>Fallback auf {@code Object.class}, wenn keine Generics-Info verfügbar.
     */
    private GenericMapTypes resolveGenericMapTypes(IInstanceCreationContext context) {
        Type genericType = null;

        // Priorität 1: Vom Feld (hat die vollständige Generics-Information)
        if (context.getFieldToResolve() != null) {
            genericType = context.getFieldToResolve().getGenericType();
        }

        // Priorität 2: Vom typeToResolve (falls es ein ParameterizedType ist)
        if (genericType == null) {
            genericType = context.getTypeToResolve();
        }

        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            if (typeArgs.length == 2) {
                return new GenericMapTypes(
                        resolveTypeArgument(typeArgs[0]),
                        resolveTypeArgument(typeArgs[1])
                );
            }
        }

        log.trace("Keine Generics-Info für Map-Feld {} verfügbar, verwende Object/Object",
                context.getFieldToResolve() != null ? context.getFieldToResolve().getName() : "?");
        return new GenericMapTypes(Object.class, Object.class);
    }

    /**
     * Löst ein einzelnes Type-Argument auf.
     *
     * <p>Behandelt sowohl einfache {@code Class<?>} als auch verschachtelte
     * {@code ParameterizedType} (z.B. {@code Map<String, List<IProcessor>>}).
     */
    private Type resolveTypeArgument(Type typeArgument) {
        if (typeArgument instanceof Class<?>) {
            return typeArgument;
        }
        if (typeArgument instanceof ParameterizedType pt) {
            // Für verschachtelte Generics (z.B. List<IProcessor>):
            // Wir geben den vollständigen ParameterizedType zurück,
            // damit der nächste Provider (CollectionInstanceProvider) die Generics weiter auflösen kann.
            return pt;
        }
        // Wildcard, TypeVariable, etc. → Fallback
        return Object.class;
    }

    // =========================================================================
    // Context-Erzeugung für Entries
    // =========================================================================

    /**
     * Erstellt einen Kind-Context für provide() eines einzelnen Map-Entry-Elements (Key oder Value).
     */
    private DefaultInstanceCreationContext createEntryContext(
            IInstanceCreationContext parentContext, Type elementType, Object parameterValue) {

        DefaultInstanceCreationContext childCtx = new DefaultInstanceCreationContext(
                (IInstanceCreationContext) parentContext);
        childCtx.setTypeToResolve(elementType);
        childCtx.setParameterValue(parameterValue);
        childCtx.setObjectToResolve(null);
        childCtx.setFieldToResolve(null); // Map-Entries haben kein eigenes Feld
        childCtx.setProcessorParameter(null);
        return childCtx;
    }

    /**
     * Erstellt einen Kind-Context für extract() eines einzelnen Map-Entry-Elements (Key oder Value).
     */
    private DefaultInstanceCreationContext createExtractionEntryContext(
            IInstanceCreationContext parentContext, Type elementType, Object value) {

        DefaultInstanceCreationContext childCtx = new DefaultInstanceCreationContext(
                (IInstanceCreationContext) parentContext);
        childCtx.setTypeToResolve(elementType);
        childCtx.setObjectToResolve(value);
        childCtx.setParameterValue(null);
        childCtx.setFieldToResolve(null);
        childCtx.setProcessorParameter(null);
        return childCtx;
    }

    // =========================================================================
    // Input-Konvertierung
    // =========================================================================

    /**
     * Konvertiert verschiedene Input-Formate in eine Map.
     *
     * <p>Akzeptierte Formate:
     * <ul>
     *   <li>Direkte {@code Map<?, ?>}</li>
     *   <li>JSON-String (über Jackson ObjectMapper)</li>
     *   <li>Komma-separierter String mit Key;Value-Paaren
     *       (Legacy-Format von ProcessorMapValueFunction)</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private Map<?, ?> convertInputToMap(Object input) {
        if (input instanceof Map) {
            return (Map<?, ?>) input;
        }

        if (input instanceof String stringInput) {
            String trimmed = stringInput.trim();
            if (trimmed.isEmpty()) return null;

            // Versuch 1: JSON
            if (trimmed.startsWith("{")) {
                try {
                    return OBJECT_MAPPER.readValue(trimmed, Map.class);
                } catch (Exception e) {
                    log.trace("Input ist kein gültiger JSON-Map-String, versuche Legacy-Format: {}", e.getMessage());
                }
            }

            // Versuch 2: Legacy "key1;value1,key2;value2" Format (aus ProcessorMapValueFunction)
            if (trimmed.contains(";")) {
                return parseLegacyMapString(trimmed);
            }

            // Versuch 3: Jackson convertValue (für andere Formate)
            try {
                return OBJECT_MAPPER.convertValue(input, Map.class);
            } catch (Exception e) {
                log.debug("Konnte String '{}' nicht in Map konvertieren: {}", trimmed, e.getMessage());
            }
        }

        log.warn("Unbekanntes Map-Input-Format: {} (Typ: {})", input,
                input != null ? input.getClass().getName() : "null");
        return null;
    }

    /**
     * Parst das Legacy-Format "key1;value1,key2;value2" aus der ProcessorMapValueFunction.
     *
     * <p>Das Format verwendet:
     * <ul>
     *   <li>{@code ,} als Entry-Trenner</li>
     *   <li>{@code ;} als Key-Value-Trenner</li>
     * </ul>
     *
     * <p>Beispiel: {@code "source;csvReaderProcessor,target;dbWriterProcessor"}
     * wird zu {@code {"source": "csvReaderProcessor", "target": "dbWriterProcessor"}}
     */
    private Map<String, String> parseLegacyMapString(String input) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String pair : input.split(",")) {
            String trimmedPair = pair.trim();
            if (trimmedPair.isEmpty()) continue;

            String[] keyValue = trimmedPair.split(";", 2);
            if (keyValue.length == 2) {
                result.put(keyValue[0].trim(), keyValue[1].trim());
            } else {
                log.warn("Ungültiges Key;Value-Paar in Map-String: '{}'", trimmedPair);
            }
        }
        return result.isEmpty() ? null : result;
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    private Class<?> resolveRawType(IInstanceCreationContext context) {
        if (context.getTypeToResolve() instanceof Class<?> clazz) return clazz;
        if (context.getTypeToResolve() instanceof ParameterizedType pt
                && pt.getRawType() instanceof Class<?> raw) return raw;
        if (context.getFieldToResolve() != null) return context.getFieldToResolve().getType();
        if (context.getObjectToResolve() != null) return context.getObjectToResolve().getClass();
        return null;
    }

    // =========================================================================
    // Interne Typen
    // =========================================================================

    /**
     * Gebündelte Generics-Information einer Map: Key-Typ und Value-Typ.
     *
     * <p>Beispiele:
     * <ul>
     *   <li>{@code Map<String, String>} → {@code keyType=String.class, valueType=String.class}</li>
     *   <li>{@code Map<String, IProcessor>} → {@code keyType=String.class, valueType=IProcessor.class}</li>
     *   <li>{@code Map<String, List<Integer>>} → {@code keyType=String.class, valueType=ParameterizedType(List<Integer>)}</li>
     * </ul>
     *
     * @param keyType   Der Typ der Map-Keys (Class oder ParameterizedType)
     * @param valueType Der Typ der Map-Values (Class oder ParameterizedType)
     */
    record GenericMapTypes(Type keyType, Type valueType) {}
}