package de.starima.pfw.base.processor.description.config;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.annotation.ValueObject;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.config.api.*;
import de.starima.pfw.base.util.MapUtils;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Processor(description = "Erzeugt eine IDescriptorConfig fÃ¼r ValueObjects, indem es Annotationen aus der Klassenhierarchie mergt.")
public class ValueObjectDescriptorConfigProvider extends AbstractProcessor implements IDescriptorConfigProvider {
    public boolean isResponsibleFor(IDescriptorConfigCreationContext context) {
        Type type = getTypeFromContext(context);
        return (type instanceof Class) && ProcessorUtils.isConsideredValueObject((Class<?>) type);
    }

    private Type getTypeFromContext(IDescriptorConfigCreationContext context) {
        if (context.getTypeToResolve() != null) return context.getTypeToResolve();
        if (context.getObjectToResolve() != null) return context.getObjectToResolve().getClass();
        return null;
    }

    @Override
    public IDescriptorConfig provide(IDescriptorConfigCreationContext context) {
        if (!isResponsibleFor(context)) return null;

        if (context.getTypeRefRootProvider() == null) {
            log.error("{}: there is no typeRefProvider set in context!", this.getFullBeanId());
        }
        Class<?> processorClass = (Class<?>) getTypeFromContext(context);

        // 1. Finde alle relevanten Annotationen in der Hierarchie.
        List<ValueObject> processorAnnotations = ProcessorUtils.findAnnotationsInHierarchy(processorClass, ValueObject.class);

        // 2. Kehre die Liste um, um von der allgemeinsten zur spezifischsten zu mergen.
        Collections.reverse(processorAnnotations);

        // 3. Erstelle das finale Konfigurationsobjekt und befÃ¼lle es.
        //Falls dieses ValueConfig Object zu einem Parameter gehÃ¶rt, verwenden wir die zugehÃ¶rige ParameterProcessor Annotation,
        //um weitere Infos zu setzen (requieredCategories etc. ...)
        //siehe auch ProcessorParameterDescriptorConfigProvider
        ValueObjectConfig processorConfig = mergeProcessorAnnotations(processorClass, context.getProcessorParameter());

        // setze die TypeRef
        DefaultTypeResolutionContext typeResolutionContext = new DefaultTypeResolutionContext();
        typeResolutionContext.setParentContext(context);
        typeResolutionContext.setRuntimeContext(context.getRuntimeContext());
        typeResolutionContext.setTypeToResolve(processorClass);


        ITypeRef typeRef = context.getTypeRefRootProvider().provide(typeResolutionContext);
        processorConfig.setTypeRef(typeRef);

        //Jetzt wird es interessant und rekursiv, es mÃ¼ssen die ParameterConfigs erzeugt werden (dazu benÃ¶tigen wir dann auch wieder den rootProvider
        //Ã¤hnlich wie bei der TypeRefProviderChain. AuÃŸerdem mÃ¼ssen wir die Input Parameter dieser Methode noch anpassen, denn fast jeder Wert stammt von einem Parameter.
        //Daher mÃ¼ssen wir auch die Infos der ProcessorParameter Annotation verarbeiten, denn diese hat z.B. requieredCategories, welche zu dem zugehÃ¶rigen ValueConfig gehÃ¶ren.
        // Schritt 3: Finde alle Felder und delegiere ihre Konfigurationserstellung an die Kette.
        Map<Field, ProcessorParameter> fields = ProcessorUtils.getAllAnnotatedParameterFields(processorClass);

        Map<String, IProcessorParameterConfig> parameterConfigs = fields.keySet().stream()
                .map(field -> {
                    // Erstelle einen Kind-Kontext fÃ¼r jedes Feld
                    DefaultDescriptorConfigCreationContext subContext = new DefaultDescriptorConfigCreationContext(context);
                    subContext.setFieldToResolve(field); // Das zu analysierende Item ist das Feld
                    return context.getRootProvider().provide(subContext);
                })
                .filter(Objects::nonNull)
                .filter(IProcessorParameterConfig.class::isInstance)
                .map(IProcessorParameterConfig.class::cast)
                // Sammle die Parameter in einer Map mit dem Property-Namen als SchlÃ¼ssel
                .collect(Collectors.toMap(IProcessorParameterConfig::getPropertyName, config -> config));

        processorConfig.setParameterConfigs(parameterConfigs);


        return processorConfig;
    }

    protected ValueObjectConfig mergeProcessorAnnotations(Class<?> processorClass, ProcessorParameter parameterAnnotation) {
        // ... Ihre bestehende Logik zum Mergen der Annotationen ...
        // Diese Methode bleibt unverÃ¤ndert.
        List<ValueObject> processorAnnotations = ProcessorUtils.findAnnotationsInHierarchy(processorClass, ValueObject.class);
        Collections.reverse(processorAnnotations);
        ValueObjectConfig processorConfig = new ValueObjectConfig();
        processorConfig.setCategories(new ArrayList<>());
        processorConfig.setSubCategories(new ArrayList<>());
        processorConfig.setTags(new ArrayList<>());
        processorConfig.setDefaultValues(new HashMap<>());

        if (parameterAnnotation != null) {
            processorConfig.setRequiredCategories(List.of(parameterAnnotation.requiredCategories()));
            processorConfig.setRequiredSubCategories(List.of(parameterAnnotation.requiredSubCategories()));
            processorConfig.setRequiredTags(List.of(parameterAnnotation.requiredTags()));
        }

        for (ValueObject p : processorAnnotations) {
            if (p.description() != null && !p.description().isEmpty()) {
                processorConfig.setDescription(p.description());
            }
            if (p.descriptionAssetName() != null && !p.descriptionAssetName().isEmpty()) {
                processorConfig.setDescriptionAssetName(p.descriptionAssetName());
            }
            if (p.descriptionAssetMimetype() != null && !p.descriptionAssetMimetype().isEmpty()) {
                processorConfig.setDescriptionAssetMimetype(p.descriptionAssetMimetype());
            }
            if (p.assetProviderIdentifier() != null && !p.assetProviderIdentifier().isEmpty()) {
                processorConfig.setAssetProviderIdentifier(p.assetProviderIdentifier());
            }
            if (p.descriptorPrototypeIdentifier() != null && !p.descriptorPrototypeIdentifier().isEmpty()) {
                processorConfig.setDescriptorPrototypeIdentifier(p.descriptorPrototypeIdentifier());
            }

            if (p.categories() != null && p.categories().length > 0) {
                processorConfig.getCategories().addAll(List.of(p.categories()));
            }
            if (p.subCategories() != null && p.subCategories().length > 0) {
                processorConfig.getSubCategories().addAll(List.of(p.subCategories()));
            }
            if (p.tags() != null && p.tags().length > 0) {
                processorConfig.getTags().addAll(List.of(p.tags()));
            }
            if (p.defaultValues() != null && p.defaultValues().length > 0) {
                Map<String, Object> newDefaults = ProcessorUtils.convertDefaultValuesToMap(p.defaultValues());
                MapUtils.mergeMaps(processorConfig.getDefaultValues(), newDefaults);
            }
        }
        return processorConfig;
    }
}