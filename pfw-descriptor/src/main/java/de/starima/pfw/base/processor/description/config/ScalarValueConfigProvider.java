package de.starima.pfw.base.processor.description.config;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfig;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfigCreationContext;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfigProvider;
import de.starima.pfw.base.processor.description.config.api.ITypeRef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Type;
import java.util.List;

@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@Processor(description = "Erzeugt eine IValueConfig fÃ¼r skalare Typen wie String, Integer, Boolean, Enums etc.")
public class ScalarValueConfigProvider extends AbstractProcessor implements IDescriptorConfigProvider {

    @Override
    public boolean isResponsibleFor(IDescriptorConfigCreationContext context) {
        Type type = context.getTypeToResolve();
        if (type == null) return false;

        DefaultTypeResolutionContext typeResolutionContext = new DefaultTypeResolutionContext();
        typeResolutionContext.setParentContext(context);
        typeResolutionContext.setRuntimeContext(context.getRuntimeContext());
        typeResolutionContext.setTypeToResolve(type);
        ITypeRef typeRef = context.getTypeRefRootProvider().provide(typeResolutionContext);
        return typeRef.isScalar();
    }

    @Override
    public IDescriptorConfig provide(IDescriptorConfigCreationContext context) {
        // Die isResponsibleFor-PrÃ¼fung wird von der Kette aufgerufen.
        // Wir kÃ¶nnen davon ausgehen, dass sie hier true war.

        Type type = context.getTypeToResolve();
        log.debug("ScalarValueConfigProvider ist zustÃ¤ndig fÃ¼r Typ: {}", type.getTypeName());

        ValueConfig scalarConfig = new ValueConfig();

        DefaultTypeResolutionContext typeResolutionContext = new DefaultTypeResolutionContext();
        typeResolutionContext.setParentContext(context);
        typeResolutionContext.setRuntimeContext(context.getRuntimeContext());
        typeResolutionContext.setTypeToResolve(type);
        ITypeRef typeRef = context.getTypeRefRootProvider().provide(typeResolutionContext);

        scalarConfig.setTypeRef(typeRef);
        if (type instanceof Class) {
            scalarConfig.setTargetType((Class<?>) type);
        }

        ProcessorParameter parameterAnnotation = context.getProcessorParameter();
        if (parameterAnnotation != null) {
            scalarConfig.setRequiredCategories(List.of(parameterAnnotation.requiredCategories()));
            scalarConfig.setRequiredSubCategories(List.of(parameterAnnotation.requiredSubCategories()));
            scalarConfig.setRequiredTags(List.of(parameterAnnotation.requiredTags()));
        }

        return scalarConfig;
    }
}