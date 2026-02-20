package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.api.IBeanProvider;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.context.domain.DefaultTransformationContext;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.api.IStructureValueDescriptor;
import de.starima.pfw.base.processor.description.api.IValueFunction;
import de.starima.pfw.base.processor.set.api.ISetProcessor;
import de.starima.pfw.base.util.IdentifierUtils;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
// wird nich mehr benÃ¶tigt, da mit ListDescriptor abgedeckt wird.
@Slf4j
@Getter
@Setter
@Processor(description = "Transformiert einen String (BeanIdentifier) in ein @ValueObject und zurÃ¼ck.")
public class ValueObjectValueFunction extends AbstractValueFunction<Object, Object> {

    @ProcessorParameter(value = "defaultValueObjectProviderProcessor", description = "Provider zur Erzeugung von @ValueObject-Instanzen.")
    private IBeanProvider beanProvider;

    @ProcessorParameter(description = "Definitionsbereich der Parameter (Prozessor Identifier). ")
    private ISetProcessor<Object> domainProcessor;
    @ProcessorParameter
    private ISetProcessor<Object> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<Object> imageProcessor;

    @ProcessorParameter(
            value = "jacksonValueFunction", // Hier wird der Default-Wert gesetzt
            description = "Optionale Fallback-ValueFunction, die verwendet wird, wenn ein ValueObject keine eigene ID hat.",
            required = false
    )
    private IValueFunction<ITransformationContext, Object, Object> fallbackValueFunction;

    public static boolean isResponsibleFor(ITransformationContext transformationContext) {
        if (transformationContext == null) return false;
        Class<?> clazz = transformationContext.getTargetType();
        Field field = transformationContext.getTargetField();
        Class<?> valueType = field != null ? field.getType() : clazz;
        return ProcessorUtils.isConsideredValueObject(valueType);
    }

    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return isResponsibleFor(transformationContext);
    }

    @Override
    public Object transformValue(ITransformationContext transformationContext, Object input) {
        if (input == null || "".equals(input.toString())) return null;

        try {
            if (transformationContext.getTargetField().getType().isInstance(input)) {
                return input;
            }

            //TODO: kÃ¶nnen wir den StructureDescriptor ermitteln? Falls ja, dann kÃ¶nnte er die Initialisierung Ã¼bernehmen.
            //Das wÃ¼rde gehen, wenn die ValueFunction selbst ein Descriptor ist (was sie gerade auch ist) und somit Zugriff auf seinen Parent hat!
            if (getParentDescriptor() != null && getParentDescriptor() instanceof IStructureValueDescriptor<?>) {

            }
            // Nutze den aufgewerteten BeanProvider, um die Instanz zu erzeugen UND zu initialisieren.
            return beanProvider.getBeanForId(transformationContext.getTargetField().getType(), input.toString());
        } catch (Exception e) {
            log.error("Konnte ValueObject fÃ¼r ID '{}' und Typ '{}' nicht erzeugen.", input, transformationContext.getTargetField().getType().getName(), e);
            return null;
        }
    }

    @Override
    public Object reverseTransformValue(ITransformationContext transformationContext, Object value) {
        if (value == null) return null;

        // Die Logik zur ID-Ermittlung wird an die Utility-Klasse delegiert.
        String identifier = IdentifierUtils.getIdentifierForValueObject(value);

        if (identifier != null) {
            // Erzeuge die volle Bean-ID: prototype:identifier
            String prototypeId = getRuntimeContext().getProcessorProvider().getBeanIdentifier(value.getClass());
            return ProcessorUtils.createFullBeanId(prototypeId, identifier, null);
        }

        // FALLBACK-PFAD: Das Objekt ist nicht identifizierbar.
        if (fallbackValueFunction != null) {
            // Delegiere an die konfigurierte Fallback-Funktion (z.B. Jackson).
            log.debug("ValueObject {} hat keine ID. Nutze Fallback-ValueFunction: {}", value.getClass().getSimpleName(), fallbackValueFunction.getClass().getSimpleName());
            return fallbackValueFunction.reverseTransformValue(transformationContext, value);
        }


        // ULTIMATIVER FALLBACK: Keine Fallback-Funktion konfiguriert.
        log.warn("Konnte keinen Identifier fÃ¼r ValueObject vom Typ {} im Kontext von Parameter '{}' ermitteln. Flache Parameter-Map wird durch die Fallback-ValueFunction erzeugt.",
                value.getClass().getName(), transformationContext.getTargetField().getName());
        return null;
    }

    @Override
    public Map<String, Map<String, Object>> extractEffectiveParameterMap(ITransformationContext transformationContext, Object value) {
        if (value == null) return new HashMap<>();
        //wenn wir einen parent StructureValueDescriptor haben, dann kann der Ã¼bernehmen
        IDescriptorProcessor parent = getParentDescriptor();
        if (parent != null) {
            return parent.extractEffectiveParameterMap(transformationContext, value);
        }
        DefaultTransformationContext context = new DefaultTransformationContext();
        context.setTargetObject(value);
        context.setRuntimeContext(transformationContext.getRuntimeContext());
        return ProcessorUtils.extractEffectiveParameterMap(context);
    }

    @Override
    public Map<String, Map<String, Object>> extractEffectiveParameterMap(Object value) {
        if (value == null) return new HashMap<>();

        //wenn wir einen parent StructureValueDescriptor haben, dann kann der Ã¼bernehmen
        IDescriptorProcessor parent = getParentDescriptor();
        if (parent != null) {
            return parent.extractEffectiveParameterMap(value);
        }
        //anderenfalls erledigt das die ProcessorUtils
        DefaultTransformationContext context = new DefaultTransformationContext();
        context.setTargetObject(value);
        return ProcessorUtils.extractEffectiveParameterMap(context);
    }

    @Override
    public Object transformValue(Object input) {
        if (input == null || "".equals(input.toString())) return null;

        try {
            // Nutze den aufgewerteten BeanProvider, um die Instanz zu erzeugen UND zu initialisieren.
            return beanProvider.getBeanForId(input.toString());
        } catch (Exception e) {
            log.error("Konnte ValueObject fÃ¼r ID '{}' nicht erzeugen.", input, e);
            return null;
        }
    }

    @Override
    public String getTypeSignature() {
        return "valueObject";
    }

    @Override
    public boolean isGeneric() {
        return true;
    }

    @Override
    public boolean isValueObject() {
        return true;
    }
}