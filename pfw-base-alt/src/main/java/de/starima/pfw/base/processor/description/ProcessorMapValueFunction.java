package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.set.api.ISetProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO: wird wohl durch die allgemeinere MapValueFunction abgelÃ¶st!

@Slf4j
@Getter
@Setter
@Processor(description = "Transformiert eine Liste von ProzessorIdentifiern in eine Liste von Prozessoren. Dabei wird aktuell nur das Interface IReconProcessor geprÃ¼ft. Der domainProcessor ist auf die aktuell geforderte Prozessorklasse bzw Interface zu spezialisieren. siehe ProcessorPrototypeIdentifierSet")
public class ProcessorMapValueFunction extends AbstractValueFunction<Object,Map<String, IProcessor>>  {
    public static boolean isResponsibleFor(Field field) {
        // wir deaktivieren diese Funktion
        return false;
        /*
        if (Map.class.isAssignableFrom(field.getType())) {
            log.debug("check processor map for field {}", field.getName());
            if (field.getGenericType() instanceof ParameterizedType) {
                Type[] fieldArgTypes = ((ParameterizedType)field.getGenericType()).getActualTypeArguments();
                Type valueType = fieldArgTypes[1];
                try {
                    Class<?> processorClass = null;
                    // keys und/oder values kÃ¶nnen wieder parametrisiert sein, in diesem Fall fragen wir nach getRawType!
                    if (valueType instanceof ParameterizedType)
                        processorClass = IReconProcessor.class.getClassLoader().loadClass(((ParameterizedType)valueType).getRawType().getTypeName());
                    else
                        processorClass = IReconProcessor.class.getClassLoader().loadClass(valueType.getTypeName());

                    return IReconProcessor.class.isAssignableFrom(processorClass);
                } catch (ClassNotFoundException cce) {
                    log.debug("Could not find class {} of field {}", fieldArgTypes[0], field.getName(), cce);
                }
            }
        }
        return false;

         */
    }

    //TODO: hier defaultwerte angeben! Die set Processoren benÃ¶tigen wir im ReconLight
    @ProcessorParameter(description = "Definitionsbereich der Parameter. Komma separierte Liste von Prozessoridentifiern. ")
    private ISetProcessor<Object> domainProcessor;
    @ProcessorParameter
    private ISetProcessor<Map<String, IProcessor>> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<Map<String, IProcessor>> imageProcessor;

    @ProcessorParameter(value = ",", description = "Trenner der key/identifier Paare")
    private String delimiter = ",";
    @ProcessorParameter(value = ";", description = "trennt den key vom Processoridentifier")
    private String keyIdentifierDelimiter = ";";

    @Override
    public ISetProcessor<Object> getDomainProcessor() {
        return domainProcessor;
    }

    @Override
    public ISetProcessor<Map<String, IProcessor>> getCoDomainProcessor() {
        return coDomainProcessor;
    }

