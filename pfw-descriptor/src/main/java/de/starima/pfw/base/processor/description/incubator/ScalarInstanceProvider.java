package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.context.domain.DefaultTransformationContext;
import de.starima.pfw.base.processor.description.api.IValueFunction;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceCreationContext;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceProvider;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.util.Collection;
import java.util.Map;

/**
 * Erzeugt und extrahiert skalare Werte.
 *
 * <p>Verantwortlich für alle Typen, die KEINE Struktur-Beans (Processor, ValueObject)
 * und KEINE Collections/Maps sind: String, Integer, Boolean, Double, Enum, Class, etc.
 *
 * <p><b>Delegiert intern an die bestehenden {@link IValueFunction}-Implementierungen.</b>
 * Die ValueFunctions (StringValueFunction, IntegerValueFunction, BooleanValueFunction,
 * EnumValueFunction, etc.) werden wiederverwendet — nicht neu geschrieben.
 *
 * <p>Skalare sind die <b>Blätter des Erzeugungsbaums</b>. Sie haben keine eigenen
 * Parameter und lösen keine Rekursion aus.
 *
 * <p><b>provide():</b> Delegiert an {@code IValueFunction.transformValue(context, input)}
 * <p><b>extract():</b> Delegiert an {@code IValueFunction.reverseTransformValue(context, value)}
 *
 * @see IValueFunction
 */
@Slf4j
@Getter
@Setter
@Order(30) // Nach Processor (10), ValueObject (20), Collection (25)
@Processor(
        description = """
                Erzeugt und extrahiert skalare Werte (String, Integer, Boolean, Enum, Class, etc.).
                Delegiert intern an die bestehenden IValueFunction-Implementierungen.
                Bildet die Blätter des rekursiven Erzeugungsbaums — keine weitere Rekursion.""",
        categories = {"incubator", "instanceProvider"},
        tags = {"scalar", "valueFunction", "leaf", "provide", "extract"}
)
public class ScalarInstanceProvider extends AbstractProcessor implements IInstanceProvider {

    @Override
    public boolean isResponsibleFor(IInstanceCreationContext context) {
        Class<?> rawType = resolveRawType(context);
        if (rawType == null) return false;

        // Nicht zuständig für Prozessoren, ValueObjects, Collections, Maps
        if (IProcessor.class.isAssignableFrom(rawType)) return false;
        if (ProcessorUtils.isConsideredValueObject(rawType)) return false;
        if (Collection.class.isAssignableFrom(rawType)) return false;
        if (Map.class.isAssignableFrom(rawType)) return false;
        if (rawType.isArray()) return false;

        return true;
    }

    @Override
    public Object provide(IInstanceCreationContext context) {
        Object parameterValue = context.getParameterValue();
        if (parameterValue == null) return null;

        // TransformationContext aufbauen (Brücke zur bestehenden ValueFunction-Welt)
        ITransformationContext txCtx = buildTransformationContext(context);

        // ValueFunction finden
        IValueFunction<ITransformationContext, ?, ?> valueFunction =
                ProcessorUtils.createValueFunctionForContext(txCtx);

        if (valueFunction == null) {
            log.warn("Keine ValueFunction gefunden für Typ {} (Feld: {})",
                    context.getTypeToResolve(),
                    context.getFieldToResolve() != null ? context.getFieldToResolve().getName() : "?");
            return null;
        }

        // Delegiere an transformValue
        @SuppressWarnings({"rawtypes", "unchecked"})
        IValueFunction rawVf = valueFunction;
        return rawVf.transformValue(txCtx, parameterValue);
    }

    @Override
    public Object extract(IInstanceCreationContext context) {
        Object value = context.getObjectToResolve();
        if (value == null) return null;

        // TransformationContext aufbauen
        ITransformationContext txCtx = buildTransformationContext(context);
        txCtx.setTargetObject(value);

        // ValueFunction finden
        IValueFunction<ITransformationContext, ?, ?> valueFunction =
                ProcessorUtils.createValueFunctionForContext(txCtx);

        if (valueFunction == null) {
            // Fallback: toString()
            log.trace("Keine ValueFunction für extract(), nutze toString() für {}",
                    value.getClass().getSimpleName());
            return value.toString();
        }

        // Delegiere an reverseTransformValue
        @SuppressWarnings({"rawtypes", "unchecked"})
        IValueFunction rawVf = valueFunction;
        return rawVf.reverseTransformValue(txCtx, value);
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    /**
     * Baut einen {@link ITransformationContext} als Brücke zur bestehenden ValueFunction-Welt.
     */
    private DefaultTransformationContext buildTransformationContext(IInstanceCreationContext context) {
        DefaultTransformationContext txCtx = new DefaultTransformationContext();
        txCtx.setTargetField(context.getFieldToResolve());
        txCtx.setRuntimeContext(context.getRuntimeContext());
        txCtx.setProcessorParameterAnnotation(context.getProcessorParameter());

        if (context.getTypeToResolve() instanceof Class<?> clazz) {
            txCtx.setTargetType(clazz);
        }
        if (context.getObjectToResolve() != null) {
            txCtx.setTargetObject(context.getObjectToResolve());
        }
        return txCtx;
    }

    private Class<?> resolveRawType(IInstanceCreationContext context) {
        if (context.getTypeToResolve() instanceof Class<?> clazz) {
            return clazz;
        }
        if (context.getFieldToResolve() != null) {
            return context.getFieldToResolve().getType();
        }
        if (context.getObjectToResolve() != null) {
            return context.getObjectToResolve().getClass();
        }
        return null;
    }
}
