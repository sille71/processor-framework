package de.starima.pfw.base.processor.context.domain;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.IDescriptorConstructorContext;
import de.starima.pfw.base.processor.context.api.LoadStrategy;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;


@Getter
@Setter
@NoArgsConstructor
public class DefaultDescriptorConstructorContext extends DefaultTaskContext implements IDescriptorConstructorContext {
    private String parentDescriptorIdentifier = "";
    private Field sourceField;
    private Object sourceObject;
    private Class<?> sourceType;
    private LoadStrategy loadStrategy = LoadStrategy.LAZY;
    private String descriptorPrototypeIdentifier;
    private ProcessorParameter processorParameterAnnotation;
    private Optional<IDescriptorProcessor> parentDescriptor;
    private Optional<IProcessor> parentProcessor;
    private Set<Class<?>> visitedTypes = new HashSet<>();

    /**
     * NEU: Privater Copy-Konstruktor.
     * Erstellt eine flache Kopie des Ã¼bergebenen Kontextes.
     * @param other Der zu klonende Kontext.
     */
    private DefaultDescriptorConstructorContext(DefaultDescriptorConstructorContext other) {
        // Kopiere alle Felder 1:1
        this.parentDescriptorIdentifier = other.parentDescriptorIdentifier;
        this.setRuntimeContext(other.getRuntimeContext());
        this.sourceField = other.sourceField;
        this.sourceObject = other.sourceObject;
        this.sourceType = other.sourceType;
        this.descriptorPrototypeIdentifier = other.descriptorPrototypeIdentifier;
        this.parentDescriptor = other.parentDescriptor;
        this.parentProcessor = other.parentProcessor;
        this.processorParameterAnnotation = other.processorParameterAnnotation;
        this.visitedTypes = new HashSet<>(other.visitedTypes);
    }

    /**
     * NEU: Statische Factory-Methode zum Klonen.
     *
     * @param source Der zu klonende Kontext. Darf nicht null sein.
     * @return Eine neue Instanz mit den gleichen Werten wie die Quelle.
     */
    public static DefaultDescriptorConstructorContext clone(IDescriptorConstructorContext source) {
        Objects.requireNonNull(source, "Der zu klonende Quell-Kontext darf nicht null sein.");

        if (source instanceof DefaultDescriptorConstructorContext) {
            // Nutze den effizienten Copy-Konstruktor
            return new DefaultDescriptorConstructorContext((DefaultDescriptorConstructorContext) source);
        } else {
            // Fallback, falls eine andere Implementierung von IDescriptorConstructorContext Ã¼bergeben wird
            DefaultDescriptorConstructorContext clone = new DefaultDescriptorConstructorContext();
            clone.setRuntimeContext(source.getRuntimeContext());
            clone.setParentDescriptorIdentifier(source.getParentDescriptorIdentifier());
            clone.setSourceField(source.getSourceField());
            clone.setSourceObject(source.getSourceObject());
            clone.setSourceType(source.getSourceType());
            clone.setParentDescriptor(source.getParentDescriptor());
            clone.setParentProcessor(source.getParentProcessor());
            clone.setDescriptorPrototypeIdentifier(source.getDescriptorPrototypeIdentifier());
            clone.setProcessorParameterAnnotation(source.getProcessorParameterAnnotation());
            clone.setVisitedTypes(new HashSet<>(source.getVisitedTypes()));
            return clone;
        }
    }


    public String getParentDescriptorIdentifier() {
        return parentDescriptor.isPresent() ? parentDescriptor.get().getFullBeanId() : parentDescriptorIdentifier;
    }

    @Override
    public String toString() {
        return "DefaultDescriptorConstructorContext{" +
               "sourceField=" + sourceField +
               ", sourceObject=" + sourceObject +
               ", sourceType=" + sourceType +
               ", parentDescriptor=" + parentDescriptor +
               ", parentProcessor=" + parentProcessor +
               '}';
    }



}