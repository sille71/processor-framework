package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.context.api.LoadStrategy;
import de.starima.pfw.base.processor.context.domain.DefaultTransformationContext;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.api.IMapValueDescriptor;
import de.starima.pfw.base.processor.description.api.IValueDescriptor;
import de.starima.pfw.base.util.MapUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Getter
@Setter
@Processor(
        description = "Beschreibt einen Parameterwert, der eine Map von Key-Value-Paaren ist.",
        categories = {"descriptor/value/map"}
)
public class DefaultMapValueDescriptor extends DefaultValueDescriptor implements IMapValueDescriptor {

    @ProcessorParameter(description = "Der IValueDescriptor, der den Typ der SchlÃ¼ssel in der Map beschreibt.")
    private IValueDescriptor keyValueDescriptor;

    @ProcessorParameter(description = "Der IValueDescriptor, der den Typ der Werte in der Map beschreibt.")
    private IValueDescriptor valueValueDescriptor;

    @Override
    public void processorOnInit() {
        super.processorOnInit();
        // Stellt die Parent-Beziehung fÃ¼r die Kind-Deskriptoren her.
        if (this.keyValueDescriptor != null) {
            this.keyValueDescriptor.setParentDescriptor(this);
        }
        if (this.valueValueDescriptor != null) {
            this.valueValueDescriptor.setParentDescriptor(this);
        }
    }

    @Override
    public Map<String, Map<String, Object>> extractEffectiveParameterMap(ITransformationContext transformationContext, Object value) {
        // Diese Ã¼berschriebene Methode bricht die Standard-Delegation an die ValueFunction.
        // Sie implementiert die spezifische Logik fÃ¼r das Extrahieren aus einer Map.
        if (!(value instanceof Map)) {
            if (value != null) {
                log.warn("extractEffectiveParameterMap in MapValueDescriptor '{}' aufgerufen mit falschem Typ: {}. Erwartet wurde eine Map.", getIdentifier(), value.getClass().getName());
            }
            return new HashMap<>();
        }

        if (keyValueDescriptor == null || valueValueDescriptor == null) {
            log.error("Kann Parameter-Map fÃ¼r Map in '{}' nicht extrahieren, da keyValueDescriptor oder valueValueDescriptor nicht definiert ist.", getIdentifier());
            return new HashMap<>();
        }

        Map<String, Map<String, Object>> effectiveBeanParameterMap = new HashMap<>();
        Map<?, ?> elements = (Map<?, ?>) value;

        log.debug("Extrahiere Parameter fÃ¼r {} Key-Value-Paare unter Verwendung von keyValueDescriptor '{}' und valueValueDescriptor '{}'.",
                elements.size(), keyValueDescriptor.getIdentifier(), valueValueDescriptor.getIdentifier());

        elements.forEach((key, val) -> {
            // Delegiere die Arbeit fÃ¼r den Key an den Key-Experten.
            Map<String, Map<String, Object>> keyMap = keyValueDescriptor.extractEffectiveParameterMap(transformationContext, key);
            MapUtils.mergeBeanIdParameterMap(effectiveBeanParameterMap, keyMap);

            // Delegiere die Arbeit fÃ¼r den Value an den Value-Experten.
            Map<String, Map<String, Object>> valueMap = valueValueDescriptor.extractEffectiveParameterMap(transformationContext, val);
            MapUtils.mergeBeanIdParameterMap(effectiveBeanParameterMap, valueMap);
        });

        return effectiveBeanParameterMap;
    }

    @Override
    public void extractEffectiveParameterMap(ITransformationContext context, Map<String, Map<String, Object>> beanParameterMap, Set<Object> visited) {
        Object bean = context.getTargetObject();

        // 1. PrÃ¼fen, ob es etwas zu tun gibt.
        if (bean == null || !(bean instanceof Map) || context.getLoadStrategy() != LoadStrategy.DEEP) {
            return;
        }

        Map<?, ?> map = (Map<?, ?>) bean;
        IDescriptorProcessor keyDescriptor = getKeyValueDescriptor();
        IDescriptorProcessor valueDescriptor = getValueValueDescriptor();

        if (keyDescriptor == null || valueDescriptor == null) {
            log.warn("key- und/oder valueValueDescriptor fÃ¼r Map-Deskriptor nicht gefunden. Kann Elemente nicht extrahieren.");
            return;
        }

        // 2. Iteriere Ã¼ber die EintrÃ¤ge der Map...
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            // --- 3a. Extraktion fÃ¼r den SCHLSSEL ---
            DefaultTransformationContext keyContext = new DefaultTransformationContext();
            keyContext.setTargetObject(entry.getKey());
            keyContext.setTargetField(context.getTargetField());
            keyContext.setRuntimeContext(context.getRuntimeContext());
            keyContext.setLoadStrategy(context.getLoadStrategy());
            keyDescriptor.extractEffectiveParameterMap(keyContext, beanParameterMap, visited);

            // --- 3b. Extraktion fÃ¼r den WERT ---
            DefaultTransformationContext valueContext = new DefaultTransformationContext();
            valueContext.setTargetObject(entry.getValue());
            valueContext.setTargetField(context.getTargetField());
            valueContext.setRuntimeContext(context.getRuntimeContext());
            valueContext.setLoadStrategy(context.getLoadStrategy());
            valueDescriptor.extractEffectiveParameterMap(valueContext, beanParameterMap, visited);
        }
    }
}