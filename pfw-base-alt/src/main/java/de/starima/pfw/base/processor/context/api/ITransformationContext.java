package de.starima.pfw.base.processor.context.api;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.api.IValueDescriptor;
import de.starima.pfw.base.processor.description.api.IValueFunction;

import java.lang.reflect.Field;
import java.util.Optional;

public interface ITransformationContext extends ITaskContext {
    /**
     * Gibt das Zielfeld im Bean zurÃ¼ck, in das der Wert injiziert werden soll.
     * Daraus kÃ¶nnen Zieltyp, generische Typen und Annotationen ausgelesen werden.
     */
    Field getTargetField();

    Class<?> getTargetType();

    /**
     * Gibt die bereits aufgelÃ¶ste Instanz des Ã¼bergeordneten Ziel Objekts zurÃ¼ck, dessen Feld bestÃ¼ckt werden soll.
     */
    Object getTargetObject();

    ProcessorParameter getProcessorParameterAnnotation();

    /**
     * Gibt den Ã¼bergeordneten Deskriptor zurÃ¼ck, falls dieser Kontext innerhalb
     * einer vollstÃ¤ndigen Deskriptor-Hierarchie operiert.
     * Optional, da er im "On-Demand"-Fall nicht existiert.
     */
    Optional<IDescriptorProcessor> getParentDescriptor();

    Optional<IValueDescriptor> getValueDescriptor();

    Optional<IValueFunction<ITransformationContext, Object, Object>> getValueFunction();

    /**
     * Gibt die Ladestrategie fÃ¼r die aktuelle Transformation an (z.B. DEEP, SHALLOW).
     * Dies steuert die Rekursionstiefe bei der Extraktion von Parametern.
     */
    LoadStrategy getLoadStrategy();

    //Type getTargetType();
}