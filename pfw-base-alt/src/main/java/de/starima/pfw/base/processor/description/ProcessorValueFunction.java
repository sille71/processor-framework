package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.set.api.ISetProcessor;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Map;

@Slf4j
@Getter
@Setter
@Processor(description = "Transformiert einen String (ProzessorIdentifier) in einen Prozessor. Dabei wird aktuell nur das Interface IReconProcessor geprÃ¼ft. Der domainProcessor ist auf die aktuell geforderte Prozessorklasse bzw Interface zu spezialisieren. siehe ProcessorPrototypeIdentifierSet")
public class ProcessorValueFunction extends AbstractValueFunction<Object, IProcessor> {
    public static boolean isResponsibleFor(ITransformationContext transformationContext) {
        if (transformationContext == null) return false;
        Class<?> clazz = transformationContext.getTargetType();
        Field field = transformationContext.getTargetField();
        Class<?> valueType = field != null ? field.getType() : clazz;
        return ProcessorUtils.isConsideredProcessor(valueType);
    }

    //TODO: hier defaultwerte angeben! Die set Processoren benÃ¶tigen wir im ReconLight
    @ProcessorParameter(description = "Definitionsbereich der Parameter (Prozessor Identifier). ")
    private ISetProcessor<Object> domainProcessor;
    @ProcessorParameter
    private ISetProcessor<IProcessor> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<IProcessor> imageProcessor;



    @Override
    public ISetProcessor<Object> getDomainProcessor() {
        return domainProcessor;
    }

    @Override
    public ISetProcessor<IProcessor> getCoDomainProcessor() {
        return coDomainProcessor;
    }

    @Override
    public ISetProcessor<IProcessor> getImageProcessor() {
        return imageProcessor;
    }

    @Override
    public boolean isResponsibleForInput(Object input) {
        return isInputString(input) || isInputProcessor(input);
    }

    @Override
    public IProcessor transformValue(Object input) {
        if (!isResponsibleForInput(input) || input == null) {
            log.warn("{}.transformValue: Can not transform value {}! Value is not in domain.", this.getIdentifier(), input);
            return null;
        }

        if (input instanceof IProcessor) return (IProcessor) input;

        IProcessor p = getRuntimeContext().getProcessorProvider().getProcessorForBeanId(input.toString(), getRuntimeContext(), null);
        if (p != null) {
            return p;
        }
        log.warn("{}.transformValue: Can not transform value {}! Value is properly not in domain.", this.getIdentifier(), input);
        return null;
    }

    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return isResponsibleFor(transformationContext);
    }

    private boolean isInputString(Object input) {
        return input instanceof String;
    }

    private boolean isInputProcessor(Object input) {
        return input instanceof IProcessor;
    }

    @Override
    public IProcessor transformValue(ITransformationContext transformationContext, Object input) {
        if (!isResponsibleForSubject(transformationContext)) {
            log.warn("{}.transformValue: Can not transform value {}! Processor is not responsible for subject.", this.getIdentifier(), input);
            return null;
        }

        if (!isResponsibleForInput(input) || input == null || "".equals(input)) {
            log.warn("{}.transformValue: Can not transform value {}! Value is not in domain.", this.getIdentifier(), input);
            return null;
        }

        if (isInputProcessor(input)) return (IProcessor) input;

        if (isInputString(input)) {
            IProcessor parent = null;
            IProcessorContext ctx = getRuntimeContext();
            if (transformationContext.getTargetObject() != null && transformationContext.getTargetObject() instanceof IProcessor) {
                parent = (IProcessor)transformationContext.getTargetObject();
                ctx = parent.getRuntimeContext();
            }

            if (ctx == null) ctx = transformationContext.getRuntimeContext();

            if (ctx == null) return null;

            IProcessor p = ctx.getProcessorProvider().getProcessorForBeanId(input.toString().trim(), ctx, parent);
            if (p != null) {
                return p;
            }
        }

        log.warn("{}.transformValue: Can not transform value {}!", this.getIdentifier(), input);
        return null;
    }

    public String transformObjectToParameter(IProcessor value) {
        if (value == null) return "";
        return value.getFullBeanId();
    }


    @Override
    public Map<String, Map<String, Object>> extractEffectiveParameterMap(Object value) {
        if (value instanceof IProcessor)
            return value != null ? ((IProcessor)value).extractEffectiveProcessorParameterMap() : null;
        else return null;
        //TODO: Fehler
    }

    @Override
    public String getTypeSignature() {
        return "processor";
    }

    @Override
    public boolean isGeneric() {
        return true; // ist in der Regel immer generisch, wir gehen erst mal so ins Rennen
    }

    @Override
    public boolean isProcessor() {
        return true;
    }
}