package de.starima.pfw.base.util;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.domain.api.IArtifact;
import de.starima.pfw.base.processor.api.IProcessor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public final class IdentifierUtils {

    private IdentifierUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Ermittelt den Identifier fÃ¼r ein ValueObject basierend auf der hybriden Strategie.
     *
     * @param valueObject Das zu identifizierende ValueObject.
     * @return Der eindeutige Identifier als String oder null, wenn keine ID ermittelt werden konnte.
     */
    public static String getIdentifierForValueObject(Object valueObject) {
        if (valueObject == null) {
            return null;
        }

        // falls das valueObject ein Processor ist
        if (valueObject instanceof IProcessor) {
            return ((IProcessor) valueObject).getIdentifier();
        }

        // 1. Konvention Ã¼ber IArtifact Interface (hat Vorrang)
        if (valueObject instanceof IArtifact) {
            Object id = ((IArtifact<?>) valueObject).getIdentifier();
            return id != null ? id.toString() : null;
        }

        // 2. Explizite Konfiguration Ã¼ber @ProcessorParameter(key=true)
        List<String> keyParts = new ArrayList<>();
        List<Field> fields = getAllFields(valueObject.getClass());

        for (Field field : fields) {
            if (field.isAnnotationPresent(ProcessorParameter.class)) {
                ProcessorParameter annotation = field.getAnnotation(ProcessorParameter.class);
                if (annotation.key()) {
                    try {
                        field.setAccessible(true);
                        Object fieldValue = field.get(valueObject);
                        if (fieldValue != null) {
                            keyParts.add(String.valueOf(fieldValue));
                        }
                    } catch (IllegalAccessException e) {
                        log.error("Fehler beim Zugriff auf SchlÃ¼sselfeld '{}' in ValueObject {}", field.getName(), valueObject.getClass().getName(), e);
                        return null; // Bei Fehler abbrechen
                    }
                }
            }
        }

        if (!keyParts.isEmpty()) {
            // Wir verwenden einen einfachen Bindestrich als Trennzeichen.
            // Ein MD5-Hash wÃ¤re eine Alternative fÃ¼r komplexere Anforderungen.
            return String.join("-", keyParts);
        }

        // 3. Kein Identifier gefunden
        return null;
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && !clazz.equals(Object.class)) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }
}