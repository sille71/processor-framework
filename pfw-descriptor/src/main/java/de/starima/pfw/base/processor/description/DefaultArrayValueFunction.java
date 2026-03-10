package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.description.api.ICollectionValueDescriptor;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.api.IValueDescriptor;
import de.starima.pfw.base.processor.set.api.ISetProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Stream;

@Getter @Setter
@Slf4j
@Processor(description = "Verarbeitet ein Array von Werten durch Delegation an eine Kind-Funktion.")
public class DefaultArrayValueFunction extends AbstractCollectionValueFunction<Object, Object[]> {
    public static boolean isResponsibleFor(ITransformationContext transformationContext) {
        if (transformationContext == null) return false;
        Class<?> clazz = transformationContext.getRawType();
        Field field = transformationContext.getFieldToResolve();
        Class<?> valueType = field != null ? field.getType() : clazz;
        return valueType != null && valueType.isArray();
    }

    @ProcessorParameter(description = "Definitionsbereich der Parameter. Komma separierte Liste von Prozessoridentifiern. ")
    private ISetProcessor<Object> domainProcessor;
    @ProcessorParameter
    private ISetProcessor<Object[]> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<Object[]> imageProcessor;

    @Override
    public ISetProcessor<Object> getDomainProcessor() {
        return domainProcessor;
    }

    @Override
    public ISetProcessor<Object[]> getCoDomainProcessor() {
        return coDomainProcessor;
    }

    @Override
    public ISetProcessor<Object[]> getImageProcessor() {
        return imageProcessor;
    }

    @Override
    public boolean isResponsibleForInput(Object input) {
        return isInputString(input) || isInputArrayOfObjects(input);
    }

    private boolean isInputString(Object input) {
        return input instanceof String;
    }

    //ist es eine Liste von Objekten?
    private boolean isInputArrayOfObjects(Object input) {
        if (input != null && input.getClass().isArray() && ((Object[]) input).length > 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return isResponsibleFor(transformationContext);
    }

    @Override
    public String getTypeSignature() {
        // Die Signatur ist dynamisch und wird ebenfalls Ã¼ber die Hierarchie ermittelt.
        IDescriptorProcessor parent = getParentDescriptor();
        if (parent instanceof ICollectionValueDescriptor) { // Annahme: Interface fÃ¼r den ListValueDescriptor
            ICollectionValueDescriptor listDescriptor = (ICollectionValueDescriptor) parent;
            IValueDescriptor elementDescriptor = listDescriptor.getElementValueDescriptor();
            if (elementDescriptor != null) {
                return "array<" + elementDescriptor.getTypeSignature() + ">";
            }
        }
        return "array";
    }

    @Override
    protected Object[] toTargetCollection(Stream<Object> stream) {
        return stream.toArray();
    }

    @Override
    protected Stream<?> toStream(Object[] array) {
        return Arrays.stream(array);
    }

    @Override
    public boolean isGeneric() {
        return false;
    }
}