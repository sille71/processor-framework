package de.starima.pfw.base.processor.description.api;

import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.description.config.api.ITypeRef;

import java.util.List;
import java.util.Map;

/**
 * Beschreibt einen konkreten Wert, der einem Parameter zugewiesen ist.
 * Trennt klar zwischen dem Rohwert (aus der Konfiguration) und dem
 * aufgelÃ¶sten Laufzeit-Objekt.
 *
 * TODO: Doku liegt unter src/main/resources/docs/Architektur-ValueDescriptor.md (die muss noch in die online Doku eingebunden werden)
 */
public interface IValueDescriptor extends IDescriptorProcessor {
    /**
     * Liefert den Rohwert, so wie er aus der Konfiguration kommt (z.B. String, Map).
     * Dieser Wert wird mit der UI ausgetauscht.
     */
    Object getRawValue();
    void setRawValue(Object rawValue);

    /**
     * Liefert das aufgelÃ¶ste Java-Objekt, nachdem es von der ValueFunction transformiert wurde.
     * Dieser Wert wird zur Initialisierung des Ziel-Beans verwendet.
     */
    Object getResolvedValue();
    void setResolvedValue(Object resolvedValue);

    IValueFunction<ITransformationContext, Object, Object> getValueFunction();
    void setValueFunction(IValueFunction<ITransformationContext, Object, Object> function);

    /**
     * Der TypeRef als primäre Typ-Quelle.
     *
     * <p>Wenn gesetzt, wird der TypeRef für alle Typ-Klassifikatoren bevorzugt.
     * Falls nicht gesetzt, werden die Klassifikatoren von der ValueFunction delegiert.
     */
    ITypeRef getTypeRef();
    void setTypeRef(ITypeRef typeRef);

    String[] getRequiredCategories();
    void setRequiredCategories(String[] requiredCategories);
    String[] getRequiredSubCategories();
    void setRequiredSubCategories(String[] requiredSubCategories);
    String[] getRequiredTags();
    void setRequiredTags(String[] requiredTags);

    /**
     * Liefert die Typsignatur — TypeRef hat Vorrang, dann ValueFunction.
     */
    default String getTypeSignature() {
        if (getTypeRef() != null) return getTypeRef().getTypeSignature();
        return getValueFunction() != null ? getValueFunction().getTypeSignature() : "unknown";
    }

    /**
     * Prüft, ob der Wert generisch ist — TypeRef hat Vorrang, dann ValueFunction.
     */
    default boolean isGeneric() {
        if (getTypeRef() != null) return getTypeRef().isPolymorphic();
        return getValueFunction() != null && getValueFunction().isGeneric();
    }

    /**
     * Liefert den generischen Typ-Identifier von der assoziierten ValueFunction.
     */
    default String getTypeIdentifier() {
        return getValueFunction() != null ? getValueFunction().getTypeIdentifier() : null;
    }

    /**
     * Prüft, ob der Wert ein IProcessor ist — TypeRef hat Vorrang, dann ValueFunction.
     */
    default boolean isProcessor() {
        if (getTypeRef() != null) return getTypeRef().isProcessor();
        return getValueFunction() != null && getValueFunction().isProcessor();
    }

    /**
     * Prüft, ob der Wert ein ValueObject ist — TypeRef hat Vorrang, dann ValueFunction.
     */
    default boolean isValueObject() {
        if (getTypeRef() != null) return getTypeRef().isValueObject();
        return getValueFunction() != null && getValueFunction().isValueObject();
    }

    /**
     * Gibt an, ob der Wert ein numerischer Typ ist — TypeRef hat Vorrang, dann ValueFunction.
     */
    default boolean isNumeric() {
        if (getTypeRef() != null) return getTypeRef().isNumeric();
        return getValueFunction() != null && getValueFunction().isNumeric();
    }

    /**
     * Gibt an, ob der Wert ein Boolean ist — TypeRef hat Vorrang, dann ValueFunction.
     */
    default boolean isBoolean() {
        if (getTypeRef() != null) return getTypeRef().isBoolean();
        return getValueFunction() != null && getValueFunction().isBoolean();
    }

