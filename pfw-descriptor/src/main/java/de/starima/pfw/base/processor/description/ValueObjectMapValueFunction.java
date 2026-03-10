package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.api.IBeanProvider;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.description.api.IValueFunction;
import de.starima.pfw.base.processor.set.api.ISetProcessor;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

//TODO: wird wohl durch die allgemeinere MapValueFunction abgelÃ¶st!

@Slf4j
@Getter
@Setter
@Processor(description = "Transformiert eine Map von Strings (BeanIdentifier) in eine Map von @ValueObject-Instanzen.")
public class ValueObjectMapValueFunction extends AbstractValueFunction<Object, Map<String, Object>>  implements IValueFunction<ITransformationContext, Object, Map<String, Object>> {

    @ProcessorParameter(value = "defaultValueObjectProviderProcessor", description = "Provider zur Erzeugung von einfachen Beans.")
    private IBeanProvider beanProvider;

    public static boolean isResponsibleFor(Field field) {
        // wir deaktivieren diese Funktion
        return false;
        /*
        if (Map.class.isAssignableFrom(field.getType())) {
            // PrÃ¼ft, ob der Wert-Typ der Map ein @ValueObject ist.
            return ProcessorUtils.getGenericType(field).isAnnotationPresent(de.starima.pfw.base.annotation.ValueObject.class);
        }
        return false;
        
         */
    }
    @ProcessorParameter(description = "Definitionsbereich der Parameter (Prozessor Identifier). ")
    private ISetProcessor<Object> domainProcessor;
    @ProcessorParameter
    private ISetProcessor<Map<String, Object>> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<Map<String, Object>> imageProcessor;


    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return transformationContext != null && isResponsibleFor(transformationContext.getTargetField());
    }

    @Override
    public Map<String, Object> transformValue(ITransformationContext transformationContext, Object input) {
        if (input == null) return null;

        if (input instanceof Map) {
            Map<String, Object> result = new HashMap<>();
            Class<?> valueType = ProcessorUtils.getGenericType(transformationContext.getTargetField());

            for (Map.Entry<?, ?> entry : ((Map<?, ?>) input).entrySet()) {
                String key = entry.getKey().toString();
                Object valueIdentifier = entry.getValue();

                try {
                    Object valueInstance = beanProvider.getBeanForId(valueType, valueIdentifier.toString());
                    result.put(key, valueInstance);
                } catch (Exception e) {
                    log.error("Konnte ValueObject fÃ¼r ID '{}' und Typ '{}' nicht erzeugen.", valueIdentifier, valueType.getName(), e);
                }
            }
            return result;
        }

        log.warn("Input fÃ¼r ValueObjectMapValueFunction ist keine Map: {}", input.getClass().getName());
        return null;
    }

    @Override
    public Map<String, Object> transformValue(Object input) {
        if (input == null) return null;

        if (input instanceof Map) {
            Map<String, Object> result = new HashMap<>();

            for (Map.Entry<?, ?> entry : ((Map<?, ?>) input).entrySet()) {
                String key = entry.getKey().toString();
                Object valueIdentifier = entry.getValue();

                try {
                    Object valueInstance = beanProvider.getBeanForId(valueIdentifier.toString());
                    result.put(key, valueInstance);
                } catch (Exception e) {
                    log.error("Konnte ValueObject fÃ¼r ID '{}' nicht erzeugen.", valueIdentifier, e);
                }
            }
            return result;
        }

        log.warn("Input fÃ¼r ValueObjectMapValueFunction ist keine Map: {}", input.getClass().getName());
        return null;
    }

    @Override
    public Object reverseTransformValue(ITransformationContext transformationContext, Map<String, Object> value) {
        // Umkehrtransformation wird vorerst nicht implementiert.
        return null;
    }

    @Override
    public String getTypeSignature() {
        return "map<valueObject>";
    }

    @Override
    public boolean isGeneric() {
        return true;
    }
}