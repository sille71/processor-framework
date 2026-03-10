package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.context.api.LoadStrategy;
import de.starima.pfw.base.processor.context.domain.DefaultTransformationContext;
import de.starima.pfw.base.processor.description.api.ICollectionValueDescriptor;
import de.starima.pfw.base.processor.description.api.IValueDescriptor;
import de.starima.pfw.base.processor.description.api.IValueFunction;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.util.*;

/**
 * {@code DefaultCollectionValueDescriptor} ist eine Implementierung von {@link ICollectionValueDescriptor}.
 * Sie beschreibt einen Wert, der eine Sammlung (Liste/Array) von Elementen ist.
 * Aktuell wird von homogenen Listen ausgegangen, d.h. alle Elemente in der Sammlung haben den gleichen Typ,
 * der durch {@code elementValueDescriptor} beschrieben wird.
 *
 * <p><b>Prozessor-Beschreibung:</b></p>
 * <p><b>Name:</b> DefaulCollectionValueDescriptor</p>
 * <p><b>Beschreibung:</b> Beschreibt einen Parameterwert, der eine Liste von Elementen ist.</p>
 * <p><b>Kategorien:</b> descriptor/value/list</p>
 * <p><b>Parameter:</b></p>
 * <ul>
 *     <li>{@code currentValue}: Die aktuell zugewiesene Liste.</li>
 *     <li>{@code valueFunction}: Die {@link IValueFunction}, die fÃ¼r die Transformation und Validierung der Liste zustÃ¤ndig ist.</li>
 *     <li>{@code elementValueDescriptor}: Der {@link IValueDescriptor}, der den Typ der Listenelemente beschreibt.</li>
 *     <li>{@code allowDuplicates}: Gibt an, ob Duplikate in der Liste erlaubt sind.</li>
 * </ul>
 */
@Slf4j
@Getter
@Setter
@Processor(
    description = "Beschreibt einen Parameterwert, der eine Sammlung (Liste/Array) von Elementen ist.",
    categories = {"descriptor/value/list"}
)
public class DefaultCollectionValueDescriptor extends DefaultValueDescriptor implements ICollectionValueDescriptor {
    @ProcessorParameter(description = "Der IValueDescriptor, der den Typ der Listenelemente beschreibt.")
    private IValueDescriptor elementValueDescriptor;

    @ProcessorParameter(description = "Gibt an, ob Duplikate in der Liste erlaubt sind.", value = "true")
    private boolean allowDuplicates = true;

    @Override
    public void processorOnInit() {
        super.processorOnInit();
        if (this.elementValueDescriptor != null) {
            this.elementValueDescriptor.setParentDescriptor(this);
        }
    }

    @Override
    public boolean allowDuplicates() {
        return allowDuplicates;
    }

    public void extractEffectiveParameterMap(ITransformationContext context, Map<String, Map<String, Object>> beanParameterMap, Set<Object> visited) {
        Object bean = context.getTargetObject();

        // 1. Grundlegende PrÃ¼fungen.
        if (bean == null || context.getLoadStrategy() != LoadStrategy.DEEP) {
            return;
        }

        if (elementValueDescriptor == null) {
            log.warn("Kein elementValueDescriptor fÃ¼r Collection-Deskriptor gefunden. Kann Elemente nicht extrahieren.");
            return;
        }

        // 2. NEU: Normalisiere den Input. Behandle Collections und Arrays einheitlich.
        Collection<?> collectionToProcess;
        if (bean instanceof Collection) {
            collectionToProcess = (Collection<?>) bean;
        } else if (bean.getClass().isArray()) {
            collectionToProcess = arrayToCollection(bean);
        } else {
            // Es ist weder eine Collection noch ein Array. Hier gibt es nichts zu tun.
            return;
        }

        // 3. Iteriere Ã¼ber die normalisierte Collection im funktionalen Stil.
        collectionToProcess.stream()
                .filter(Objects::nonNull) // Ignoriere null-Elemente, um Fehler zu vermeiden.
                .forEach(elementBean -> {
                    // 4. Erzeuge Kontext und rufe die Rekursion fÃ¼r jedes Element auf.
                    DefaultTransformationContext elementContext = createChildContextForElement(context, elementBean);
                    elementValueDescriptor.extractEffectiveParameterMap(elementContext, beanParameterMap, visited);
                });
    }

    /**
     * Private Helfermethode, um den Code in der Stream-Operation sauber zu halten.
     * Erzeugt einen neuen, prÃ¤zisen Kontext fÃ¼r ein Element der Collection.
     */
    private DefaultTransformationContext createChildContextForElement(ITransformationContext parentContext, Object elementBean) {
        DefaultTransformationContext elementContext = new DefaultTransformationContext();
        elementContext.setTargetObject(elementBean);
        //TODO: hier kÃ¶nnen wir mehr Infos Ã¼ber den Typ des Elements einfÃ¼gen (targetField ist ja hier List oder Array!
        elementContext.setTargetField(parentContext.getTargetField());
        elementContext.setRuntimeContext(parentContext.getRuntimeContext());
        elementContext.setLoadStrategy(parentContext.getLoadStrategy());
        return elementContext;
    }

    /**
     * Konvertiert ein beliebiges Array (primitiv oder Objekt) sicher in eine List<Object>.
     * Verwendet Reflection, um generisch zu arbeiten.
     *
     * @param array Das Array-Objekt, das konvertiert werden soll.
     * @return Eine Liste, die die Elemente des Arrays enthÃ¤lt.
     */
    private List<Object> arrayToCollection(Object array) {
        int length = Array.getLength(array);
        List<Object> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            list.add(Array.get(array, i));
        }
        return list;
    }
}