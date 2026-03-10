package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceCreationContext;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceProvider;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Erzeugt und extrahiert Collection-Typen ({@code List}, {@code Set}) und Arrays.
 *
 * <p>Vereinheitlicht die bisherige Logik aus:
 * <ul>
 *   <li>{@code DefaultListValueFunction} — Listen mit einer {@code elementFunction}</li>
 *   <li>{@code DefaultArrayValueFunction} — Arrays mit einer {@code elementFunction}</li>
 *   <li>{@code AbstractCollectionValueFunction} — gemeinsame Basislogik</li>
 * </ul>
 *
 * <p><b>Kernkonzept:</b> Der Element-Typ wird via Java Generics bestimmt.
 * Für jedes Element wird rekursiv {@code rootProvider.provide/extract(elementContext)}
 * aufgerufen. Der Element-Context hat {@code typeToResolve = elementType} — damit
 * entscheidet die Chain, welcher Provider für das Element zuständig ist.
 *
 * <p><b>Abgrenzung zum MapInstanceProvider:</b> Collections haben <b>einen</b>
 * Rekursionspfad (elementFunction), Maps haben <b>zwei</b> (keyFunction + valueFunction).
 *
 * <h3>provide() — Hydration</h3>
 * <ol>
 *   <li>Input interpretieren: direkte Collection, oder String (Komma-separiert via delimiter)</li>
 *   <li>Element-Typ via {@code ParameterizedType} bestimmen</li>
 *   <li>Für jedes Element: {@code rootProvider.provide(elementContext)} — REKURSION</li>
 *   <li>Ergebnis als List, Set oder Array zurückgeben (je nach Zieltyp)</li>
 * </ol>
 *
 * <h3>extract() — Dehydration</h3>
 * <ol>
 *   <li>Für jedes Element: {@code rootProvider.extract(elementContext)} — REKURSION</li>
 *   <li>Ergebnis als Liste von Raw-Werten zurückgeben</li>
 * </ol>
 */
@Slf4j
@Getter
@Setter
@Order(25) // Gleiche Priorität wie MapInstanceProvider
@Processor(
        description = "Erzeugt und extrahiert Collection-Typen (List, Set) und Arrays. " +
                "Bestimmt den Element-Typ via Generics und ruft pro Element rekursiv " +
                "die InstanceProviderChain auf. Vereinheitlicht DefaultListValueFunction und DefaultArrayValueFunction.",
        categories = {"incubator", "instanceProvider"},
        tags = {"collection", "list", "set", "array", "provide", "extract", "recursive"}
)
public class CollectionInstanceProvider extends AbstractProcessor implements IInstanceProvider {

    @ProcessorParameter(value = ",",
            description = "Trennzeichen für String-Inputs. " +
                    "Wird verwendet, wenn der parameterValue ein komma-separierter String ist.")
    private String delimiter = ",";

    // =========================================================================
    // isResponsibleFor
    // =========================================================================

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
        Type elementType = resolveElementType(context);

        // 2. Input in eine iterierbare Struktur konvertieren
        List<?> rawElements = convertInputToList(parameterValue, context);
        if (rawElements == null || rawElements.isEmpty()) {
            return createEmptyCollection(context);
        }

        // 3. Für jedes Element: rekursiv über die Chain auflösen
        List<Object> resolvedElements = new ArrayList<>();
        for (Object rawElement : rawElements) {
            DefaultInstanceCreationContext elementCtx = createElementContext(context, elementType, rawElement);
            Object resolvedElement = context.getRootProvider().provide(elementCtx);
            if (resolvedElement != null) {
                resolvedElements.add(resolvedElement);
            }
        }

