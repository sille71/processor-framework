package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.api.IBeanProvider;
import de.starima.pfw.base.processor.description.api.IValueFunction;
import de.starima.pfw.base.processor.description.api.IValueFunctionResolver;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceCreationContext;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

/**
 * Löst eine ValueFunction auf, die explizit über die {@code @ProcessorParameter}-Annotation
 * konfiguriert wurde ({@code valueFunctionIdentifier} / {@code valueFunctionPrototypeIdentifier}).
 *
 * <p>Dies ist die höchste Priorität: Wenn der Entwickler eine ValueFunction
 * explizit konfiguriert hat, wird diese verwendet — unabhängig davon, ob eine
 * dynamische Suche eine andere finden würde.
 *
 * <p><b>Auflösungslogik:</b>
 * <ol>
 *   <li>Prüfe {@code context.getProcessorParameter()} auf valueFunctionIdentifier</li>
 *   <li>Baue fullBeanId aus prototypeId + identifier</li>
 *   <li>Hole die Bean via BeanProvider</li>
 *   <li>Bestätige Zuständigkeit via {@code isResponsibleForSubject()}</li>
 * </ol>
 */
@Slf4j
@Getter
@Setter
@Order(10)
@Processor(
        description = "Löst eine ValueFunction auf, die explizit über @ProcessorParameter " +
                "(valueFunctionIdentifier/valueFunctionPrototypeIdentifier) konfiguriert wurde. " +
                "Höchste Priorität in der ValueFunctionResolverChain.",
        categories = {"incubator", "resolver"},
        tags = {"valueFunction", "annotation", "explicit", "configuration"}
)
public class AnnotationValueFunctionResolver extends AbstractProcessor implements IValueFunctionResolver {

    @ProcessorParameter(description = "BeanProvider für den Lookup der konfigurierten ValueFunction")
    private IBeanProvider beanProvider;

    @Override
    public boolean isResponsibleFor(IInstanceCreationContext context) {
        if (context.getProcessorParameter() == null) return false;
        de.starima.pfw.base.annotation.ProcessorParameter annotation = context.getProcessorParameter();
        String fullBeanId = ProcessorUtils.createFullBeanId(
                annotation.valueFunctionPrototypeIdentifier(),
                annotation.valueFunctionIdentifier(),
                null);
        return StringUtils.hasLength(fullBeanId);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public IValueFunction resolve(IInstanceCreationContext context) {
        de.starima.pfw.base.annotation.ProcessorParameter annotation = context.getProcessorParameter();
        String fullBeanId = ProcessorUtils.createFullBeanId(
                annotation.valueFunctionPrototypeIdentifier(),
                annotation.valueFunctionIdentifier(),
                null);

        try {
            IValueFunction vf = beanProvider.getBeanForId(IValueFunction.class, fullBeanId);
            if (vf != null) {
                log.debug("ValueFunction '{}' via Annotation aufgelöst", fullBeanId);
                return vf;
            }
        } catch (Exception e) {
            log.warn("ValueFunction '{}' aus Annotation konnte nicht instantiiert werden: {}",
                    fullBeanId, e.getMessage());
        }

        return null;
    }
}