    @Override
    public ISetProcessor<Map<String, IProcessor>> getImageProcessor() {
        return imageProcessor;
    }

    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return transformationContext != null && isResponsibleFor(transformationContext.getTargetField());
    }

    private boolean isInputString(Object input) {
        return input instanceof String;
    }

    private boolean isInputListOfStrings(Object input) {
        if (input instanceof List && !((List) input).isEmpty()) {
            for (Object item : (List) input) {
                if (!(item instanceof String)) return false;
            }
            return true;
        }
        return false;
    }

    private boolean isInputMapOfProcessors(Object input) {
        if (input instanceof Map && !((Map) input).isEmpty()) {
            for (Map.Entry<?, ?> entry : ((Map<?,?>) input).entrySet()) {
                if (!(entry.getKey() instanceof String && entry.getValue() instanceof IProcessor)) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public Map<String, IProcessor> transformValue(Object input) {
        if (!isResponsibleForInput(input)) {
            log.warn("{}.transformValue: Can not transform value {}! Value is not in domain.", this.getIdentifier(), input);
            return null;
        }

        if (isInputMapOfProcessors(input)) return (Map<String, IProcessor>) input;

        if (isInputString(input)) {
            Map<String, IProcessor> processorMap = new HashMap<>();
            if (input == null) return processorMap;

            for (String keyIdentifierPair : input.toString().split(delimiter)) {
                String[] keyIdentifierArr = keyIdentifierPair.split(keyIdentifierDelimiter);
                String key = keyIdentifierArr[0];
                String processorIdentifier = keyIdentifierArr[1];

                IProcessor p = getRuntimeContext().getProcessorProvider().getProcessorForBeanId(processorIdentifier, getRuntimeContext(), null);
                if (p != null) {
                    processorMap.put(key, p);
                } else {
                    log.debug("Can not assign key {} and processor {} to map field!", key, processorIdentifier);
                }
            }

            if (!processorMap.isEmpty()) return processorMap;
        }

        log.warn("{}.transformValue: Can not transform value {}! Value is properly not in domain.", this.getIdentifier(), input);
        return null;
    }

    @Override
    public Map<String, IProcessor> transformValue(ITransformationContext transformationContext, Object input) {
        if (!isResponsibleForSubject(transformationContext)) {
            log.warn("{}.transformValue: Can not transform value {}! Processor is not responsible for subject.", this.getIdentifier(), input);
            return null;
        }

        if (!isResponsibleForInput(input)) {
            log.warn("{}.transformValue: Can not transform value {}! Value is not in domain.", this.getIdentifier(), input);
            return null;
        }

        if (isInputMapOfProcessors(input)) return (Map<String, IProcessor>) input;

        if (isInputString(input)) {
            Map<String, IProcessor> processorMap = new HashMap<>();
            if (input == null) return processorMap;

            IProcessor parent = null;
            IProcessorContext ctx = getRuntimeContext();
            if (transformationContext.getTargetObject() != null && transformationContext.getTargetObject() instanceof IProcessor) {
                parent = (IProcessor)transformationContext.getTargetObject();
                ctx = parent.getRuntimeContext();
            }

            if (ctx == null) ctx = transformationContext.getRuntimeContext();

            if (ctx == null) return null;

            for (String keyIdentifierPair : input.toString().split(delimiter)) {
                String[] keyIdentifierArr = keyIdentifierPair.split(keyIdentifierDelimiter);
                String key = keyIdentifierArr[0];
                String processorIdentifier = keyIdentifierArr[1];

                IProcessor p = ctx.getProcessorProvider().getProcessorForBeanId(processorIdentifier, ctx, parent);
                if (p != null) {
                    processorMap.put(key, p);
                } else {
                    log.debug("Can not assign key {} and processor {} to map field!", key, processorIdentifier);
                }
            }

            if (!processorMap.isEmpty()) return processorMap;
        }

        log.warn("{}.transformValue: Can not transform value {}!", this.getIdentifier(), input);
        return null;
    }

    public String transformObjectToParameter(Map<String, IProcessor> value) {
        if (value == null) return "";
        List<String> keyIdentifierPairs = new ArrayList<>();

        for (Map.Entry<String, IProcessor> entry : value.entrySet()) {
            keyIdentifierPairs.add(entry.getKey().concat(keyIdentifierDelimiter).concat(entry.getValue().getFullBeanId()));
        }

        if (!keyIdentifierPairs.isEmpty()) {
            log.debug("{} transformed processor list to {}", this.getIdentifier(), StringUtils.collectionToDelimitedString(keyIdentifierPairs, delimiter));
            return StringUtils.collectionToDelimitedString(keyIdentifierPairs, delimiter);
        }
        return null;
    }


    @Override
    public String getTypeSignature() {
        return "map<processor>";
    }

    @Override
    public boolean isGeneric() {
        return true;
    }
}