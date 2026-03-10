package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.set.api.ISetProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
@Processor(description = "Stellt einen Wert aus einer vordefinierten Liste von Optionen (Enum) bereit.",
        tags = {"Enum", "Scalar"})
public class EnumValueFunction extends AbstractValueFunction<Object,Enum<?>> {
    public static boolean isResponsibleFor(ITransformationContext transformationContext) {
        if (transformationContext == null) return false;
        Class<?> clazz = transformationContext.getRawType();
        Field field = transformationContext.getFieldToResolve();
        Class<?> valueType = field != null ? field.getType() : clazz;
        return valueType != null && valueType.isEnum();
    }

    //TODO: hier defaultwerte angeben! Die set Processoren benÃ¶tigen wir im ReconLight
    @ProcessorParameter(description = "Definitionsbereich der Parameter. Dies kÃ¶nnen Double oder auch Strings sein. ")
    private ISetProcessor<Object> domainProcessor;
    @ProcessorParameter
    private ISetProcessor<Enum<?>> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<Enum<?>> imageProcessor;

    // Ein Cache, um wiederholte Class.forName-Aufrufe zu vermeiden.
    private static final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();

    @Override
    public ISetProcessor<Object> getDomainProcessor() {
        return domainProcessor;
    }

    @Override
    public ISetProcessor<Enum<?>> getCoDomainProcessor() {
        return coDomainProcessor;
    }

    @Override
    public ISetProcessor<Enum<?>> getImageProcessor() {
        return imageProcessor;
    }

    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return isResponsibleFor(transformationContext);
    }

    @Override
    public boolean isResponsibleForInput(Object input) {
        return isInputString(input) || isInputEnum(input);
    }

    private boolean isInputString(Object input) {
        return input instanceof String;
    }

    private boolean isInputEnum(Object input) {
        return input != null && input.getClass().isEnum();
    }

    @Override
    public Enum<?> transformValue(Object input) {
        if (!isResponsibleForInput(input) || input == null || "".equals(input)) {
            log.warn("{}.transformValue: Can not transform value {}! Value is not in domain.", this.getIdentifier(), input);
            return null;
        }
        if (isInputEnum(input)) return (Enum<?>) input;

        Class<?> enumType = getEnumClass();
        if (enumType == null || !enumType.isEnum()) {
            log.error("'{}': es konnte kein Enum-Typ bestimmt werden.", getFullBeanId());
            return null;
        }

        try {
            // Dies ist der typsichere Weg, um einen Enum-Wert aus einem String zu erhalten.
            @SuppressWarnings({"unchecked", "rawtypes"})
            Enum<?> result = Enum.valueOf((Class<Enum>) enumType, input.toString());
            return result;
        } catch (IllegalArgumentException e) {
            log.warn("Keine Enum-Konstante mit dem Namen '{}' im Typ '{}' gefunden.", input, enumType.getSimpleName());
            return null;
        }
    }

    @Override
    public Enum<?> transformValue(ITransformationContext transformationContext, Object input) {
        if (!isResponsibleForSubject(transformationContext)) {
            log.warn("{}.transformValue: Can not transform value {}! Processor is not responsible for subject.", this.getIdentifier(), input);
            return null;
        }

        if (!isResponsibleForInput(input) || input == null || "".equals(input)) {
            log.warn("{}.transformValue: Can not transform value {}! Value is not in domain.", this.getIdentifier(), input);
            return null;
        }

        Class<?> enumType = getEnumClass();

        if (transformationContext != null && transformationContext.getFieldToResolve() != null) {
            enumType = transformationContext.getFieldToResolve().getType();
        }

        if (enumType == null) {
            log.error("'{}': es konnte kein Enum-Typ bestimmt werden.", getFullBeanId());
            return null;
        }

        if (!enumType.isEnum()) {
            log.error("Feld '{}' ist kein Enum-Typ, kann aber von EnumValueFunction verarbeitet werden.", transformationContext.getFieldToResolve() != null ? transformationContext.getFieldToResolve().getName() : "");
            return null;
        }

        if (isInputEnum(input)) return (Enum<?>) input;

        try {
            // Dies ist der typsichere Weg, um einen Enum-Wert aus einem String zu erhalten.
            @SuppressWarnings({"unchecked", "rawtypes"})
            Enum<?> result = Enum.valueOf((Class<Enum>) enumType, input.toString());
            return result;
        } catch (IllegalArgumentException e) {
            log.warn("Keine Enum-Konstante mit dem Namen '{}' im Typ '{}' gefunden.", input, enumType.getSimpleName());
            return null;
        }
    }

    public Object transformObjectToParameter(Double value) {
        if (value == null) return "";
        return value.toString();
    }

    // --- Implementierung der Typ-API ---

    @Override
    public String getTypeSignature() {
        return "enum";
    }

    @Override
    public boolean isGeneric() {
        // Ein Enum ist nicht generisch im Sinne von "Implementierungen suchen".
        return false;
    }

    // --- Hilfsmethoden ---

    /**
     * Holt die Enum-Klasse Ã¼ber den ClassLoader. Nutzt einen Cache fÃ¼r die Performance.
     */
    private Class<?> getEnumClass() {
        if (getTypeIdentifier() == null) {
            return null;
        }
        // PrÃ¼fe den Cache zuerst
        return classCache.computeIfAbsent(getTypeIdentifier(), typeName -> {
            try {
                // Nutze den ContextClassLoader - das ist die robusteste Methode.
                ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                if (contextClassLoader == null) {
                    // Fallback
                    return Class.forName(typeName);
                }
                return Class.forName(typeName, true, contextClassLoader);
            } catch (ClassNotFoundException e) {
                log.error("Enum-Klasse '{}' konnte nicht gefunden werden.", typeName, e);
                return null; // Gib null zurÃ¼ck, um den Fehler im Cache zu signalisieren (oder wirf eine RuntimeException)
            }
        });
    }

    @Override
    public List<String> getPossibleInputValues() {
        Class<?> enumType = getEnumClass();
        if (enumType != null && enumType.isEnum()) {
            return Arrays.stream(enumType.getEnumConstants())
                    .map(e -> ((Enum<?>) e).name())
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}