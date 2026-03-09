package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.api.ICollectionValueDescriptor;
import de.starima.pfw.base.processor.description.api.IValueDescriptor;
import de.starima.pfw.base.processor.set.api.ISetProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter @Setter
@Slf4j
@Processor(description = "Verarbeitet eine Liste von Werten durch Delegation an eine Kind-Funktion.")
public class DefaultListValueFunction extends AbstractCollectionValueFunction<Object, List<Object>> {
    public static boolean isResponsibleFor(ITransformationContext transformationContext) {
        if (transformationContext == null) return false;
        Class<?> clazz = transformationContext.getTargetType();
        Field field = transformationContext.getTargetField();
        Class<?> valueType = field != null ? field.getType() : clazz;
        return valueType != null && List.class.isAssignableFrom(valueType);
    }

    @ProcessorParameter(description = "Definitionsbereich der Parameter. Komma separierte Liste von Prozessoridentifiern. ")
    private ISetProcessor<Object> domainProcessor;
    @ProcessorParameter
    private ISetProcessor<List<Object>> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<List<Object>> imageProcessor;

    @Override
    public ISetProcessor<Object> getDomainProcessor() {
        return domainProcessor;
    }

    @Override
    public ISetProcessor<List<Object>> getCoDomainProcessor() {
        return coDomainProcessor;
    }

    @Override
    public ISetProcessor<List<Object>> getImageProcessor() {
        return imageProcessor;
    }

    @Override
    public boolean isResponsibleForInput(Object input) {
        return isInputString(input) || isInputListOfObjects(input);
    }

    private boolean isInputString(Object input) {
        return input instanceof String;
    }

    //ist es eine Liste von Objekten?
    private boolean isInputListOfObjects(Object input) {
        if (input instanceof List && !((List) input).isEmpty()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return isResponsibleFor(transformationContext);
    }

    @Override
    protected List<Object> toTargetCollection(Stream<Object> stream) {
        return stream.collect(Collectors.toList());
    }

    @Override
    protected Stream<?> toStream(List<Object> collection) {
        return collection.stream();
    }

    @Override
    public String getTypeSignature() {
        // Die Signatur ist dynamisch und wird ebenfalls Ã¼ber die Hierarchie ermittelt.
        IDescriptorProcessor parent = getParentDescriptor();
        if (parent instanceof ICollectionValueDescriptor) { // Annahme: Interface fÃ¼r den ListValueDescriptor
            ICollectionValueDescriptor listDescriptor = (ICollectionValueDescriptor) parent;
            IValueDescriptor elementDescriptor = listDescriptor.getElementValueDescriptor();
            if (elementDescriptor != null) {
                return "list<" + elementDescriptor.getTypeSignature() + ">";
            }
        }
        return "list";
    }

    @Override
    public boolean isGeneric() {
        return false;
    }
}

