package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.api.IBeanProvider;
import de.starima.pfw.base.processor.description.api.IValueFunction;
import de.starima.pfw.base.processor.description.api.IValueFunctionResolver;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceCreationContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * Fallback-Resolver: Durchsucht alle registrierten {@link IValueFunction}-Beans
 * und findet die erste, die {@code isResponsibleForSubject()} bestätigt.
 *
 * <p>Dies ist die dynamische Suche, die bisher in
 * {@code ProcessorUtils.createValueFunctionForContext()} implementiert war.
 * Sie wird nur erreicht, wenn weder eine explizite Annotation noch ein
 * Descriptor eine ValueFunction liefert.
 *
 * <p><b>Vereinfacht:</b> Da {@link IInstanceCreationContext} nun
 * {@code ITransformationContext} erweitert, kann {@code context} direkt
 * an {@code isResponsibleForSubject()} übergeben werden — keine Bridge-Klasse nötig.
 *
 * <p><b>Keine Komposition:</b> Im Gegensatz zur bisherigen
 * {@code createValueFunctionForContext()} wird hier KEINE elementFunction
 * oder keyFunction in die gefundene ValueFunction injiziert. Diese
 * Komposition übernehmen die Collection/MapInstanceProvider über die
 * InstanceProviderChain-Rekursion.
 */
@Slf4j
@Getter
@Setter
@Order(30)
@Processor(
        description = "Durchsucht alle registrierten IValueFunction-Beans und findet " +
                "die erste, die isResponsibleForSubject() bestätigt. " +
                "Fallback-Resolver — wird nur erreicht, wenn Annotation und Descriptor keine VF liefern.",
        categories = {"incubator", "resolver"},
        tags = {"valueFunction", "dynamic", "fallback", "isResponsibleForSubject"}
)
public class DynamicValueFunctionResolver extends AbstractProcessor implements IValueFunctionResolver {

    @ProcessorParameter(description = "BeanProvider für den Zugriff auf alle ValueFunction-Beans")
    private IBeanProvider beanProvider;

    @Override
    public boolean isResponsibleFor(IInstanceCreationContext context) {
        return true; // Fallback — immer zuständig
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public IValueFunction resolve(IInstanceCreationContext context) {
        List<IValueFunction> allFunctions = beanProvider.getBeansOfType(IValueFunction.class);
        if (allFunctions == null || allFunctions.isEmpty()) {
            log.warn("Keine IValueFunction-Beans registriert");
            return null;
        }

        // IInstanceCreationContext extends ITransformationContext — direkte Übergabe möglich
        IValueFunction result = allFunctions.stream()
                .filter(vf -> {
                    try {
                        return vf.isResponsibleForSubject(context);
                    } catch (Exception e) {
                        log.trace("isResponsibleForSubject fehlgeschlagen für {}: {}",
                                vf.getClass().getSimpleName(), e.getMessage());
                        return false;
                    }
                })
                .findFirst()
                .orElse(null);

        if (result != null) {
            log.debug("ValueFunction '{}' dynamisch aufgelöst für Typ {}",
                    result.getClass().getSimpleName(), context.getTypeToResolve());
        } else {
            log.debug("Keine ValueFunction dynamisch gefunden für Typ {}",
                    context.getTypeToResolve());
        }

        return result;
    }
}
