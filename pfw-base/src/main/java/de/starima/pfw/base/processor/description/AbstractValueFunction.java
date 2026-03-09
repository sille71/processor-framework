package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.api.IValueFunction;
import de.starima.pfw.base.processor.transformator.api.ISubjectFunction;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Getter
@Setter
@Processor
public abstract class AbstractValueFunction<I,O> extends DescriptorProcessor implements IValueFunction<ITransformationContext,I,O> {

    @ProcessorParameter
    private ISubjectFunction<ITransformationContext,O,I> reverseFunction;

    @ProcessorParameter(description = "Liefert den sprachunabhÃ¤ngigen Identifier des generischen Typs (des Zielwertes O). Wird in ProcessorUtils bei der Erzeugung des PrototypeDescriptors ermittelt und sollte auch nicht Ã¼berschrieben werden!", ignoreInitialization = true)
    private String typeIdentifier;

    /**
     * Gibt an, ob dieser Deskriptor ein polymorpher Platzhalter ist (z.B. fÃ¼r ein Interface
     * oder eine abstrakte Klasse) und zur Laufzeit eine Auswahl an konkreten Implementierungen
     * anbieten muss. Wenn 'false', reprÃ¤sentiert der Deskriptor einen konkreten, finalen Typ.
     * Dieses Feld wird von ProcessorUtils bei der Erzeugung des Blueprints gesetzt.
     */
    @ProcessorParameter(description = "Gibt an, ob der Deskriptor ein polymorpher Platzhalter ist.", ignoreInitialization = true)
    private boolean polymorphic = false; // Default auf false setzen

    @Override
    public ISubjectFunction<ITransformationContext,O,I> getReverseFunction() {
        return reverseFunction;
    }
    @Override
    public boolean isResponsibleForInput(I input) {
        return getDomainProcessor() != null ? getDomainProcessor().isMember(input) : true;
    }

    @Override
    public I reverseTransformValue(O value) {
        return getReverseFunction() != null ? getReverseFunction().transformValue(value) : transformObjectToParameter(value);
    }

    @Override
    public I reverseTransformValue(ITransformationContext transformationContext,O value) {
        return getReverseFunction() != null ? getReverseFunction().transformValue(transformationContext, value) : transformObjectToParameter(transformationContext, value);
    }

    public I transformObjectToParameter(O value) {
        log.warn("{} transformObjectToParameter is not implemented!");
        return null;
    }

    public I transformObjectToParameter(ITransformationContext transformationContext, O value) {
        return transformObjectToParameter(value);
    }

    @Override
    public O transformValue(ITransformationContext transformationContext, I input) {
        return transformValue(input);
    }

    @Override
    public Map<String, Map<String, Object>> extractEffectiveParameterMap(Object value) {
        return null;
    }

    @Override
    public Map<String, Map<String, Object>> extractEffectiveParameterMap(ITransformationContext transformationContext, Object value) {
        return extractEffectiveParameterMap(value);
    }
    public void extractEffectiveParameterMap(ITransformationContext context, Map<String, Map<String, Object>> beanParameterMap, Set<Object> visited) {
        if (this.getParentDescriptor() != null) {
            this.getParentDescriptor().extractEffectiveParameterMap(context, beanParameterMap, visited);
        }
    }

    @Override
    public String getPath() {
        //TODO: ausimplementieren, falls nÃ¶tig
        return "";
    }

    @Override
    public Optional<IDescriptorProcessor> findDescriptor(String path) {
        //TODO: ausimplementieren, falls nÃ¶tig (aktuell kein Bedarf)
        return Optional.empty();
    }
}