    /**
     * Gibt an, ob der Wert ein Datumstyp ist — TypeRef hat Vorrang, dann ValueFunction.
     */
    default boolean isDate() {
        if (getTypeRef() != null) return getTypeRef().isDate();
        return getValueFunction() != null && getValueFunction().isDate();
    }

    /**
     * Gibt an, ob der Wert ein Zeittyp ist — TypeRef hat Vorrang, dann ValueFunction.
     */
    default boolean isTime() {
        if (getTypeRef() != null) return getTypeRef().isTime();
        return getValueFunction() != null && getValueFunction().isTime();
    }

    /**
     * Gibt an, ob der Wert ein String ist — TypeRef hat Vorrang, dann ValueFunction.
     */
    default boolean isString() {
        if (getTypeRef() != null) return getTypeRef().isString();
        return getValueFunction() != null && getValueFunction().isString();
    }

    /**
     * Gibt an, ob der Wert ein Enum ist — TypeRef hat Vorrang, dann ValueFunction.
     */
    default boolean isEnum() {
        if (getTypeRef() != null) return getTypeRef().isEnum();
        return getValueFunction() != null && getValueFunction().isEnum();
    }

    /**
     * Wenn isPolymorphic den Wert true hat, ist der Blueprint ein Platzhalter. Seine getPossibleValueDescriptors()-Methode wird dafÃ¼r verantwortlich sein, die konkreten Optionen zu finden und bereitzustellen.
     * Wenn isPolymorphic den Wert false hat, reprÃ¤sentiert der Blueprint einen konkreten, finalen Typ
     * @return
     */
    default boolean isPolymorphic() {
        return false;
    }

    default List<String> getPossibleRawValues() {
        if (getValueFunction() != null) {
            return getValueFunction().getPossibleInputValues();
        }
        return null;
    }

    default List<String> getPossibleResolvedValues() {
        if (getValueFunction() != null) {
            return getValueFunction().getPossibleOutputValues();
        }
        return null;
    }

    //Wenn es einen Prototypen gibt, beschreibt dieser Descriptor eine konkrete Instanz
    public IValueDescriptor getPrototypeValueDescriptor();
    public void setPrototypeValueDescriptor(IValueDescriptor prototypeValueDescriptor);



    public List<IValueDescriptor> getPossibleValueDescriptors();


    public default Map<String, Map<String, Object>> extractEffectiveParameterMap(Object bean) {
        if (getValueFunction() != null) {
            return getValueFunction().extractEffectiveParameterMap(bean);
        }
        return null;
    }

    /**
     * @deprecated Wird durch die neue, zustandsbasierte Methode ersetzt. Dient nur noch als Einstiegspunkt.
     */
    public default Map<String, Map<String, Object>> extractEffectiveParameterMap(ITransformationContext transformationContext, Object bean) {
        if (getValueFunction() != null) {
            return getValueFunction().extractEffectiveParameterMap(transformationContext, bean);
        }
        return null;
    }

    //TODO: brauchen wir die noch? Falls ja, kann sie per default auf extractEffectiveParameterMap zurÃ¼ckgefÃ¼hrt werden!
    public default Map<String, Map<String, Object>> extractEffectiveParameterMapForResolvedValue() {
        return extractEffectiveParameterMap(getResolvedValue());
    }

    public default Object createRawValueFromResolvedValue() {
        if (getRawValue() != null) return getRawValue();
        if (getValueFunction() != null && getResolvedValue() != null) {
            //beim Aufruf dieser Methode benÃ¶tigen wir in der Regel keinen TransformationContext, da wir von einer voll etablierten Descriptor Hierarchie ausgehen.
            setRawValue(getValueFunction().reverseTransformValue(getResolvedValue()));
            return getRawValue();
        }
        return null;
    }

    public default Object createResolvedValueFromRawValue() {
        if (getResolvedValue() != null) return getResolvedValue();
        if (getValueFunction() != null && getRawValue() != null) {
            //beim Aufruf dieser Methode benÃ¶tigen wir in der Regel keinen TransformationContext, da wir von einer voll etablierten Descriptor Hierarchie ausgehen.
            setResolvedValue(getValueFunction().transformValue(getRawValue()));
            return getResolvedValue();
        }
        return null;
    }
}