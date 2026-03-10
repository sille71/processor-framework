package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.IProposalContext;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.context.domain.DefaultTransformationContext;
import de.starima.pfw.base.processor.description.api.*;
import de.starima.pfw.base.processor.description.incubator.api.IDescriptorResolver;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Field;
import java.util.*;

@Getter
@Setter
@Slf4j
@Processor
public class DefaultParameterDescriptor extends DescriptorProcessor implements IParameterDescriptor {

    //TODO kann evtl. weg
    @ProcessorParameter(value = "defaultDescriptorResolver", description = "Der Resolver, der alle mÃ¶glichen Kandidaten fÃ¼r diesen Parameter findet.")
    private IDescriptorResolver descriptorResolver;

    private HashMap<String, IDescriptorProcessor> descriptorCache;

    protected HashMap<String,IDescriptorProcessor> getDescriptorCache() {
        if (descriptorCache == null) {
            descriptorCache = new HashMap<>();
        }
        return descriptorCache;
    }

    @ProcessorParameter
    private String parameterName;
    @ProcessorParameter
    private String propertyName;
    @ProcessorParameter
    private String defaultValue;
    @ProcessorParameter
    private IValueDescriptor valueDescriptor;
    @ProcessorParameter
    private boolean required = false;
    @ProcessorParameter
    private boolean ignoreInitialization = false;
    @ProcessorParameter
    private boolean ignoreRefresh = false;
    @ProcessorParameter
    private boolean ignoreExtractParameter = false;

    @ProcessorParameter
    private boolean input = true;
    @ProcessorParameter
    private boolean output = true;

    @ProcessorParameter(description = "Wenn auf 'true' gesetzt, wird dieses Feld als Teil des\n" +
            "\t  zusammengesetzten SchlÃ¼ssels zur Identifizierung einer @ValueObject-Instanz verwendet.\n" +
            "\t Dies wird nur berÃ¼cksichtigt, wenn das ValueObject nicht das IArtifact-Interface implementiert.")
    private boolean key;

    @Override
    public void processorOnInit() {
        if (this.valueDescriptor != null) {
            this.valueDescriptor.setParentDescriptor(this);
        }
    }

    public String getParameterName() {
        return parameterName != null ? parameterName : propertyName;
    }

    @Override
    public List<IValueDescriptor> getPossibleValueDescriptors(IProposalContext context) {
        if (descriptorResolver == null) {
            log.error("DescriptorResolver is not set for ParameterDescriptor '{}'. Cannot provide possible value descriptors.", getParameterName());
            return new ArrayList<>();
        }

        List<IValueDescriptor> possibleValues = new ArrayList<>();

        //TODO: ist noch auszuimplementieren
        return possibleValues;
    }

    private IValueDescriptor createDescriptorFromMap(Map<String, Map<String, Object>> descriptorMap) {
        String descriptorFullBeanId = descriptorMap.keySet().stream().findFirst().orElse(null);
        if (descriptorFullBeanId == null) {
            log.warn("Cannot create descriptor from map: no main bean ID found.");
            return null;
        }

        IProcessor currentProcessor = getRuntimeContext().getInitializedProcessor();
        if (currentProcessor == null) {
            log.error("Cannot create descriptor from map: current processor (factory) is null.");
            return null;
        }

        try {
            return currentProcessor.createProcessor(
                IValueDescriptor.class,
                descriptorFullBeanId,
                null,
                Collections.singletonList(descriptorMap)
            );
        } catch (Exception e) {
            log.error("Failed to create IValueDescriptor from map for ID '{}': {}", descriptorFullBeanId, e.getMessage(), e);
            return null;
        }
    }

    private boolean matchesExplicitly(IStructureValueDescriptor candidate) {
        if (descriptorResolver == null) {
            log.error("DescriptorResolver is not defined for ParameterDescriptor '{}'. Cannot perform explicit matching.", getIdentifier());
            return false;
        }

        //boolean categoryMatch = descriptorResolver.matchesCategories(candidate.getCategories(), getRequiredCategories());
        //boolean tagMatch = descriptorResolver.matchesTags(candidate.getTags(), getRequiredTags());
        //boolean subCategoryMatch = descriptorResolver.matchesSubCategories(candidate.getSubCategories(), getRequiredSubCategories());

        //return categoryMatch && tagMatch && subCategoryMatch;
        return false;
    }