        // 4. In den richtigen Collection-Typ konvertieren
        return convertToTargetType(resolvedElements, context);
    }

    // =========================================================================
    // extract() — Dehydration
    // =========================================================================

    @Override
    public Object extract(IInstanceCreationContext context) {
        Object objectToResolve = context.getObjectToResolve();
        if (objectToResolve == null) return null;

        // In eine iterierbare Struktur konvertieren
        Collection<?> sourceCollection = toCollection(objectToResolve);
        if (sourceCollection == null || sourceCollection.isEmpty()) {
            return new ArrayList<>();
        }

        // Element-Typ bestimmen
        Type elementType = resolveElementType(context);

        // Für jedes Element: rekursiv extrahieren
        List<Object> rawValues = new ArrayList<>();
        for (Object element : sourceCollection) {
            DefaultInstanceCreationContext elementCtx =
                    createExtractionElementContext(context, elementType, element);
            Object rawValue = context.getRootProvider().extract(elementCtx);
            rawValues.add(rawValue);
        }

        return rawValues;
    }

    // =========================================================================
    // Element-Typ-Auflösung
    // =========================================================================

    /**
     * Bestimmt den Element-Typ aus den Generics des Feldes.
     *
     * <p>Für {@code List<IProcessor>} → {@code IProcessor.class}
     * <p>Für {@code String[]} → {@code String.class}
     * <p>Fallback: {@code Object.class}
     */
    private Type resolveElementType(IInstanceCreationContext context) {
        // Array: Komponententyp direkt verfügbar
        Class<?> rawType = resolveRawType(context);
        if (rawType != null && rawType.isArray()) {
            return rawType.getComponentType();
        }

        // Generics: aus dem Feld extrahieren
        Type genericType = null;
        if (context.getFieldToResolve() != null) {
            genericType = context.getFieldToResolve().getGenericType();
        } else if (context.getTypeToResolve() instanceof ParameterizedType) {
            genericType = context.getTypeToResolve();
        }

        if (genericType instanceof ParameterizedType pt) {
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length >= 1) {
                Type elementArg = typeArgs[0];
                if (elementArg instanceof Class<?> || elementArg instanceof ParameterizedType) {
                    return elementArg;
                }
            }
        }

        // Fallback: ProcessorUtils-Methode (bewährt)
        if (context.getFieldToResolve() != null) {
            Class<?> elementClass = ProcessorUtils.getElementType(context.getFieldToResolve().getGenericType());
            if (elementClass != null) return elementClass;
        }

        log.trace("Element-Typ nicht bestimmbar für {}, verwende Object.class",
                context.getFieldToResolve() != null ? context.getFieldToResolve().getName() : "?");
        return Object.class;
    }

    // =========================================================================
    // Context-Erzeugung für Elemente
    // =========================================================================

    private DefaultInstanceCreationContext createElementContext(
            IInstanceCreationContext parentContext, Type elementType, Object parameterValue) {

        DefaultInstanceCreationContext childCtx = new DefaultInstanceCreationContext(
                (IInstanceCreationContext) parentContext);
        childCtx.setTypeToResolve(elementType);
        childCtx.setParameterValue(parameterValue);
        childCtx.setObjectToResolve(null);
        childCtx.setFieldToResolve(null); // Elemente haben kein eigenes Feld
        childCtx.setProcessorParameter(null);
        return childCtx;
    }

    private DefaultInstanceCreationContext createExtractionElementContext(
            IInstanceCreationContext parentContext, Type elementType, Object element) {

        DefaultInstanceCreationContext childCtx = new DefaultInstanceCreationContext(
                (IInstanceCreationContext) parentContext);
        childCtx.setTypeToResolve(elementType);
        childCtx.setObjectToResolve(element);
        childCtx.setParameterValue(null);
        childCtx.setFieldToResolve(null);
        childCtx.setProcessorParameter(null);
        return childCtx;
    }

    // =========================================================================
    // Input-Konvertierung
    // =========================================================================

    /**
     * Konvertiert verschiedene Input-Formate in eine Liste.
     *
     * <p>Akzeptierte Formate:
     * <ul>
     *   <li>Direkte {@code Collection<?>} → zu Liste</li>
     *   <li>Array → zu Liste</li>
     *   <li>String → Split mit {@code delimiter} (Default: Komma)</li>
     * </ul>
     */
    private List<?> convertInputToList(Object input, IInstanceCreationContext context) {
        if (input instanceof Collection<?> coll) {
            return new ArrayList<>(coll);
        }
        if (input != null && input.getClass().isArray()) {
            return arrayToList(input);
        }
        if (input instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) return Collections.emptyList();

            // Delimiter aus @ProcessorParameter oder Default
            String effectiveDelimiter = this.delimiter;
            if (context.getProcessorParameter() != null
                    && !context.getProcessorParameter().delimiter().isEmpty()) {
                effectiveDelimiter = context.getProcessorParameter().delimiter();
            }

            return Arrays.stream(trimmed.split(effectiveDelimiter))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        log.warn("Unbekanntes Collection-Input-Format: {} (Typ: {})", input,
                input != null ? input.getClass().getName() : "null");
        return null;
    }

    /**
     * Konvertiert ein Array (primitiv oder Objekt) in eine Liste.
     */
    private List<?> arrayToList(Object array) {
        int length = Array.getLength(array);
        List<Object> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            list.add(Array.get(array, i));
        }
        return list;
    }

    /**
     * Konvertiert ein Objekt (Collection oder Array) in eine Collection.
     */
    private Collection<?> toCollection(Object obj) {
        if (obj instanceof Collection<?> coll) return coll;
        if (obj != null && obj.getClass().isArray()) return arrayToList(obj);
        return null;
    }

    // =========================================================================
    // Ergebnis-Konvertierung
    // =========================================================================

    /**
     * Konvertiert die aufgelöste Liste in den gewünschten Zieltyp
     * (List, Set, oder Array).
     */
    private Object convertToTargetType(List<Object> elements, IInstanceCreationContext context) {
        Class<?> rawType = resolveRawType(context);
        if (rawType == null) return elements;

        // Array
        if (rawType.isArray()) {
            Class<?> componentType = rawType.getComponentType();
            Object array = Array.newInstance(componentType, elements.size());
            for (int i = 0; i < elements.size(); i++) {
                Array.set(array, i, elements.get(i));
            }
            return array;
        }

        // Set
        if (Set.class.isAssignableFrom(rawType)) {
            return new LinkedHashSet<>(elements);
        }

        // Default: List
        return elements;
    }

    /**
     * Erstellt eine leere Collection des richtigen Typs.
     */
    private Object createEmptyCollection(IInstanceCreationContext context) {
        Class<?> rawType = resolveRawType(context);
        if (rawType == null) return new ArrayList<>();

        if (rawType.isArray()) {
            return Array.newInstance(rawType.getComponentType(), 0);
        }
        if (Set.class.isAssignableFrom(rawType)) {
            return new LinkedHashSet<>();
        }
        return new ArrayList<>();
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
}