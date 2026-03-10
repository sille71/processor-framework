package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.api.IValueFunction;
import de.starima.pfw.base.processor.description.api.IValueFunctionResolver;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceCreationContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

/**
 * Löst die ValueFunction aus dem Descriptor-System auf.
 *
 * <p>Wenn im {@code IInstanceCreationContext} ein {@code IValueDescriptor}
 * vorhanden ist (weil der Prozessor einen ProcessorDescriptor hat, RunLevel 2+),
 * wird dessen ValueFunction direkt übernommen.
 *
 * <p>Dieser Pfad ist deterministischer als die dynamische Suche, weil die
 * ValueFunction bei der Descriptor-Erzeugung bereits korrekt zugeordnet wurde.
 */
@Slf4j
@Getter
@Setter
@Order(20)
@Processor(
        description = "Löst die ValueFunction aus dem Descriptor-System auf. " +
                "Nutzt context.getValueDescriptor().getValueFunction() wenn verfügbar. " +
                "Deterministischer als die dynamische Suche.",
        categories = {"incubator", "resolver"},
        tags = {"valueFunction", "descriptor", "deterministic"}
)
public class DescriptorValueFunctionResolver extends AbstractProcessor implements IValueFunctionResolver {

    @Override
    public boolean isResponsibleFor(IInstanceCreationContext context) {
        return context.getValueDescriptor() != null
                && context.getValueDescriptor().getValueFunction() != null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public IValueFunction resolve(IInstanceCreationContext context) {
        IValueFunction vf = context.getValueDescriptor().getValueFunction();
        log.debug("ValueFunction '{}' aus Descriptor aufgelöst",
                vf != null ? vf.getClass().getSimpleName() : "null");
        return vf;
    }
}
