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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

//TODO: wird wohl durch die allgemeinere ListValueFunction abgelÃ¶st!

@Slf4j
@Getter
@Setter
@Processor(description = "Transformiert eine Liste von Strings (BeanIdentifier) in eine Liste von @ValueObject-Instanzen. Eine bessere alternative bietet aber die allgemeinere ListValueFunction!")
public class ValueObjectListValueFunction extends AbstractValueFunction<Object, List<Object>> implements IValueFunction<ITransformationContext, Object, List<Object>> {

    @ProcessorParameter(value = "defaultBeanProvider", description = "Provider zur Erzeugung von einfachen Beans.")
    private IBeanProvider beanProvider;

    public static boolean isResponsibleFor(Field field) {
        // wir deaktivieren diese Funktion
        return false;
        /*
        if (List.class.isAssignableFrom(field.getType())) {
            return ProcessorUtils.getGenericType(field).isAnnotationPresent(de.starima.pfw.base.annotation.ValueObject.class);
        }
        return false;

         */
    }

    @ProcessorParameter(description = "Definitionsbereich der Parameter (Prozessor Identifier). ")
    private ISetProcessor<Object> domainProcessor;
    @ProcessorParameter
    private ISetProcessor<List<Object>> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<List<Object>> imageProcessor;

    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return transformationContext != null && isResponsibleFor(transformationContext.getTargetField());
    }

    @Override
    public List<Object> transformValue(ITransformationContext transformationContext, Object input) {
        if (input == null) return null;

        if (input instanceof List) {
            // Hier kÃ¶nnten wir noch prÃ¼fen, ob die Elemente schon vom richtigen Typ sind
            return (List<Object>) input;
        }

        if (input instanceof Collection) {
            List<Object> result = new ArrayList<>();
            Class<?> genericType = ProcessorUtils.getGenericType(transformationContext.getTargetField());
            for (Object item : (Collection<?>) input) {
                try {
                    result.add(beanProvider.getBeanForId(genericType, item.toString()));
                } catch (Exception e) {
                    log.error("Konnte ValueObject fÃ¼r ID '{}' und Typ '{}' nicht erzeugen.", item, genericType.getName(), e);
                }
            }
            return result;
        }

        log.warn("Input fÃ¼r ValueObjectListValueFunction ist keine Collection: {}", input.getClass().getName());
        return null;
    }

    @Override
    public List<Object> transformValue(Object input) {
        if (input == null) return null;

        if (input instanceof List) {
            // Hier kÃ¶nnten wir noch prÃ¼fen, ob die Elemente schon vom richtigen Typ sind
            return (List<Object>) input;
        }

        if (input instanceof Collection) {
            List<Object> result = new ArrayList<>();
            for (Object item : (Collection<?>) input) {
                try {
                    result.add(beanProvider.getBeanForId(item.toString()));
                } catch (Exception e) {
                    log.error("Konnte ValueObject fÃ¼r ID '{}' nicht erzeugen.", item, e);
                }
            }
            return result;
        }

        log.warn("Input fÃ¼r ValueObjectListValueFunction ist keine Collection: {}", input.getClass().getName());
        return null;
    }

    @Override
    public Object reverseTransformValue(ITransformationContext transformationContext, List<Object> value) {
        // Umkehrtransformation ist hier ebenfalls komplex und wird vorerst nicht implementiert.
        return null;
    }

    @Override
    public String getTypeSignature() {
        return "list<valueObject>";
    }

    @Override
    public boolean isGeneric() {
        return true;
    }
}