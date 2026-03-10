package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.context.domain.DefaultTransformationContext;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.api.ICollectionValueDescriptor;
import de.starima.pfw.base.processor.description.api.IValueDescriptor;
import de.starima.pfw.base.processor.description.api.IValueFunction;
import de.starima.pfw.base.util.MapUtils;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Getter
@Setter
@Processor
public abstract class AbstractCollectionValueFunction<I, O> extends AbstractValueFunction<I, O> {

    @ProcessorParameter(description = "Die Function der Elemente kann explizit angegeben werden. Dies ist sinnvoll, wenn keine Descriptor Hierarchie vorliegt.")
    private IValueFunction<ITransformationContext, Object, Object> elementFunction;

    @ProcessorParameter(value = ",", description = "Das Trennzeichen, das verwendet wird, wenn der Input ein String ist.")
    private String delimiter = ",";

    // --- Abstrakte Methoden fÃ¼r die Subklassen ---

    /**
     * Konvertiert einen Stream von transformierten Elementen in den finalen Ziel-Collection-Typ (List oder Array).
     */
    protected abstract O toTargetCollection(Stream<Object> stream);

    /**
     * Konvertiert das Input-Objekt (garantiert eine Collection oder ein Array) in einen Stream seiner rohen Elemente.
     */
    protected abstract Stream<?> toStream(O collectionOrArray);

    // --- Zentrale Logik ---

    @Override
    public O transformValue(I input) {
        return transformValue(null, input);
    }

    @Override
    public O transformValue(ITransformationContext transformationContext, I input) {
        if (input == null || !isResponsibleForInput(input)) {
            log.warn("{}.transformValue: Kann Wert {} nicht transformieren! Wert ist nicht im Definitionsbereich.", getIdentifier(), input);
            return null;
        }

        // 1. Rohe Elemente als Stream holen (behebt die NPE)
        Stream<?> rawElementsStream;
        if (input instanceof String) {
            rawElementsStream = Arrays.stream(((String) input).split(delimiter));
        } else {
            rawElementsStream = toStream((O) input);
        }

        // 2. Element-Funktion finden
        IValueFunction<ITransformationContext, Object, Object> localElementFunction = getElementFunction(transformationContext);
        if (localElementFunction == null) {
            log.warn("Keine Kind-ValueFunction fÃ¼r CollectionValueFunction bei '{}' gefunden. Transformation nicht mÃ¶glich.", getPath());
            return null;
        }

        // 3. Kontext fÃ¼r Elemente erstellen
        DefaultTransformationContext elementContext = new DefaultTransformationContext();
        elementContext.setRuntimeContext(transformationContext.getRuntimeContext());
        if (transformationContext.getFieldToResolve() != null) {
            elementContext.setTypeToResolve(ProcessorUtils.getElementType(transformationContext.getFieldToResolve().getGenericType()));
        }

        // 4. Stream transformieren und in Ziel-Collection konvertieren
        Stream<Object> transformedStream = rawElementsStream
                .map(element -> localElementFunction.transformValue(elementContext, element));

        return toTargetCollection(transformedStream);
    }

    @Override
    public I transformObjectToParameter(ITransformationContext transformationContext, O value) {
        if (value == null) {
            return (I) "";
        }

        IValueFunction<ITransformationContext, Object, Object> localElementFunction = getElementFunction(transformationContext);
        if (localElementFunction == null) {
            log.warn("Keine Kind-ValueFunction fÃ¼r '{}' gefunden. Ransformation nicht mÃ¶glich.", getPath());
            return (I) value.toString(); // Fallback
        }

        // Korrigiert die RÃ¼cktransformation: gibt einen String zurÃ¼ck, keine Liste.
        String result = toStream(value)
                .map(element -> localElementFunction.reverseTransformValue(transformationContext, element))
                .map(String::valueOf)
                .collect(Collectors.joining(delimiter));

        return (I) result;
    }

    @Override
    public Map<String, Map<String, Object>> extractEffectiveParameterMap(Object value) {
        return extractEffectiveParameterMap(new DefaultTransformationContext(), value);
    }

