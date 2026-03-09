package de.starima.pfw.base.processor.description.api;

import de.starima.pfw.base.processor.context.api.IProposalContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Stellt die Beschreibung/Dokumentation sowie Infos Ã¼ber die Wertebereiche der Parameter zur VerfÃ¼gung.
 * Hier handelt es sich um eine Erweiterung der Beschreibung mittels @ProcessorParameter, da die Wertebereiche und konkrete Beschreibung
 * vom jeweiligen Einsatz abhÃ¤ngen.
 */
public interface IParameterDescriptor extends IDescriptorProcessor {
    /**
    *der Name/Key des Parameters in der BeanParameterMap (default identisch mit propertyName)
     */
    public String getParameterName();
    public void setParameterName(String parameterName);

    /**
     * der zugehÃ¶rige Name der Property im Prozessor
     */
    public String getPropertyName();
    public void setPropertyName(String propertyName);

    public String getDefaultValue();
    public void setDefaultValue(String defaultValue);

    public boolean isRequired();
    public void setRequired(boolean required);

    public boolean isIgnoreInitialization();
    public void setIgnoreInitialization(boolean ignoreInitialization);

    public boolean isIgnoreRefresh();
    public void setIgnoreRefresh(boolean ignoreRefresh);

    public boolean isIgnoreExtractParameter();
    public void setIgnoreExtractParameter(boolean ignoreExtractParameter);

    public boolean isOutput();
    public void setOutput(boolean output);

    public boolean isInput();
    public void setInput(boolean input);

    /**
     * Liefert den Deskriptor, der den *Wert* dieses Parameters beschreibt.
     * Dies kann ein Scalar-, Listen- oder Struktur-Deskriptor sein.
     * @return Der IValueDescriptor fÃ¼r den Wert.
     */
    IValueDescriptor getValueDescriptor();
    void setValueDescriptor(IValueDescriptor valueDescriptor);

    /**
     * Liefert alle mÃ¶glichen ValueDescriptoren, die fÃ¼r diesen Parameter
     * im aktuellen Kontext gÃ¼ltig sind.
     * WIRD IN PHASE 3 IMPLEMENTIERT.
     */
    List<IValueDescriptor> getPossibleValueDescriptors(IProposalContext context);

    /**
     * Gibt an, ob dieser Parameter als Teil des SchlÃ¼ssels fÃ¼r sein
     * umschlieÃŸendes ValueObject dient.
     * @return true, wenn das Feld mit @ProcessorParameter(key = true) annotiert ist.
     */
    boolean isKey();

    void setKey(boolean isKey);

    /**
     * Liefert die Typsignatur, indem es den assoziierten ValueDescriptor fragt.
     */
    default String getTypeSignature() {
        return getValueDescriptor() != null ? getValueDescriptor().getTypeSignature() : "unknown";
    }

    /**
     * PrÃ¼ft, ob der Parameter generisch ist, indem es den assoziierten ValueDescriptor fragt.
     */
    default boolean isGeneric() {
        return getValueDescriptor() != null && getValueDescriptor().isGeneric();
    }

    /**
     * Liefert den generischen Typ-Identifier vom assoziierten ValueDescriptor.
     */
    default String getTypeIdentifier() {
        return getValueDescriptor() != null ? getValueDescriptor().getTypeIdentifier() : null;
    }

    default boolean isNumeric() {
        return getValueDescriptor() != null && getValueDescriptor().isNumeric();
    }

    default boolean isBoolean() {
        return getValueDescriptor() != null && getValueDescriptor().isBoolean();
    }

    default boolean isDate() {
        return getValueDescriptor() != null && getValueDescriptor().isDate();
    }

    default boolean isTime() {
        return getValueDescriptor() != null && getValueDescriptor().isTime();
    }

    default boolean isString() {
        return getValueDescriptor() != null && getValueDescriptor().isString();
    }

    default boolean isEnum() {
        return getValueDescriptor() != null && getValueDescriptor().isEnum();
    }

    /**
     * Delegiert an den ValueDescriptor, um eine Liste mÃ¶glicher Werte zu erhalten.
     * NÃ¼tzlich fÃ¼r UI-Dropdowns.
     */
    default public List<IValueDescriptor> getPossibleValueDescriptors() {
        if (getValueDescriptor() != null) {
            return getValueDescriptor().getPossibleValueDescriptors();
        }
        return Collections.emptyList();
    }

    public void initBeanParameters(Object bean, Map<String, Object> parameters);

    public void bindEffectiveParameterValues(Object bean);

    public Map<String, Object> extractEffectiveParameters(Object bean);

    public Map<String,Map<String, Object>> extractEffectiveParameterMap(Object bean);

    public String getJsonRepresentation(Object value);

    public Map<String, Object> extractEffectiveParametersForResolvedValue();

    public Map<String, Map<String, Object>> extractEffectiveParameterMapForResolvedValue();
}