    private boolean matchesImplicitly(IValueDescriptor candidate) {
        Field describedField = null;
        //TODO: noch auszuimplementieren

        if (describedField == null) return false;

        IValueFunction candidateValueFunction = candidate.getValueFunction();
        if (candidateValueFunction == null) return false;

        return candidateValueFunction.isResponsibleForSubject(Pair.of(null, describedField));
    }

    protected Object getParameterValue(Map<String, Object> parameters) {
        String parameterName = getParameterName();
        Object parameterValue = parameters.get(parameterName);

        if (parameterValue == null) {
            parameterName = parameterName + "Identifier";
            parameterValue = parameters.get(parameterName);
        }

        if (parameterValue == null) {
            parameterValue = getDefaultValue();
        }
        return parameterValue;
    }

    @Override
    public void initBeanParameters(Object bean, Map<String, Object> parameters) {
        if (bean == null || isIgnoreInitialization()) return;

        Object parameterValue = getParameterValue(parameters);
        if (parameterValue == null) return;

        if (getValueDescriptor() != null && getValueDescriptor().getValueFunction() != null) {
            try {
                Field field = ProcessorUtils.getDeclaredFieldForProcessorClass(bean.getClass(), getPropertyName());
                if (field != null) {
                    DefaultTransformationContext transformationContext = new DefaultTransformationContext();
                    transformationContext.setTargetObject(bean);
                    transformationContext.setTargetField(field);
                    Object transformedValue = getValueDescriptor().getValueFunction().transformValue(transformationContext, parameterValue);
                    field.set(bean, transformedValue);
                }
            } catch (Exception e) {
                log.error("Fehler beim Initialisieren des Parameters '{}'", getParameterName(), e);
            }
        }
    }

    public void bindEffectiveParameterValues(Object bean) {
        if (bean == null) return;
        try {
            if (getValueDescriptor() != null && getValueDescriptor().getValueFunction() != null) {
                Field field = ProcessorUtils.getDeclaredFieldForProcessorClass(bean.getClass(),getPropertyName());
                DefaultTransformationContext transformationContext = new DefaultTransformationContext();
                transformationContext.setTargetObject(bean);
                transformationContext.setTargetField(field);
                getValueDescriptor().setRawValue(getValueDescriptor().getValueFunction().reverseTransformValue(transformationContext, field.get(bean)));
            }
        } catch (IllegalAccessException e) {
            log.warn("{}.extractEffectiveBeanParameters: {} can not get value from field {}!",getIdentifier(), bean, getPropertyName());
        }
    }

    @Override
    public Map<String, Object> extractEffectiveParameters(Object bean) {
        if (bean == null) return null;
        HashMap<String, Object> parameters = new HashMap<>();
        try {
            if (getValueDescriptor() != null && getValueDescriptor().getValueFunction() != null && !isIgnoreExtractParameter()) {
                Field field = ProcessorUtils.getDeclaredFieldForProcessorClass(bean.getClass(),getPropertyName());
                DefaultTransformationContext transformationContext = new DefaultTransformationContext();
                transformationContext.setTargetObject(bean);
                transformationContext.setTargetField(field);
                parameters.put(getParameterName(), getValueDescriptor().getValueFunction().reverseTransformValue(transformationContext, field.get(bean)));
            }
        } catch (IllegalAccessException e) {
            log.warn("{}.extractEffectiveBeanParameters: {} can not get value from field {}!",getIdentifier(), bean, getPropertyName());
        }
        return parameters;
    }

    @Override
    public Map<String, Object> extractEffectiveParametersForResolvedValue() {
        HashMap<String, Object> parameters = new HashMap<>();
        try {
            if (getValueDescriptor() != null && !isIgnoreExtractParameter()) {
                parameters.put(getParameterName(), getValueDescriptor().createRawValueFromResolvedValue());
            }
        } catch (Exception e) {
            log.warn("{}.extractEffectiveParametersFoResolvedValue: can not get value from parameter {}!",getIdentifier(), getPropertyName());
        }
        return parameters;
    }