    /**
     * Extrahiert die Parameter-Map aus einer Sammlung (Liste oder Array).
     * Diese Methode delegiert die Extraktion fÃ¼r jedes Element an die zustÃ¤ndige Kind-Funktion.
     */
    @Override
    public Map<String, Map<String, Object>> extractEffectiveParameterMap(ITransformationContext transformationContext, Object value) {
        // 1. Hole die zustÃ¤ndige Funktion und den Kontext fÃ¼r die Elemente.
        CollectionFunctionContext context = getCollectionFunctionContext(transformationContext);
        if (context == null) {
            log.error("Parameter-Extraktion in '{}' fehlgeschlagen: Element-Function konnte nicht ermittelt werden.", getIdentifier());
            return new HashMap<>();
        }

        // 2. Stelle sicher, dass der Input eine Sammlung ist und konvertiere ihn in einen Stream.
        Stream<?> elementsStream;
        if (value instanceof List) {
            elementsStream = ((List<?>) value).stream();
        } else if (value != null && value.getClass().isArray()) {
            elementsStream = Arrays.stream((Object[]) value);
        } else {
            if (value != null) {
                log.warn("extractEffectiveParameterMap in CollectionValueFunction '{}' aufgerufen mit falschem Typ: {}. Erwartet wurde eine Liste oder ein Array.", getIdentifier(), value.getClass().getName());
            }
            return new HashMap<>();
        }

        Map<String, Map<String, Object>> effectiveBeanParameterMap = new HashMap<>();
        log.debug("Extrahiere Parameter fÃ¼r Elemente der Sammlung in '{}'.", getIdentifier());

        // 3. Iteriere durch den Stream und delegiere die Extraktion fÃ¼r jedes Element an die Kind-Funktion.
        elementsStream.forEach(resolvedElement -> {
            Map<String, Map<String, Object>> elementMap = context.elementFunction.extractEffectiveParameterMap(context.elementContext, resolvedElement);
            MapUtils.mergeBeanIdParameterMap(effectiveBeanParameterMap, elementMap);
        });

        return effectiveBeanParameterMap;
    }

    // Eine kleine private Klasse, um die Kontexte und Funktionen zu bÃ¼ndeln.
    private static class CollectionFunctionContext {
        final IValueFunction<ITransformationContext, Object, Object> elementFunction;
        final ITransformationContext elementContext;

        CollectionFunctionContext(IValueFunction<ITransformationContext, Object, Object> elementFunction, ITransformationContext elementContext) {
            this.elementFunction = elementFunction;
            this.elementContext = elementContext;
        }
    }

    /**
     * Zentralisiert die Logik zur Ermittlung der Kind-Funktion und ihres Kontexts.
     */
    private CollectionFunctionContext getCollectionFunctionContext(ITransformationContext transformationContext) {
        IValueFunction<ITransformationContext, Object, Object> elementFunction = getElementFunction(transformationContext);
        if (elementFunction == null) {
            return null;
        }

        Class<?> elementType = getGenericCollectionType(transformationContext);

        DefaultTransformationContext elementContext = new DefaultTransformationContext();
        elementContext.setRuntimeContext(transformationContext.getRuntimeContext());
        elementContext.setTypeToResolve(elementType);

        return new CollectionFunctionContext(elementFunction, elementContext);
    }

    /**
     * Hilfsmethode, um den generischen Typ (z.B. T in List<T>) aus dem Feld zu extrahieren.
     */
    private Class<?> getGenericCollectionType(ITransformationContext context) {
        if (context == null || context.getFieldToResolve() == null) {
            return Object.class; // Fallback
        }

        java.lang.reflect.Type genericType = context.getFieldToResolve().getGenericType();
        if (genericType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType parameterizedType = (java.lang.reflect.ParameterizedType) genericType;
            java.lang.reflect.Type[] typeArguments = parameterizedType.getActualTypeArguments();

            if (typeArguments.length == 1 && typeArguments[0] instanceof Class) {
                return (Class<?>) typeArguments[0];
            }
        } else if (genericType instanceof Class && ((Class<?>) genericType).isArray()) {
            return ((Class<?>) genericType).getComponentType();
        }

        log.trace("Konnte generischen Typ fÃ¼r Sammlung nicht bestimmen. Verwende Object.class als Fallback.");
        return Object.class;
    }

    /**
     * Findet die zustÃ¤ndige ValueFunction fÃ¼r die Elemente.
     * PrioritÃ¤t 1: Explizit gesetzte 'elementFunction'.
     * PrioritÃ¤t 2: Ãœber die Deskriptor-Hierarchie vom Parent.
     * PrioritÃ¤t 3: Fallback Ã¼ber den TransformationContext.
     */
    protected IValueFunction<ITransformationContext, Object, Object> getElementFunction(ITransformationContext transformationContext) {
        if (this.elementFunction != null) {
            return this.elementFunction;
        }

        IDescriptorProcessor parent = getParentDescriptor();
        if (parent instanceof ICollectionValueDescriptor) {
            IValueDescriptor elementDescriptor = ((ICollectionValueDescriptor) parent).getElementValueDescriptor();
            if (elementDescriptor != null) {
                this.elementFunction = elementDescriptor.getValueFunction();
                return this.elementFunction;
            }
        }
        return null;
    }
}