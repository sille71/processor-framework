package de.starima.pfw.base.processor.description.api;

import de.starima.pfw.base.processor.context.api.ITransformationContext;

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

    String[] getRequiredCategories();
    void setRequiredCategories(String[] requiredCategories);
    String[] getRequiredSubCategories();
    void setRequiredSubCategories(String[] requiredSubCategories);
    String[] getRequiredTags();
    void setRequiredTags(String[] requiredTags);

    /**
     * Liefert die Typsignatur, indem es die assoziierte ValueFunction fragt.
     */
    default String getTypeSignature() {
        return getValueFunction() != null ? getValueFunction().getTypeSignature() : "unknown";
    }

    /**
     * PrÃ¼ft, ob der Wert generisch ist, indem es die assoziierte ValueFunction fragt.
     */
    default boolean isGeneric() {
        return getValueFunction() != null && getValueFunction().isGeneric();
    }

    /**
     * Liefert den generischen Typ-Identifier von der assoziierten ValueFunction.
     */
    default String getTypeIdentifier() {
        return getValueFunction() != null ? getValueFunction().getTypeIdentifier() : null;
    }

    /**
     * PrÃ¼ft, ob der Wert ein IReconProcessor ist, indem es die assoziierte ValueFunction fragt.
     */
    default boolean isProcessor() {
        return getValueFunction() != null && getValueFunction().isProcessor();
    }

    /**
     * PrÃ¼ft, ob der Wert ein ValueObject ist, indem es die assoziierte ValueFunction fragt.
     */
    default boolean isValueObject() {
        return getValueFunction() != null && getValueFunction().isValueObject();
    }

    /**
     * Gibt an, ob der Wert ein numerischer Typ ist (z.B. integer, long, double).
     * Die Default-Implementierung nutzt die typeSignature.
     * @return true, wenn der Wert ein numerischer Typ ist.
     */
    default boolean isNumeric() {
        return getValueFunction() != null && getValueFunction().isNumeric();
    }

    /**
     * Gibt an, ob der Wert ein Boolean ist.
     * @return true, wenn der Wert ein boolean oder Boolean ist.
     */
    default boolean isBoolean() {
        return getValueFunction() != null && getValueFunction().isBoolean();
    }

    /**
     * Gibt an, ob der Wert ein Datumstyp ist.
     */
    default boolean isDate() {
        return getValueFunction() != null && getValueFunction().isDate();
    }

    /**
     * Gibt an, ob der Wert ein Zeittyp ist.
     */
    default boolean isTime() {
        return getValueFunction() != null && getValueFunction().isTime();
    }

    /**
     * Gibt an, ob der Wert ein String ist.
     */
    default boolean isString() {
        return getValueFunction() != null && getValueFunction().isString();
    }

    /**
     * Gibt an, ob der Wert ein Enum ist.
     * (Annahme: Wir definieren eine 'enum' typeSignature)
     */
    default boolean isEnum() {
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