    @Override
    public void extractEffectiveParameterMap(ITransformationContext context, Map<String, Map<String, Object>> beanParameterMap, Set<Object> visited) {
        // Ein ParameterDescriptor selbst hat keine Parameter. Seine einzige Aufgabe ist es,
        // den Aufruf an seinen ValueDescriptor weiterzuleiten, aber mit dem korrekten Kontext.

        if (getValueDescriptor() == null) {
            // Nichts zu tun, wenn kein Wert beschrieben wird.
            return;
        }

        Object parentBean = context.getTargetObject();
        if (parentBean == null) {
            return;
        }

        try {
            // 1. Hole den Wert dieses Parameters aus dem Eltern-Bean.
            Field field = ProcessorUtils.getDeclaredFieldForProcessorClass(parentBean.getClass(), getPropertyName());
            Object childValue = field.get(parentBean);

            // 2. Erzeuge einen neuen, prÃ¤zisen Kontext fÃ¼r diesen Wert.
            DefaultTransformationContext childContext = new DefaultTransformationContext();
            childContext.setTargetObject(childValue);
            childContext.setTargetField(field);
            childContext.setProcessorParameterAnnotation(field.getAnnotation(ProcessorParameter.class));
            childContext.setRuntimeContext(context.getRuntimeContext());
            childContext.setLoadStrategy(context.getLoadStrategy());

            // 3. Leite den Aufruf an den ValueDescriptor weiter.
            getValueDescriptor().extractEffectiveParameterMap(childContext, beanParameterMap, visited);

        } catch (Exception e) {
            log.error("Fehler bei der Weiterleitung der Extraktion fÃ¼r Parameter '{}'", getParameterName(), e);
        }
    }

    @Override
    public Map<String, Map<String, Object>> extractEffectiveParameterMap(Object bean) {
        if (bean == null) return null;
        try {
            if (getValueDescriptor() != null && getValueDescriptor().getValueFunction() != null && !isIgnoreExtractParameter()) {
                Field field = ProcessorUtils.getDeclaredFieldForProcessorClass(bean.getClass(),getPropertyName());
                DefaultTransformationContext transformationContext = new DefaultTransformationContext();
                transformationContext.setTargetObject(bean);
                transformationContext.setTargetField(field);
                return getValueDescriptor().getValueFunction().extractEffectiveParameterMap(transformationContext, field.get(bean));
            }
        } catch (IllegalAccessException e) {
            log.warn("{}.extractEffectiveBeanParameters: {} can not get value from field {}!",getIdentifier(), bean, getPropertyName());
        }
        return null;
    }

    @Override
    public Map<String, Map<String, Object>> extractEffectiveParameterMap(ITransformationContext transformationContext, Object bean) {
        return this.extractEffectiveParameterMap(bean);
    }

    public Map<String, Map<String, Object>> extractEffectiveParameterMapForResolvedValue() {
        try {
            if (getValueDescriptor() != null && !isIgnoreExtractParameter()) {
                return getValueDescriptor().extractEffectiveParameterMapForResolvedValue();
            }
        } catch (Exception e) {
            log.warn("{}.extractEffectiveBeanParameters: can not get value from parameter {}!",getIdentifier(), getPropertyName());
        }
        return null;
    }

    @Override
    public String getJsonRepresentation(Object value) {
        return null;
    }

    @Override
    public String getPath() {
        // Der Pfad ist der Pfad des Eltern-Deskriptors plus der eigene Parametername.
        if (getParentDescriptor() == null) {
            // Sollte nicht vorkommen, wenn die Hierarchie korrekt aufgebaut ist.
            return getParameterName();
        }
        return getParentDescriptor().getPath() + "/" + getParameterName();
    }

    @Override
    public Optional<IDescriptorProcessor> findDescriptor(String path) {
        if (path == null || path.isEmpty()) {
            // Wenn der Pfad leer ist, sind wir am Ziel, aber die Suche sollte an den Wert delegiert werden.
            // Ein ParameterDescriptor selbst ist nur ein Container.
            return Optional.empty();
        }
        // Ein ParameterDescriptor selbst ist ein Blattknoten in der Parameter-Hierarchie.
        // Die weitere Traversierung findet innerhalb seines *Wertes* statt.
        // Daher delegieren wir die Suche direkt an den IValueDescriptor.
        if (getValueDescriptor() != null) {
            return getValueDescriptor().findDescriptor(path);
        }
        return Optional.empty();
    }
}