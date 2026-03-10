package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.exception.InvalidVersionNumberException;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.description.api.IValueFunction;
import de.starima.pfw.base.processor.set.api.ISetProcessor;
import de.starima.pfw.base.util.ArtifactIdentifier;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

//TODO: kann durch ValueObjectValueFunction ersetzt werden (dann muss ArtifactIdentifier die Annotation ValueObject haben)

@Slf4j
@Getter
@Setter
@Processor
public class ArtifactIdentifierValueFunction extends AbstractValueFunction<Object, ArtifactIdentifier> implements IValueFunction<ITransformationContext, Object, ArtifactIdentifier> {
    public static boolean isResponsibleFor(ITransformationContext transformationContext) {
        if (transformationContext == null) return false;
        Class<?> clazz = transformationContext.getRawType();
        Field field = transformationContext.getFieldToResolve();
        Class<?> valueType = field != null ? field.getType() : clazz;
        return valueType != null && ArtifactIdentifier.class.isAssignableFrom(valueType);
    }

    //TODO: hier defaultwerte angeben! Die set Processoren benÃ¶tigen wir im ReconLight
    @ProcessorParameter(description = "Definitionsbereich der Parameter. Dies kÃ¶nnen Integer oder auch Strings sein. ")
    private ISetProcessor<Object> domainProcessor;
    @ProcessorParameter
    private ISetProcessor<ArtifactIdentifier> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<ArtifactIdentifier> imageProcessor;

    @Override
    public ISetProcessor<Object> getDomainProcessor() {
        return domainProcessor;
    }

    @Override
    public ISetProcessor<ArtifactIdentifier> getCoDomainProcessor() {
        return coDomainProcessor;
    }

    @Override
    public ISetProcessor<ArtifactIdentifier> getImageProcessor() {
        return imageProcessor;
    }

    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return isResponsibleFor(transformationContext);
    }

    @Override
    public ArtifactIdentifier transformValue(Object input) {
        if (!isResponsibleForInput(input)) {
            log.warn("{}.transformValue: Can not transform value {}! Value is not in domain.", this.getIdentifier(), input);
            return null;
        }
        try {
            if (input instanceof String) {
                return new ArtifactIdentifier(input.toString());
            }
        } catch (InvalidVersionNumberException e) {
            log.error("{}.transformValue: Can not transform value {}, invalid version String!", this.getIdentifier(), input, e);
        }
        log.warn("{}.transformValue: Can not transform value {}! Value is properly not in domain.", this.getIdentifier(), input);
        return null;
    }

    public Object transformObjectToParameter(ArtifactIdentifier value) {
        if (value == null) return "";
        return value.toString();
    }

    @Override
    public String getTypeSignature() {
        return "artifactIdentifier";
    }

    @Override
    public boolean isGeneric() {
        return false;
    }
}