package de.starima.pfw.base.processor.description.incubator.ai;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.description.incubator.ai.domain.ParameterSummary;
import de.starima.pfw.base.processor.description.incubator.ai.domain.ProcessorCatalogEntry;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility-Klasse zum Aufbau des {@link DefaultProcessorCatalog} aus
 * {@link Processor @Processor}-annotierten Klassen.
 *
 * <h3>Verwendung</h3>
 * <pre>{@code
 * // Manuell:
 * List<ProcessorCatalogEntry> entries =
 *     ProcessorCatalogBuilder.fromClasses(List.of(MyProcessor.class, OtherProcessor.class));
 *
 * // In der Spring Boot Starter Auto-Configuration:
 * List<Class<?>> processorClasses = applicationContext
 *     .getBeansWithAnnotation(Processor.class).values().stream()
 *     .map(Object::getClass).toList();
 * DefaultProcessorCatalog catalog = new DefaultProcessorCatalog();
 * catalog.rebuildFrom(ProcessorCatalogBuilder.fromClasses(processorClasses));
 * }</pre>
 *
 * <h3>Metadaten-Extraktion</h3>
 * Aus der {@link Processor}-Annotation werden extrahiert:
 * <ul>
 *   <li>{@code description} → shortDescription</li>
 *   <li>{@code categories} → categories</li>
 *   <li>{@code tags} → tags</li>
 * </ul>
 * Der {@code prototypeId} wird aus dem Klassennamen abgeleitet (erstes Zeichen kleingeschrieben).
 */
@Slf4j
public final class ProcessorCatalogBuilder {

    private ProcessorCatalogBuilder() {
        // Utility-Klasse
    }

    // =========================================================================
    // fromClasses — Haupteinstiegspunkt
    // =========================================================================

    /**
     * Erzeugt {@link ProcessorCatalogEntry}-Instanzen aus einer Liste von Klassen.
     *
     * <p>Klassen ohne {@link Processor}-Annotation werden übersprungen.
     *
     * @param classes Die zu inspizierenden Klassen
     * @return Liste von Catalog-Einträgen
     */
    public static List<ProcessorCatalogEntry> fromClasses(List<Class<?>> classes) {
        if (classes == null) return List.of();

        List<ProcessorCatalogEntry> entries = new ArrayList<>();
        for (Class<?> clazz : classes) {
            ProcessorCatalogEntry entry = fromClass(clazz);
            if (entry != null) {
                entries.add(entry);
            }
        }

        log.info("ProcessorCatalogBuilder: {} Einträge aus {} Klassen gebaut",
                entries.size(), classes.size());
        return entries;
    }

    /**
     * Erzeugt einen einzelnen {@link ProcessorCatalogEntry} aus einer Klasse.
     *
     * @param clazz Die zu inspizierende Klasse
     * @return den Catalog-Eintrag oder {@code null} wenn keine {@link Processor}-Annotation vorhanden
     */
    public static ProcessorCatalogEntry fromClass(Class<?> clazz) {
        if (clazz == null) return null;

        Processor annotation = clazz.getAnnotation(Processor.class);
        if (annotation == null) {
            // Superklassen prüfen (Annotation könnte vererbt sein)
            Class<?> parent = clazz.getSuperclass();
            if (parent != null) {
                annotation = parent.getAnnotation(Processor.class);
            }
        }
        if (annotation == null) return null;

        String prototypeId = derivePrototypeId(clazz);
        String displayName = deriveDisplayName(clazz);
        List<ParameterSummary> parameters = extractParameters(clazz);

        return ProcessorCatalogEntry.builder()
                .prototypeId(prototypeId)
                .displayName(displayName)
                .shortDescription(annotation.description())
                .categories(annotation.categories())
                .tags(annotation.tags())
                .parameters(parameters)
                .capabilities(derivedCapabilities(annotation))
                .typicalUseCases(new String[0])
                .compatibleWith(new String[0])
                .build();
    }

    // =========================================================================
    // Parameter-Extraktion
    // =========================================================================

    /**
     * Extrahiert alle {@link de.starima.pfw.base.annotation.ProcessorParameter @ProcessorParameter}
     * annotierten Felder als {@link ParameterSummary}-Liste.
     */
    public static List<ParameterSummary> extractParameters(Class<?> clazz) {
        List<ParameterSummary> parameters = new ArrayList<>();
        if (clazz == null) return parameters;

        // Felder der Klasse und aller Superklassen
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                de.starima.pfw.base.annotation.ProcessorParameter annotation =
                        field.getAnnotation(de.starima.pfw.base.annotation.ProcessorParameter.class);
                if (annotation != null) {
                    parameters.add(ParameterSummary.builder()
                            .name(field.getName())
                            .typeSignature(field.getType().getSimpleName())
                            .description(annotation.description())
                            .required(annotation.required())
                            .defaultValue(annotation.value().isBlank() ? null : annotation.value())
                            .build());
                }
            }
            current = current.getSuperclass();
        }

        return parameters;
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    /**
     * Leitet die prototypeId aus dem Klassennamen ab.
     *
     * <p>Konvention: Erster Buchstabe des einfachen Klassennamens klein.
     * Beispiel: {@code MyProcessor} → {@code myProcessor}
     */
    public static String derivePrototypeId(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        if (simpleName.isEmpty()) return simpleName;
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    /**
     * Leitet einen Display-Namen aus dem Klassennamen ab.
     *
     * <p>Beispiel: {@code CsvReaderProcessor} → {@code Csv Reader Processor}
     */
    public static String deriveDisplayName(Class<?> clazz) {
        String name = clazz.getSimpleName();
        // CamelCase aufbrechen
        return name.replaceAll("([A-Z])", " $1").trim();
    }

    /**
     * Leitet Capabilities aus der @Processor-Annotation ab.
     *
     * <p>Verwendet Tags als Capabilities (als natürlichsprachliche Wörter).
     */
    private static String[] derivedCapabilities(Processor annotation) {
        if (annotation.tags() == null || annotation.tags().length == 0) {
            return new String[0];
        }
        return Arrays.stream(annotation.tags())
                .map(tag -> "Unterstützt " + tag)
                .toArray(String[]::new);
    }
}