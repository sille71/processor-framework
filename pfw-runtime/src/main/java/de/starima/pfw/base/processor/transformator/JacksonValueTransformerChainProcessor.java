package de.starima.pfw.base.processor.transformator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.transformator.api.IValueTransformerProcessor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

@Slf4j
@Processor
public class JacksonValueTransformerChainProcessor extends AbstractProcessor implements IValueTransformerProcessor {
    @ProcessorParameter(description = "Liste der Transformer, die die Transformation Ã¼bernehmen kÃ¶nnen.", value = "jacksonGenericListValueTransformerProcessor, jacksonValueTransformerProcessor")
    private List<IValueTransformerProcessor> transformerProcessors;

    @Override
    public Object transformValue(Field field,Object value) {
        if (transformerProcessors == null) return null;
        Optional<IValueTransformerProcessor> optional = transformerProcessors.stream().filter(transformer -> transformer.isResponsibleForSubject(field)).findAny();
        return optional.get() != null ? optional.get().transformValue(field,value) : null;
    }

    @Override
    public boolean isResponsibleForSubject(Field field) {
        return transformerProcessors != null;
    }

    @Override
    public boolean isResponsibleForInput(Object input) {
        return true;
    }

    @Override
    public Object transformValue(Object input) {
        return null;
    }
}