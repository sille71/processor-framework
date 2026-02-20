package de.starima.pfw.base.processor.description.config;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.dzbank.recon.ms.base.processor.description.config.api.*;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfig;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfigCreationContext;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfigProvider;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.List;

@Slf4j
@Processor(description = "Erzeugt eine ProcessorParameterConfig fÃ¼r ein Feld, das mit @ProcessorParameter annotiert ist.")
public class ProcessorParameterDescriptorConfigProvider extends AbstractProcessor implements IDescriptorConfigProvider {

    @Override
    public boolean isResponsibleFor(IDescriptorConfigCreationContext context) {
        // Wir sind zustÃ¤ndig, wenn das zu untersuchende Item ein Feld mit der richtigen Annotation ist.
        return (context.getFieldToResolve() instanceof Field) && ((Field) context.getFieldToResolve()).isAnnotationPresent(ProcessorParameter.class);
    }

    @Override
    public IDescriptorConfig provide(IDescriptorConfigCreationContext context) {
        if (!isResponsibleFor(context)) return null;

        Field field = (Field) context.getFieldToResolve();
        ProcessorParameter annotation = field.getAnnotation(ProcessorParameter.class);

        ProcessorParameterConfig config = new ProcessorParameterConfig();

        // 1. FÃ¼lle die einfachen Werte aus der Annotation.
        config.setPropertyName(field.getName());
        config.setParameterName(annotation.name().isEmpty() ? field.getName() : annotation.name());
        config.setDescription(annotation.description());
        config.setDescriptionAssetName(annotation.descriptionAssetName());
        config.setDescriptionAssetMimetype(annotation.descriptionAssetMimetype());
        config.setAssetProviderIdentifier(annotation.assetProviderIdentifier());
        config.setAliases(List.of(annotation.aliases()));
        config.setInput(annotation.isInput());
        config.setOutput(annotation.isOutput());
        config.setKey(annotation.key());
        config.setIgnoreExtractParameter(annotation.ignoreExtractParameter());
        config.setIgnoreInitialization(annotation.ignoreInitialization());
        config.setIgnoreRefresh(annotation.ignoreRefresh());
        config.setParameterDefaultValue(annotation.value());
        config.setDescriptorPrototypeIdentifier(annotation.parameterDescriptorPrototypeIdentifier());
        config.setDescriptorIdentifier(annotation.parameterDescriptorIdentifier());
        config.setRequired(annotation.required());

        // 2. Rekursiver Aufruf der Descriptor-Kette, um die Descriptoren der Werte zu ermitteln
        DefaultDescriptorConfigCreationContext subContext = new DefaultDescriptorConfigCreationContext(context);
        subContext.setObjectToResolve(null);
        subContext.setTypeToResolve(field.getGenericType());
        subContext.setProcessorParameter(annotation);
        subContext.setFieldToResolve(null);

        IDescriptorConfig valueDescriptor = context.getRootProvider().provide(subContext);
        if (valueDescriptor != null) {
            valueDescriptor.asValueConfig().ifPresent(config::setValueConfig);
        } else {
            log.warn("FÃ¼r den Parameter '{}' konnte keine IValueConfig fÃ¼r den Typ '{}' erzeugt werden. MÃ¶glicherweise fehlt ein entsprechender Provider (z.B. ScalarConfigProvider).", config.getParameterName(), field.getGenericType().getTypeName());
        }

        return config;
    }
}