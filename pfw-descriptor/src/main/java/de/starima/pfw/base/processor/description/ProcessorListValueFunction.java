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
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

//TODO: wird wohl durch die allgemeinere ListValueFunction abgelÃ¶st!

@Slf4j
@Getter
@Setter
@Processor(description = "Eine bessere alternative bietet aber die allgemeinere ListValueFunction! Transformiert eine Liste von ProzessorIdentifiern in eine Liste von Prozessoren. Dabei wird aktuell nur das Interface IProcessor geprÃ¼ft. Der domainProcessor ist auf die aktuell geforderte Prozessorklasse bzw Interface zu spezialisieren. siehe ProcessorPrototypeIdentifierSet")
public class ProcessorListValueFunction extends AbstractValueFunction<Object, List<IProcessor>> {
    public static boolean isResponsibleFor(Field field) {
        // wir deaktivieren diese Funktion
        return false;
        /*
        if (List.class.isAssignableFrom(field.getType())) {
            log.debug("check list for field {}", field.getName());
            if (field.getGenericType() instanceof ParameterizedType) {
                Type[] fieldArgTypes = ((ParameterizedType)field.getGenericType()).getActualTypeArguments();
                try {
                    Class<?> processorClass = null;
                    //der Listentyp kann selbst wieder parametrisiert sein, in diesem Fall fragen wir nach getRawType!
                    if (fieldArgTypes[0] instanceof ParameterizedType)
                        processorClass = IProcessor.class.getClassLoader()
                                .loadClass(((ParameterizedType)fieldArgTypes[0]).getRawType().getTypeName());
                    else
                        processorClass = IProcessor.class.getClassLoader().loadClass(fieldArgTypes[0].getTypeName());

                    return IProcessor.class.isAssignableFrom(processorClass);
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
    private ISetProcessor<List<IProcessor>> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<List<IProcessor>> imageProcessor;

    @ProcessorParameter(value = ",")
    private String delimiter = ",";

    @Override
    public ISetProcessor<Object> getDomainProcessor() {
        return domainProcessor;
    }

    @Override
    public ISetProcessor<List<IProcessor>> getCoDomainProcessor() {
        return coDomainProcessor;
    }

    @Override
    public ISetProcessor<List<IProcessor>> getImageProcessor() {
        return imageProcessor;
    }

    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return transformationContext != null && isResponsibleFor(transformationContext.getTargetField());
    }

    @Override
    public boolean isResponsibleForInput(Object input) {
        return isInputString(input) || isInputListOfStrings(input) || isInputListOfProcessors(input);
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

    private boolean isInputListOfProcessors(Object input) {
        if (input instanceof List && !((List) input).isEmpty()) {
            for (Object item : (List) input) {
                if (!(item instanceof IProcessor)) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public List<IProcessor> transformValue(Object input) {
        if (!isResponsibleForInput(input) || input == null) {
            log.warn("{}.transformValue: Can not transform value {}! Value is not in domain.", this.getIdentifier(), input);
            return null;
        }
        ArrayList<IProcessor> processors = new ArrayList<>();
        if (isInputString(input)) {
            for (String processorIdentifier : input.toString().split(delimiter)) {
                IProcessor p = getRuntimeContext().getProcessorProvider().getProcessorForBeanId(processorIdentifier, getRuntimeContext(), null);
                if (p != null) {
                    processors.add(p);
                } else {
                    log.debug("Can not assign processor {} to list field!", processorIdentifier);
                }
            }
        } else if (isInputListOfStrings(input)) {
            for (String processorIdentifier : (List<String>) input) {
                IProcessor p = getRuntimeContext().getProcessorProvider().getProcessorForBeanId(processorIdentifier, getRuntimeContext(), null);
                if (p != null) {
                    processors.add(p);
                } else {
                    log.debug("Can not assign processor {} to list field!", processorIdentifier);
                }
            }
        } else if (isInputListOfProcessors(input)) {
            return (List<IProcessor>) input;
        }

        if (!processors.isEmpty()) return processors;

        log.warn("{}.transformValue: Can not transform value {}! Value is properly not in domain.", this.getIdentifier(), input);
        return null;
    }

    @Override
    public List<IProcessor> transformValue(ITransformationContext transformationContext, Object input) {
        if (!isResponsibleForSubject(transformationContext)) {
            log.warn("{}.transformValue: Can not transform value {}! Processor is not responsible for subject.", this.getIdentifier(), input);
            return null;
        }

        if (!isResponsibleForInput(input) || input == null) {
            log.warn("{}.transformValue: Can not transform value {}! Value is not in domain.", this.getIdentifier(), input);
            return null;
        }

        IProcessor parent = null;
        IProcessorContext ctx = getRuntimeContext();
        if (transformationContext.getTargetObject() != null && transformationContext.getTargetObject() instanceof IProcessor) {
            parent = (IProcessor)transformationContext.getTargetObject();
            ctx = parent.getRuntimeContext();
        }

        if (ctx == null) ctx = transformationContext.getRuntimeContext();

        if (ctx == null) return null;

        ArrayList<IProcessor> processors = new ArrayList<>();
        if (isInputString(input)) {
            for (String processorIdentifier : input.toString().split(delimiter)) {
                IProcessor p = ctx.getProcessorProvider().getProcessorForBeanId(processorIdentifier, ctx, parent);
                if (p != null) {
                    processors.add(p);
                } else {
                    log.debug("Can not assign processor {} to list field!", processorIdentifier);
                }
            }
        } else if (isInputListOfStrings(input)) {
            for (String processorIdentifier : (List<String>) input) {
                IProcessor p = ctx.getProcessorProvider().getProcessorForBeanId(processorIdentifier, ctx, parent);
                if (p != null) {
                    processors.add(p);
                } else {
                    log.debug("Can not assign processor {} to list field!", processorIdentifier);
                }
            }
        } else if (isInputListOfProcessors(input)) {
            return (List<IProcessor>) input;
        }

        if (!processors.isEmpty()) return processors;
        log.warn("{}.transformValue: Can not transform value {}!", this.getIdentifier(), input);
        return null;
    }

    public Object transformObjectToParameter(List<IProcessor> value) {
        if (value == null) return "";
        List<String> prototypeIdentifiers = new ArrayList<>();

        for (IProcessor p : value) {
            prototypeIdentifiers.add(p.getFullBeanId());
        }

        if (!prototypeIdentifiers.isEmpty()) {
            log.debug("{} transformed processor list to {}", this.getIdentifier(), StringUtils.collectionToDelimitedString(prototypeIdentifiers, delimiter));
            return StringUtils.collectionToDelimitedString(prototypeIdentifiers, delimiter);
        }

        return null;
    }

    @Override
    public String getTypeSignature() {
        return "list<processor>";
    }

    @Override
    public boolean isGeneric() {
        return true;
    }
}