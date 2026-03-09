package de.starima.pfw.base.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.annotation.ValueObject;
import de.starima.pfw.base.domain.ProcessorScope;
import de.starima.pfw.base.processor.assets.DocumentAssetProviderProcessor;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.*;
import de.starima.pfw.base.processor.context.domain.DefaultDescriptorConstructorContext;
import de.starima.pfw.base.processor.context.domain.DefaultTransformationContext;
import de.starima.pfw.base.processor.context.domain.DefaultValueFunctionConstructorContext;
import de.starima.pfw.base.processor.description.*;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.api.IValueFunction;
import de.starima.pfw.base.processor.description.api.IProcessorDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import jakarta.xml.bind.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ProcessorUtils {

    /* private constructor to not allow creation of object */
    private ProcessorUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String key_descriptorPrototypeIdentifier = "descriptorPrototypeIdentifier";
    public static String key_requestProcessorIdentifier = "requestProcessorIdentifier";
    public static String key_beanParameterMap = "beanParameterMap";
    public static String processorDescriptor = "processorDescriptor";
    public static String contextProviderProcessor = "contextProviderProcessor";
    public static String defaultProcessorDescriptorBeanId = "defaultProcessorDescriptor";
    public static String defaultParameterDescriptorBeanId = "defaultParameterDescriptor";
    public static String defaultParameterGroupDescriptorBeanId = "defaultParameterGroupDescriptor";

    public static String defaultParameterDomainProcessorBeanId = "defaultParameterDomainProcessor";
    public static String defaultParameterValueTransformerProcessorBeanId = "defaultParameterValueTransformerProcessor";

    /*
     * Ein Hilfs-Record, der ein Feld und die zugehÃ¶rige @ProcessorParameter-Annotation bÃ¼ndelt.
     *
     * @param field      Das reflektierte Feld-Objekt.
     * @param annotation Die auf dem Feld gefundene Annotation.
     */
    //public record AnnotatedField(Field field, ProcessorParameter annotation) {}

    public static String makeFirstLetterLowerCase(String input) {
        return input != null && !input.isEmpty() ? Character.toLowerCase(input.charAt(0)) + input.substring(1) : input;
    }

    public static String getPrototypeIdentifierFromClass(Class<?> clazz) {
        if (clazz == null) return null;
        String[] parts = clazz.getCanonicalName().split("\\.");
        return makeFirstLetterLowerCase(parts[parts.length - 1]);
    }

    public static String createFullBeanId(String prototypeIdentifier, String identifier, ProcessorScope scope) {
        if(prototypeIdentifier == null || prototypeIdentifier.isEmpty()) return null;
        String fullBeanId = prototypeIdentifier;
        if (identifier != null && !identifier.isEmpty() && !identifier.equals(fullBeanId)) {
            fullBeanId += ":" + identifier;
        }

        if (scope != null && !scope.name().isEmpty()) {
            fullBeanId += "@" + scope.name();
        }
        return fullBeanId;
    }

    public static boolean equalsPath(String path, String prototypeIdentifier, String identifier, ProcessorScope scope) {
        if (path == null || path.isEmpty()) return false;
        return path.equals(ProcessorUtils.createFullBeanId(prototypeIdentifier,identifier, scope));
    }

    public static String toUrlSafe(String id) {
        if (id == null) return null;
        return id.replace(":","~").replace("@","--");
    }

    public static String fromUrlSafe(String id) {
        if (id == null) return null;
        return id.replace("~",":").replace("--","@");
    }

    public static String getPrototypeId(String fullBeanId) {
        String[] arr = fullBeanId != null ? fullBeanId.trim().split(":") : null;
        return arr != null && arr.length > 0 ? arr[0].trim() : null;
    }

    public static String getIdentifier(String fullBeanId) {
        String[] arr = fullBeanId != null ? fullBeanId.trim().split(":") : null;
        if (arr != null)
            return arr.length > 1 ? grepIdentifier(arr[1]) : grepIdentifier(arr[0]);
        return null;
    }

    private static String grepIdentifier(String identifier) {
        String[] arr = identifier != null ? identifier.trim().split("@") : null;
        return arr != null && arr.length > 0 ? arr[0].trim() : null;
    }

    public static ProcessorScope getProcessorScope(String identifier) {
        String[] arr = identifier != null ? identifier.trim().split("@") : null;
        try {
            return arr != null && arr.length > 1 ? ProcessorScope.valueOf(arr[1].trim().toLowerCase()): ProcessorScope.prototype;
        } catch (IllegalArgumentException e) {
            log.error("Illegal scope {} detected!", arr[1].trim());
            return ProcessorScope.unknown;
        }
    }

    public static void registerProcessorWithType(IProcessor processor, String type, IProcessorContext ctx) {
        if (StringUtils.hasLength(type) && ctx != null) {
            log.info("register processor {} with type {}", processor.getFullBeanId(), type);
            ctx.addBeanIdType(type, processor.getFullBeanId());
        }
    }

    public static void registerProcessorInScope(IProcessor processor, IProcessorContext ctx) {
        if (processor == null || ctx == null) return;
        switch (processor.getScope()) {
            case unknown:
                log.error("Can not register processor {} in scope {}", processor.getFullBeanId(), processor.getScope());
            case context:
                log.info("register processor {} in scope {} with name {}", processor.getFullBeanId(), processor.getScope(), processor.getRuntimeContext());
                try {
                    ctx.addProcessor(processor);
                } catch (NullPointerException e) {
                    log.error("Can not register processor {} in scope {}. No final context available.", processor.getFullBeanId(), processor.getScope());
                }
            case parentcontext:
                //die Registrierung erfolgt analog zum scope context. Das Auslesen schaut dann in der Hierarchie nach oben.
                log.info("register processor {} in scope {} with name {}", processor.getFullBeanId(), processor.getScope(), processor.getRuntimeContext());
                try {
                    ctx.addProcessor(processor);
                } catch (NullPointerException e) {
                    log.error("Can not register processor {} in scope {}. No final context available.", processor.getFullBeanId(), processor.getScope());
                }
            case instance:
                log.info("register processor {} in scope {}", processor.getFullBeanId(), processor.getScope());
                ctx.getRootContext().addProcessor(processor);
        }
    }

    public static IProcessor getProcessorFromScope(String beanId, IProcessorContext ctx) {
        if (ctx == null) return null;
        log.info("get processor {} from scope {} in context {}", beanId, getProcessorScope(beanId) , ctx != null ? ctx.getName() : null);
        switch (getProcessorScope(beanId)) {
            case context:
                return ctx.getProcessor(beanId);
            case parentcontext:
                return ctx.getProcessorFromHierarchy(beanId);
            case instance:
                return ctx.getRootContext().getProcessor(beanId);
        }
        return null;
    }

    //TODO: wird nicht mehr benÃ¶tigt
    public static String createProcessorPrototypeId(Class<? extends IProcessor> processorClass, Field field, IProcessorContext ctx) {
        //if (!field.isAnnotationPresent(ProcessorParameter.class)) return null;

        List<Class<?>> processorClasses = findSpringClassesImplementingInterface(null, processorClass);
        if (processorClasses.isEmpty()) {
            log.error("no classes found for interface {} and field {}", processorClass.getName(), field.getName());
            return null;
        }

        for (Class<?> clazz : processorClasses) {
            try {
                Method method = clazz.getMethod("isResponsibleFor", Field.class);
                if ((boolean) method.invoke(null,field)) {
                    return ctx.getProcessorProvider().getBeanIdentifier(clazz);
                }
            } catch (Exception e) {
                log.warn("no method isResponsibleFor(field) defined for class {}", clazz.getName());
            }
        }

        log.warn("No processor prototype id found for interface {} and field {}",processorClass.getName(), field.getName());
        return null;
    }

    public static IDescriptorProcessor createDescriptorFromType(Class<?> clazz, IProcessorContext ctx) {
        //TODO
        return null;
    }

    public static Map<String, Object> extractParameterDescriptorParameters(Field field, Class<?> clazz, IProcessorContext ctx) {
        ProcessorParameter processorParameter = field.getAnnotation(ProcessorParameter.class);
        HashMap<String,Object> parameterDescriptorParameters = new HashMap<>();
        parameterDescriptorParameters.put("propertyName", field.getName());
        String pname = "".equals(processorParameter.name()) ? field.getName() : processorParameter.name();
        parameterDescriptorParameters.put("parameterName", pname);
        parameterDescriptorParameters.put("key", processorParameter.key());
        parameterDescriptorParameters.put("defaultValue", ProcessorUtils.getDefaultValue(clazz,field));
        parameterDescriptorParameters.put("type", "".equals(processorParameter.type()) ? null : processorParameter.type());
        parameterDescriptorParameters.put("range", processorParameter.range().length == 0 ? null : processorParameter.range());
        parameterDescriptorParameters.put("generalDescription", "".equals(processorParameter.description()) ? null : processorParameter.description());
        parameterDescriptorParameters.put("ignoreInitialization", processorParameter.ignoreInitialization());
        parameterDescriptorParameters.put("ignoreRefresh", processorParameter.ignoreRefresh());
        parameterDescriptorParameters.put("required", processorParameter.required());
        parameterDescriptorParameters.put("ignoreExtractParameter", processorParameter.ignoreExtractParameter());
        String assetProvider = "".equals(processorParameter.assetProviderIdentifier()) ? getPrototypeIdentifierFromClass(DocumentAssetProviderProcessor.class) : processorParameter.assetProviderIdentifier();
        parameterDescriptorParameters.put("generalAssetProviderProcessor", assetProvider);
        String assetName = "".equals(processorParameter.descriptionAssetName()) ? getPrototypeIdentifierFromClass(clazz) + "_" + pname : processorParameter.descriptionAssetName();
        parameterDescriptorParameters.put("generalAssetName", assetName);
        String assetType = "".equals(processorParameter.descriptionAssetMimetype()) ? "html" : processorParameter.descriptionAssetMimetype();
        parameterDescriptorParameters.put("generalAssetType", assetType);

        // Schreibe die Metadaten-Anforderungen aus der Annotation in die Parameter-Map,
        // damit der Deskriptor die Anforderungen an seine Werte kennt.
        if (processorParameter.requiredCategories() != null && processorParameter.requiredCategories().length > 0) {
            parameterDescriptorParameters.put("requiredCategories", processorParameter.requiredCategories());
        }
        if (processorParameter.requiredSubCategories() != null && processorParameter.requiredSubCategories().length > 0) {
            parameterDescriptorParameters.put("requiredSubCategories", processorParameter.requiredSubCategories());
        }
        if (processorParameter.requiredTags() != null && processorParameter.requiredTags().length > 0) {
            parameterDescriptorParameters.put("requiredTags", processorParameter.requiredTags());
        }

        return parameterDescriptorParameters;
    }

    /**
     * Erzeugt die beanParameterMap des ProcessorDescriptor aus einer json Ressource im Klassenpfad. In Angular kann das durch die
     * Property ProcessorConfig.processorDescriptorBeanParameterMap direkt an den Decorator Ã¼bergeben werden!
     * Per default wird der Dateiname aus dem generierten Descriptor Identifier des Processors genommen. Kann aber mit der Annotation  explicitProcessorDescriptorFileName Ã¼berschrieben werden.
     * Dies kann sinnvoll sein, wenn mehrere Descriptoren in einem File zusammengefasst werden
     * @param clazz
     * @param ctx
     * @return
     *
     * TODO: der identifier ist noch zu bestimmen, wenn noch benÃ¶tigt!
     */
    public static Map<String, Map<String, Object>> getExplicitProcessorDescriptorBeanParameterMap(Class<?> clazz, IProcessorContext ctx) {
        if (clazz == null) return null;
        String identifier = null; //ProcessorUtils.generateDescriptorFullBeanId(clazz, ctx);
        Processor processor = clazz.getAnnotation(Processor.class);
        if (processor != null && processor.explicitProcessorDescriptorFileName() != null) {
            identifier = processor.explicitProcessorDescriptorFileName();
        }
        try {
            Path basePath = Paths.get("descriptors");
            Path descriptorDefaultsPath = basePath.resolve(getIdentifier(identifier) + ".json");
            return loadBeanParameterMapFromPath(clazz, descriptorDefaultsPath);
        } catch (Exception e) {
            log.warn("Can not get bean parameter map for processor descriptor {}", identifier, e);
        }
        return null;
    }

    /**
     * Ermittelt die default beanParameterMap eines Prozessors.  Diese wird in einer json Datei unter
     * resources/defaults/processors abgelegt. Der Name der json Datei kann in der Prozessor Annotation unter defaultBeanParameterMapFileName() definiert werden.
     * Wird kein Name angegeben, so wird der kleingeschriebene Klassenname verwendet. Dabei werden auch Superklassen durchsucht.
     * Die beanParameterMap der Superklasse wird mit der Subklasse verschmolzen.
     *
     * @param clazz
     * @return
     */
    public static Map<String, Map<String, Object>> loadProcessorDefaults(Class<?> clazz) {
        if (clazz == null || !clazz.isAnnotationPresent(Processor.class)) return null;
        Map<String, Map<String, Object>> defaults = loadProcessorDefaults(clazz.getSuperclass());
        Processor processor = clazz.getAnnotation(Processor.class);
        String beanParameterMapName = processor.defaultBeanParameterMapFileName();
        if (beanParameterMapName == null) beanParameterMapName = getPrototypeIdentifierFromClass(clazz);
        defaults = MapUtils.mergeBeanIdParameterMap(defaults, loadDefaultBeanParameterMap(clazz, beanParameterMapName));

        return defaults;
    }

    public static Map<String, Map<String, Object>> loadDefaultBeanParameterMap(Class<?> clazz, String identifier) {
        try {
            Path basePath = Paths.get("defaults");
            Path processorDefaultsPath = basePath.resolve("processors").resolve(identifier + ".json");
            return loadBeanParameterMapFromPath(clazz, processorDefaultsPath);
        } catch (Exception e) {
            log.warn("Can not get bean parameter map for processor {}", identifier, e);
        }
        return null;
    }

    public static Map<String, Map<String, Object>> loadBeanParameterMapFromPath(Class<?> clazz, Path path) {
        try {
            ClassLoader cl = clazz.getClassLoader();
            InputStream input = cl.getResourceAsStream(path.toString());
            if (input == null) return null;
            ObjectMapper mapper = new ObjectMapper();
            Map<String,Map<String, Object>> map = mapper.readValue(input, Map.class);
            return map;
        } catch (Exception e) {
            log.warn("Can not get bean parameter map for path {}", path, e);
        }
        return null;
    }

    /**
     * PrÃ¼ft, ob ein gegebener Typ als "ValueObject" betrachtet werden soll,
     * indem die gesamte Klassenhierarchie auf die @ValueObject-Annotation Ã¼berprÃ¼ft wird.
     *
     * @param type Die zu prÃ¼fende Klasse oder das Interface.
     * @return true, wenn der Typ als ValueObject gilt, andernfalls false.
     */
    public static boolean isConsideredValueObject(Class<?> type) {
        // Die neue, saubere Implementierung.
        // Sie verlÃ¤sst sich vollstÃ¤ndig auf die zentrale Hierarchie-Suchmethode.
        return type != null &&!findAnnotationsInHierarchy(type, ValueObject.class).isEmpty();
    }

    /**
     * NEU: PrÃ¼ft, ob ein gegebener Typ als "Processor" betrachtet werden soll,
     * indem die gesamte Klassenhierarchie auf die @Processor-Annotation Ã¼berprÃ¼ft wird.
     *
     * @param type Die zu prÃ¼fende Klasse oder das Interface.
     * @return true, wenn der Typ als Processor gilt, andernfalls false.
     */
    public static boolean isConsideredProcessor(Class<?> type) {
        return type != null && !findAnnotationsInHierarchy(type, Processor.class).isEmpty() && IProcessor.class.isAssignableFrom(type);
    }

    public static boolean isConsideredStructure(Class<?> type) {
        return isConsideredValueObject(type) || isConsideredProcessor(type);
    }

    /**
     * FÃ¼llt die Ã¼bergebene targetMap mit den "effektiven" Annotationseigenschaften,
     * die aus der gesamten Klassenhierarchie des gegebenen Typs gesammelt und gemergt werden.
     * <p>
     * <b>Merge-Strategie:</b>
     * <ol>
     *     <li>Annotationen werden von der allgemeinsten (entferntestes Interface) bis zur
     *         spezifischsten (die Klasse selbst) verarbeitet.</li>
     *     <li>@ValueObject-Annotationen werden zuerst verarbeitet.</li>
     *     <li>@Processor-Annotationen werden danach verarbeitet, sodass ihre Werte immer Vorrang haben.</li>
     *     <li>Leere/Default-Werte einer spezifischeren Annotation Ã¼berschreiben keine bereits gesetzten
     *         Werte aus einer allgemeineren Annotation.</li>
     * </ol>
     *
     * @param type       Die zu analysierende Klasse.
     * @param targetMap  Die Map, die mit den Blueprint-Parametern befÃ¼llt wird (wird modifiziert).
     */
    public static void populateEffectiveAnnotationProperties(Class<?> type, Map<String, Object> targetMap) {
        // 1. Finde alle relevanten Annotationen in der Hierarchie.
        List<ValueObject> valueObjectAnnotations = findAnnotationsInHierarchy(type, ValueObject.class);
        List<Processor> processorAnnotations = findAnnotationsInHierarchy(type, Processor.class);

        // 2. Kehre die Listen um, um von der allgemeinsten zur spezifischsten zu mergen.
        Collections.reverse(valueObjectAnnotations);
        Collections.reverse(processorAnnotations);

        // 3. Merge zuerst alle @ValueObject-Annotationen.
        for (ValueObject vo : valueObjectAnnotations) {
            Map<String, Object> annotationMap = valueObjectAnnotationToMap(vo);
            MapUtils.mergeMaps(targetMap, annotationMap);
        }

        // 4. Merge danach alle @Processor-Annotationen (diese Ã¼berschreiben bei Bedarf).
        for (Processor p : processorAnnotations) {
            Map<String, Object> annotationMap = processorAnnotationToMap(p);
            MapUtils.mergeMaps(targetMap, annotationMap);
        }
    }

    /**
     * Konvertiert eine @ValueObject-Annotation in eine Map fÃ¼r den Blueprint.
     * Nur nicht-leere/nicht-default Werte werden berÃ¼cksichtigt.
     */
    private static Map<String, Object> valueObjectAnnotationToMap(ValueObject vo) {
        Map<String, Object> map = new HashMap<>();
        if (vo == null) return map;

        if (vo.description() != null && !vo.description().isEmpty()) {
            map.put("generalDescription", vo.description());
        }
        if (vo.descriptionAssetName() != null && !vo.descriptionAssetName().isEmpty()) {
            map.put("generalAssetName", vo.descriptionAssetName());
        }
        if (vo.descriptionAssetMimetype() != null && !vo.descriptionAssetMimetype().isEmpty()) {
            map.put("generalAssetMimetype", vo.descriptionAssetMimetype());
        }
        if (vo.assetProviderIdentifier() != null && !vo.assetProviderIdentifier().isEmpty()) {
            map.put("generalAssetProviderProcessor", vo.assetProviderIdentifier());
        }
        if (vo.categories() != null && vo.categories().length > 0) {
            map.put("categories", vo.categories());
        }
        if (vo.subCategories() != null && vo.subCategories().length > 0) {
            map.put("subCategories", vo.subCategories());
        }
        if (vo.tags() != null && vo.tags().length > 0) {
            map.put("tags", vo.tags());
        }

        if (vo.defaultValues() != null && vo.defaultValues().length > 0) {
            map.put("defaultValues", vo.defaultValues());
        }

        if (vo.descriptorPrototypeIdentifier() != null && !vo.descriptorPrototypeIdentifier().isEmpty()) {
            map.put(key_descriptorPrototypeIdentifier, vo.descriptorPrototypeIdentifier());
        }

        return map;
    }

    /**
     * Konvertiert eine @Processor-Annotation in eine Map fÃ¼r den Blueprint.
     * Nur nicht-leere/nicht-default Werte werden berÃ¼cksichtigt.
     */
    private static Map<String, Object> processorAnnotationToMap(Processor p) {
        Map<String, Object> map = new HashMap<>();
        if (p == null) return map;

        // Nutze die Logik von ValueObject fÃ¼r gemeinsame Felder
        if (p.description() != null && !p.description().isEmpty()) {
            map.put("generalDescription", p.description());
        }
        if (p.descriptionAssetName() != null && !p.descriptionAssetName().isEmpty()) {
            map.put("generalAssetName", p.descriptionAssetName());
        }
        if (p.descriptionAssetMimetype() != null && !p.descriptionAssetMimetype().isEmpty()) {
            map.put("generalAssetMimetype", p.descriptionAssetMimetype());
        }
        if (p.assetProviderIdentifier() != null && !p.assetProviderIdentifier().isEmpty()) {
            map.put("generalAssetProviderProcessor", p.assetProviderIdentifier());
        }
        if (p.categories() != null && p.categories().length > 0) {
            map.put("categories", p.categories());
        }
        if (p.subCategories() != null && p.subCategories().length > 0) {
            map.put("subCategories", p.subCategories());
        }
        if (p.tags() != null && p.tags().length > 0) {
            map.put("tags", p.tags());
        }

        if (p.defaultValues() != null && p.defaultValues().length > 0) {
            map.put("defaultValues", p.defaultValues());
        }

        if (p.descriptorPrototypeIdentifier() != null && !p.descriptorPrototypeIdentifier().isEmpty()) {
            map.put(key_descriptorPrototypeIdentifier, p.descriptorPrototypeIdentifier());
        }

        // Prozessor-spezifische Felder

        if (p.defaultBeanParameterMapFileName() != null && !p.defaultBeanParameterMapFileName().isEmpty()) {
            map.put("defaultBeanParameterMapFileName", p.defaultBeanParameterMapFileName());
        }

        return map;
    }

    /**
     * Hilfsmethode, um alle Annotationen eines Typs in der gesamten Hierarchie zu finden.
     * Die Reihenfolge ist von der spezifischsten Klasse nach oben/auÃŸen.
     */
    public static <A extends java.lang.annotation.Annotation> List<A> findAnnotationsInHierarchy(Class<?> type, Class<A> annotationClass) {
        // Implementierung aus der vorherigen Antwort...
        return findAnnotationsInHierarchy(type, annotationClass, new HashSet<>());
    }

    private static <A extends java.lang.annotation.Annotation> List<A> findAnnotationsInHierarchy(Class<?> type, Class<A> annotationClass, Set<Class<?>> visited) {
        if (type == null || !visited.add(type)) {
            return Collections.emptyList();
        }
        List<A> found = new ArrayList<>();
        // 1. PrÃ¼fe die Klasse selbst (am wichtigsten)
        if (type.isAnnotationPresent(annotationClass)) {
            found.add(type.getAnnotation(annotationClass));
        }
        // 2. PrÃ¼fe alle Interfaces
        for (Class<?> iface : type.getInterfaces()) {
            found.addAll(findAnnotationsInHierarchy(iface, annotationClass, visited));
        }
        // 3. PrÃ¼fe die Superklasse
        found.addAll(findAnnotationsInHierarchy(type.getSuperclass(), annotationClass, visited));
        return found;
    }

    /**
     * Sammelt alle Felder, die mit @ProcessorParameter annotiert sind, aus einer Klasse
     * und ihrer gesamten Superklassen-Hierarchie.
     * <p>
     * Die Methode durchlÃ¤uft die Hierarchie von der spezifischsten Klasse (Subklasse) nach oben.
     * Wenn ein Feld in einer Subklasse (anhand des Namens) ein Feld aus einer Superklasse "Ã¼berschattet",
     * wird nur das Feld und die Annotation der Subklasse berÃ¼cksichtigt.
     *
     * @param type Die zu inspizierende Klasse.
     * @return Eine Map, bei der der SchlÃ¼ssel der Name des Feldes ist und der Wert ein
     *         {@link }-Objekt, das sowohl das {@link Field} als auch seine
     *         {@link ProcessorParameter}-Annotation enthÃ¤lt.
     *
     * TODO: nutze den record AnnotatedField (siehe Definition oben, sobald auf Java 16 umgestellt wurde
     */
    public static Map<Field, ProcessorParameter> getAllAnnotatedParameterFields(Class<?> type) {
        Map<Field, ProcessorParameter> collectedFields = new HashMap<>();
        if (type == null) {
            return collectedFields;
        }

        // Wir starten bei der Ã¼bergebenen Klasse und arbeiten uns die Hierarchie nach oben.
        Class<?> currentClass = type;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(ProcessorParameter.class)) {
                    // Wichtig: Das Feld zugÃ¤nglich machen, da es wahrscheinlich private ist.
                    field.setAccessible(true);

                    ProcessorParameter annotation = field.getAnnotation(ProcessorParameter.class);

                    // putIfAbsent ist der SchlÃ¼ssel: Es fÃ¼gt das Feld nur hinzu, wenn der Name
                    // noch nicht in der Map ist. Da wir von der Subklasse starten, "gewinnt"
                    // immer die spezifischste Implementierung des Feldes.
                    collectedFields.putIfAbsent(field, annotation);
                }
            }
            // Gehe zur nÃ¤chsten Superklasse.
            currentClass = currentClass.getSuperclass();
        }

        return collectedFields;
    }

    /**
     * FÃ¼llt eine Ã¼bergebene Map mit der Blueprint-Hierarchie fÃ¼r einen gegebenen Kontext
     * und gibt den vollstÃ¤ndigen Referenz-Identifier des erzeugten Root-Elements zurÃ¼ck.
     * Dies ist die zentrale, rekursive Methode zur Erzeugung von Blueprints.
     *
     * Blueprint-Deskriptor (oder Prototyp-Deskriptor): Ein IDescriptorProcessor, der einen Typ beschreibt. Er hat keinen resolvedValue. Seine Aufgabe ist es,
     *                  Ã¼ber getPossibleValueDescriptors() alle mÃ¶glichen Implementierungen oder Werte anzubieten.
     * Instanz-Deskriptor: Ein IDescriptorProcessor, der einen konkreten Wert reprÃ¤sentiert. Er hat einen resolvedValue und eine Referenz auf seinen prototypeDescriptor.
     *                  SeSeine Aufgabe ist es, den gewÃ¤hlten Wert darzustellen.
     *
     * @param context   Der Kontext fÃ¼r den zu erzeugenden (Sub-)Blueprint.
     * @param blueprint Der SammelbehÃ¤lter (Map), der mit dem gesamten Blueprint befÃ¼llt wird.
     *                  Dieser Parameter wird durch die Methode modifiziert.
     * @return Der vollstÃ¤ndige Referenz-Identifier (z.B. "defaultProcessorDescriptor:UUID-123")
     *         des erzeugten Knotens, bereit zur Verwendung in einem Eltern-Deskriptor.
     */
    public static String generateDescriptorBlueprint(IDescriptorConstructorContext context, Map<String, Map<String, Object>> blueprint) {
        if (isConsideredProcessor(context.getSourceType())) {
            return generateProcessorDescriptorBlueprint(context, blueprint);
        }

        if (isConsideredValueObject(context.getSourceType())) {
            return generateValueObjectBlueprint(context, blueprint);
        }

        // TODO: sollen die anderen FÃ¤lle auch abgedeckt werden? Eigentlich sollte der Ausganspunkt nur ein Prozessor sein.
        // der Rest wird rekursiv aufgerufen. BenÃ¶tigt man einen spezifischen Blueprint, so kann man die spezielle Methode aufrufen.
        return null;
    }

    /**
     * Erzeugt die beanParameterMap des StructureDescriptor Blueprint aus der Annotation.
     * @param context   Der Kontext fÃ¼r den zu erzeugenden (Sub-)Blueprint.
     * @param blueprint Der SammelbehÃ¤lter (Map), der mit dem gesamten Blueprint befÃ¼llt wird.
     *                      Dieser Parameter wird durch die Methode modifiziert.
     * @return Der vollstÃ¤ndige Referenz-Identifier (z.B. "defaultProcessorDescriptor:UUID-123")
     *              des erzeugten Knotens, bereit zur Verwendung in einem Eltern-Deskriptor.
     *
     *
     */
    public static String generateValueObjectBlueprint(IDescriptorConstructorContext context, Map<String, Map<String, Object>> blueprint) {
        //ValueObject und Processor unterscheiden sich momentan noch nicht in den Parametern. Das wird sich Ã¤ndern, wenn der MethodDescriptor eingefÃ¼hrt wird.
        Class<?> clazz = context.getSourceType();
        IProcessorContext ctx = context.getRuntimeContext();
        ProcessorParameter processorParameter = context.getProcessorParameterAnnotation();

        if (clazz == null || !(isConsideredValueObject(clazz))) {
            log.error("{} no ValueObject Annotation present!", clazz.getName());
            return null;
        }
        Map<String, Object> descriptorParameters = new HashMap<>();

        // 1. wir befÃ¼llen die Map zunÃ¤chst mit den Werten der Annotationen
        populateEffectiveAnnotationProperties(clazz, descriptorParameters);

        // FÃ¼r den Fall, dass der Descriptor als ValueDescriptor eines Parameters dient (das ist der Fall, wenn es hier eine
        // ProcessorParameter Annotation gibt), fÃ¼gen wir noch die requiredCategorie Attribute hinzu.
        if (processorParameter != null && processorParameter.requiredCategories() != null && processorParameter.requiredCategories().length > 0) {
            descriptorParameters.put("requiredCategories", processorParameter.requiredCategories());
        }
        if (processorParameter != null && processorParameter.requiredSubCategories() != null && processorParameter.requiredSubCategories().length > 0) {
            descriptorParameters.put("requiredSubCategories", processorParameter.requiredSubCategories());
        }
        if (processorParameter != null && processorParameter.requiredTags() != null && processorParameter.requiredTags().length > 0) {
            descriptorParameters.put("requiredTags", processorParameter.requiredTags());
        }

        // 2. NEU: Die erweiterte Logik zur Polymorphie-Erkennung
        boolean isPolymorphicPlaceholder = clazz.isInterface() || java.lang.reflect.Modifier.isAbstract(clazz.getModifiers());

        // Optional: Eine noch mÃ¤chtigere PrÃ¼fung kÃ¶nnte den ProcessorProvider einbeziehen,
        // um zu sehen, ob es bekannte Subklassen gibt. FÃ¼r den Anfang ist dies aber sehr robust.
        // List<Class<?>> subtypes = ctx.getProcessorProvider().findImplementationsOf(clazz);
        // if (subtypes.size() > 1) { isPolymorphicPlaceholder = true; }

        descriptorParameters.put("polymorphic", isPolymorphicPlaceholder);

        if (isPolymorphicPlaceholder) {
            // FÃ¼r einen Platzhalter-Blueprint speichern wir den generischen Typ, falls vorhanden.
            // Wir fÃ¼gen KEINE parameterDescriptors hinzu, da diese von der konkreten Implementierung abhÃ¤ngen.
            Field sourceField = context.getSourceField();
            if (sourceField != null && sourceField.getGenericType() instanceof ParameterizedType) {
                descriptorParameters.put("requiredGenericSignature", sourceField.getGenericType().toString());
            }
        } else {
            descriptorParameters.put("sourceClass", clazz.getName());
            descriptorParameters.put("sourcePrototypeIdentifier", ctx.getProcessorProvider().getBeanIdentifier(clazz));
            //der identifier ist zunÃ¤chst identisch mit dem protoTypeIdentifier und kann im Recon Light dann angepasst werden
            descriptorParameters.put("sourceIdentifier", ctx.getProcessorProvider().getBeanIdentifier(clazz));
            // FÃ¼r einen konkreten Blueprint sammeln wir alle Parameter und achten auf Zyklen
            // 1. PrÃ¼fe, ob wir diesen Typ auf dem aktuellen Pfad schon einmal hatten.
            if (context.getVisitedTypes().contains(clazz)) {
                log.trace("Zyklische AbhÃ¤ngigkeit bei der Blueprint-Erstellung erkannt fÃ¼r Typ: {}. Erzeuge einen 'Lazy Stub', um die Rekursion zu unterbrechen.", clazz.getName());

                // 2. Zyklus erkannt! Wir erzeugen einen "Lazy Stub" anstatt weiter zu rekursieren.
                // Dieser Stub enthÃ¤lt nur die nÃ¶tigsten Infos (Typ, Polymorphie), aber keine Parameter.
                // Die UI weiÃŸ dadurch, dass hier ein Objekt dieses Typs hingehÃ¶rt, kann es aber nicht aufklappen.

                descriptorParameters.put("polymorphic", true); // Ein Stub ist quasi immer ein Platzhalter
                //TODO: isRecursionStub statt polymorphic?
                //stubParams.put("isRecursionStub", true); // Explizites Flag fÃ¼r die UI

                String stubUuid = "valueObjectDescriptor_stub_" + UUID.randomUUID().toString();
                String descriptorPrototypeIdentifier = Objects.toString(descriptorParameters.get(key_descriptorPrototypeIdentifier), getPrototypeIdentifierFromClass(DefaultStructureValueDescriptor.class));

                blueprint.put(stubUuid, descriptorParameters);
                return createFullBeanId(descriptorPrototypeIdentifier, stubUuid, null);
            }

            // Wenn kein Zyklus vorliegt, fÃ¼gen wir den aktuellen Typ zum Pfad hinzu und machen weiter.
            context.getVisitedTypes().add(clazz);
            ArrayList<String> parameterDescriptorIdentifiers = new ArrayList<String>(); //Sammler fÃ¼r die ParameterIdentifiers , um sie dann im Descriptor zu referenzieren
            for (Map.Entry<Field, ProcessorParameter> param : getAllAnnotatedParameterFields(clazz).entrySet()) {
                DefaultDescriptorConstructorContext paramContext = DefaultDescriptorConstructorContext.clone(context);
                paramContext.setSourceField(param.getKey());
                paramContext.setProcessorParameterAnnotation(param.getValue());
                parameterDescriptorIdentifiers.add(generateParameterBlueprint(paramContext, blueprint));
            }
            // die ParameterIdentifiers werden im ProcessorDescriptor referenziert
            descriptorParameters.put("parameterDescriptors", StringUtils.collectionToCommaDelimitedString(parameterDescriptorIdentifiers));
        }

        // 3. Bestimme den Prototyp-Identifier (beanId) fÃ¼r den aktuellen Kontext
        String descriptorPrototypeIdentifier = Objects.toString(descriptorParameters.get(key_descriptorPrototypeIdentifier), getPrototypeIdentifierFromClass(DefaultStructureValueDescriptor.class));

        // 4. Erzeuge eine temporÃ¤re, eindeutige ID fÃ¼r diesen Blueprint-Knoten
        // hier kÃ¶nnte noch der descriptorPrototypeIdentifier vorangestellt werden, um die Lesbarkeit zu erhÃ¶hen
        String currentUuid = "valueObjectDescriptor_" + UUID.randomUUID().toString();

        // 5. wir bauen die ValueFunction
        DefaultDescriptorConstructorContext valueFunctionDescriptorContext = DefaultDescriptorConstructorContext.clone(context);
        valueFunctionDescriptorContext.setSourceType(clazz);
        String valueFunctionDescriptorFullBeanId = generateValueFunctionDescriptorBlueprint(valueFunctionDescriptorContext, blueprint);
        // VerknÃ¼pfe den ParameterDescriptor mit seinem ValueDescriptor
        descriptorParameters.put("valueFunction", valueFunctionDescriptorFullBeanId);

        // 6. wir aktualisieren den blueprint mit dem aktuellen ValueObject
        blueprint.put(currentUuid, descriptorParameters);

        return createFullBeanId(descriptorPrototypeIdentifier, currentUuid, null);
    }

    /**
     * Erzeugt die beanParameterMap des ProcessorDescriptor Blueprint aus der Annotation.
     * @param context   Der Kontext fÃ¼r den zu erzeugenden (Sub-)Blueprint.
     * @param blueprint Der SammelbehÃ¤lter (Map), der mit dem gesamten Blueprint befÃ¼llt wird.
     *                      Dieser Parameter wird durch die Methode modifiziert.
     * @return Der vollstÃ¤ndige Referenz-Identifier (z.B. "defaultProcessorDescriptor:UUID-123")
     *              des erzeugten Knotens, bereit zur Verwendung in einem Eltern-Deskriptor.
     *
     * TODO: aktuell unterscheided sich der ProcessorDescriptor kaum vom StructureDescriptor (nur in der ParameterGroup - die kann evtl auch entfallen)
     * Trotzdem implementieren wir diese Methode fÃ¼r die Zukunft (MethodDescriptor)
     */
    public static String generateProcessorDescriptorBlueprint(IDescriptorConstructorContext context, Map<String, Map<String, Object>> blueprint) {
        Class<?> clazz = context.getSourceType();
        IProcessorContext ctx = context.getRuntimeContext();
        ProcessorParameter processorParameter = context.getProcessorParameterAnnotation();

        if (clazz == null || !(isConsideredProcessor(clazz))) {
            log.error("{} no Processor Annotation present!", clazz.getName());
            return null;
        }
        Map<String, Object> descriptorParameters = new HashMap<>();

        // 1. wir befÃ¼llen die Map zunÃ¤chst mit den Werten der Annotationen
        populateEffectiveAnnotationProperties(clazz, descriptorParameters);

        // FÃ¼r den Fall, dass der ProcessorDescriptor als ValueDescriptor eines Parameters dient (das ist der Fall, wenn es hier eine
        // ProcessorParameter Annotation gibt), fÃ¼gen wir noch die requiredCategorie Attribute hinzu.
        if (processorParameter != null && processorParameter.requiredCategories() != null && processorParameter.requiredCategories().length > 0) {
            descriptorParameters.put("requiredCategories", processorParameter.requiredCategories());
        }
        if (processorParameter != null && processorParameter.requiredSubCategories() != null && processorParameter.requiredSubCategories().length > 0) {
            descriptorParameters.put("requiredSubCategories", processorParameter.requiredSubCategories());
        }
        if (processorParameter != null && processorParameter.requiredTags() != null && processorParameter.requiredTags().length > 0) {
            descriptorParameters.put("requiredTags", processorParameter.requiredTags());
        }

        // 2. NEU: Die erweiterte Logik zur Polymorphie-Erkennung
        boolean isPolymorphicPlaceholder = clazz.isInterface() || java.lang.reflect.Modifier.isAbstract(clazz.getModifiers());

        // Optional: Eine noch mÃ¤chtigere PrÃ¼fung kÃ¶nnte den ProcessorProvider einbeziehen,
        // um zu sehen, ob es bekannte Subklassen gibt. FÃ¼r den Anfang ist dies aber sehr robust.
        // List<Class<?>> subtypes = ctx.getProcessorProvider().findImplementationsOf(clazz);
        // if (subtypes.size() > 1) { isPolymorphicPlaceholder = true; }

        descriptorParameters.put("polymorphic", isPolymorphicPlaceholder);

        if (isPolymorphicPlaceholder) {
            // FÃ¼r einen Platzhalter-Blueprint speichern wir den generischen Typ, falls vorhanden.
            // Wir fÃ¼gen KEINE parameterDescriptors hinzu, da diese von der konkreten Implementierung abhÃ¤ngen.
            Field sourceField = context.getSourceField();
            if (sourceField != null && sourceField.getGenericType() instanceof ParameterizedType) {
                descriptorParameters.put("requiredGenericSignature", sourceField.getGenericType().toString());
            }
        } else {
            descriptorParameters.put("sourceClass", clazz.getName());
            descriptorParameters.put("sourcePrototypeIdentifier", ctx.getProcessorProvider().getBeanIdentifier(clazz));
            //der identifier ist zunÃ¤chst identisch mit dem protoTypeIdentifier und kann im Recon Light dann angepasst werden
            descriptorParameters.put("sourceIdentifier", ctx.getProcessorProvider().getBeanIdentifier(clazz));
            // FÃ¼r einen konkreten Blueprint sammeln wir alle Parameter und achten auf Zyklen
            // 1. PrÃ¼fe, ob wir diesen Typ auf dem aktuellen Pfad schon einmal hatten.
            if (context.getVisitedTypes().contains(clazz)) {
                log.trace("Zyklische AbhÃ¤ngigkeit bei der Blueprint-Erstellung erkannt fÃ¼r Typ: {}. Erzeuge einen 'Lazy Stub', um die Rekursion zu unterbrechen.", clazz.getName());

                // 2. Zyklus erkannt! Wir erzeugen einen "Lazy Stub" anstatt weiter zu rekursieren.
                // Dieser Stub enthÃ¤lt nur die nÃ¶tigsten Infos (Typ, Polymorphie), aber keine Parameter.
                // Die UI weiÃŸ dadurch, dass hier ein Objekt dieses Typs hingehÃ¶rt, kann es aber nicht aufklappen.

                descriptorParameters.put("polymorphic", true); // Ein Stub ist quasi immer ein Platzhalter
                //TODO: isRecursionStub statt polymorphic?
                //stubParams.put("isRecursionStub", true); // Explizites Flag fÃ¼r die UI

                String stubUuid = "processorDescriptor_stub_" + UUID.randomUUID().toString();
                String descriptorPrototypeIdentifier = Objects.toString(descriptorParameters.get(key_descriptorPrototypeIdentifier), getPrototypeIdentifierFromClass(DefaultProcessorDescriptor.class));

                blueprint.put(stubUuid, descriptorParameters);
                return createFullBeanId(descriptorPrototypeIdentifier, stubUuid, null);
            }

            // Wenn kein Zyklus vorliegt, fÃ¼gen wir den aktuellen Typ zum Pfad hinzu und machen weiter.
            context.getVisitedTypes().add(clazz);
            // FÃ¼r einen konkreten Blueprint sammeln wir alle Parameter.
            ArrayList<String> parameterDescriptorIdentifiers = new ArrayList<String>(); //Sammler fÃ¼r die ParameterIdentifiers , um sie dann im Descriptor zu referenzieren
            for (Map.Entry<Field, ProcessorParameter> param : getAllAnnotatedParameterFields(clazz).entrySet()) {
                DefaultDescriptorConstructorContext paramContext = DefaultDescriptorConstructorContext.clone(context);
                paramContext.setSourceField(param.getKey());
                paramContext.setRuntimeContext(ctx);
                paramContext.setProcessorParameterAnnotation(param.getValue());
                parameterDescriptorIdentifiers.add(generateParameterBlueprint(paramContext, blueprint));
            }
            // die ParameterIdentifiers werden im ProcessorDescriptor referenziert
            descriptorParameters.put("parameterDescriptors", StringUtils.collectionToCommaDelimitedString(parameterDescriptorIdentifiers));
        }

        // 3. Bestimme den Prototyp-Identifier (beanId) fÃ¼r den aktuellen Kontext
        String descriptorPrototypeIdentifier = Objects.toString(descriptorParameters.get(key_descriptorPrototypeIdentifier), getPrototypeIdentifierFromClass(DefaultProcessorDescriptor.class));

        // 4. Erzeuge eine temporÃ¤re, eindeutige ID fÃ¼r diesen Blueprint-Knoten
        // hier kÃ¶nnte noch der descriptorPrototypeIdentifier vorangestellt werden, um die Lesbarkeit zu erhÃ¶hen
        String currentUuid = "processorDescriptor_" + UUID.randomUUID().toString();


        //TODO: falls wir den ParameterGroupDescriptor hier benÃ¶tigen, dann muss der parameter hier noch erzeugt werden (das kann fÃ¼r eine Gruppierung im UI sinnvoll sein)
        // paramGroupDescrUUID = generateParameterGroupDescriptor(....)
        // descriptorParameters.put("parameterGroupDescriptor", paramGroupDescrUUID);

        // 5. wir bauen die ValueFunction
        DefaultDescriptorConstructorContext valueFunctionDescriptorContext = DefaultDescriptorConstructorContext.clone(context);
        valueFunctionDescriptorContext.setSourceType(clazz);
        String valueFunctionDescriptorFullBeanId = generateValueFunctionDescriptorBlueprint(valueFunctionDescriptorContext, blueprint);
        // VerknÃ¼pfe den ParameterDescriptor mit seinem ValueDescriptor
        descriptorParameters.put("valueFunction", valueFunctionDescriptorFullBeanId);

        // 6. wir aktualisieren den blueprint mit dem aktuellen ValueObject
        blueprint.put(currentUuid, descriptorParameters);

        return createFullBeanId(descriptorPrototypeIdentifier, currentUuid, null);
    }

    public static String generateParameterBlueprint(IDescriptorConstructorContext context, Map<String, Map<String, Object>> blueprint) {
        Class<?> clazz = context.getSourceType();
        IProcessorContext ctx = context.getRuntimeContext();
        if (context.getProcessorParameterAnnotation() == null && context.getSourceField() != null) {
            log.error("can not create parameterBlueprint - no ProcessorParameter Annotation present in context {}!", context);
            return null;
        }
        ProcessorParameter processorParameter = context.getProcessorParameterAnnotation();
        Field field = context.getSourceField();

        HashMap<String,Object> parameterDescriptorParameters = new HashMap<>();
        parameterDescriptorParameters.put("propertyName", field.getName());
        String pname = "".equals(processorParameter.name()) ? field.getName() : processorParameter.name();
        parameterDescriptorParameters.put("parameterName", pname);
        parameterDescriptorParameters.put("key", processorParameter.key());
        parameterDescriptorParameters.put("defaultValue", ProcessorUtils.getDefaultValue(clazz,field));
        parameterDescriptorParameters.put("type", "".equals(processorParameter.type()) ? null : processorParameter.type());
        parameterDescriptorParameters.put("range", processorParameter.range().length == 0 ? null : processorParameter.range());
        parameterDescriptorParameters.put("generalDescription", "".equals(processorParameter.description()) ? null : processorParameter.description());
        parameterDescriptorParameters.put("ignoreInitialization", processorParameter.ignoreInitialization());
        parameterDescriptorParameters.put("ignoreRefresh", processorParameter.ignoreRefresh());
        parameterDescriptorParameters.put("required", processorParameter.required());
        parameterDescriptorParameters.put("ignoreExtractParameter", processorParameter.ignoreExtractParameter());
        String assetProvider = "".equals(processorParameter.assetProviderIdentifier()) ? getPrototypeIdentifierFromClass(DocumentAssetProviderProcessor.class) : processorParameter.assetProviderIdentifier();
        parameterDescriptorParameters.put("generalAssetProviderProcessor", assetProvider);
        String assetName = "".equals(processorParameter.descriptionAssetName()) ? getPrototypeIdentifierFromClass(clazz) + "_" + pname : processorParameter.descriptionAssetName();
        parameterDescriptorParameters.put("generalAssetName", assetName);
        String assetType = "".equals(processorParameter.descriptionAssetMimetype()) ? "html" : processorParameter.descriptionAssetMimetype();
        parameterDescriptorParameters.put("generalAssetType", assetType);

        String currentUuid = "parameterDescriptor_" + UUID.randomUUID().toString();
        String descriptorPrototypeIdentifier = "".equals(processorParameter.parameterDescriptorPrototypeIdentifier()) ? ctx.getProcessorProvider().getBeanIdentifier(DefaultParameterDescriptor.class) : processorParameter.parameterDescriptorPrototypeIdentifier();

        // wir erfassen die ValueDescriptoren
        DefaultDescriptorConstructorContext valueDescriptorContext = DefaultDescriptorConstructorContext.clone(context);
        String valueDescriptorFullBeanId = generateValueDescriptorBlueprint(valueDescriptorContext, blueprint);
        // VerknÃ¼pfe den ParameterDescriptor mit seinem ValueDescriptor
        parameterDescriptorParameters.put("valueDescriptor", valueDescriptorFullBeanId);


        blueprint.put(currentUuid, parameterDescriptorParameters);

        return createFullBeanId(descriptorPrototypeIdentifier, currentUuid, null);
    }

    public static String generateValueDescriptorBlueprint(IDescriptorConstructorContext context, Map<String, Map<String, Object>> blueprint) {
        Class<?> clazz = context.getSourceType();
        Field field = context.getSourceField();
        Class<?> valueType = field != null ? field.getType() : clazz;
        IProcessorContext ctx = context.getRuntimeContext();
        if (valueType == null) {
            log.error("can not create valueDescriptorBlueprint - no valueType found in context {}!", context);
            return null;
        }

        // hier wird es rekursiv! Man beachte, dass ein ProcessorDescriptor oder ein StructureDescriptor auch wieder ValueDescriptoren sein kÃ¶nnen. In diesem Fall reichen wir
        // die Bereitstellung des Blueprints fÃ¼r den valueType an die zugehÃ¶rigen Methoden!

        // Handelt es sich um einen Processor?
        if (ProcessorUtils.isConsideredProcessor(valueType)) {
            // Es ist ein Processor -> IProcessorDescriptor
            DefaultDescriptorConstructorContext valueContext = DefaultDescriptorConstructorContext.clone(context);
            valueContext.setSourceType(valueType);
            return generateProcessorDescriptorBlueprint(valueContext, blueprint);
        }

        // Handelt es sich um ein ValueObject?
        if (ProcessorUtils.isConsideredValueObject(valueType)) {
            // Es ist eine komplexe Struktur (ValueObject) -> IStructureValueDescriptor
            DefaultDescriptorConstructorContext valueContext = DefaultDescriptorConstructorContext.clone(context);
            valueContext.setSourceType(valueType);
            //TODO: hier weitermachen: (requiredCategories verschieben - siehe generateProcessorDescriptorBlueprint)
            return generateValueObjectBlueprint(valueContext, blueprint);
        }

        // danach behandeln wir spezielle typen (Listen, Scalare)

        // Handelt es sich um eine Liste?
        if (List.class.isAssignableFrom(valueType) || valueType.isArray()) {
            // Es ist eine Liste -> IListValueDescriptor
            DefaultDescriptorConstructorContext valueContext = DefaultDescriptorConstructorContext.clone(context);
            valueContext.setSourceType(valueType);
            return generateListValueDescriptorBlueprint(valueContext, blueprint);
        }

        // Handelt es sich um einen Scalaren?
        if (isScalarType(valueType)) {
            // Es ist ein primitiver Datentyp -> IDefaultValueDescriptor
            DefaultDescriptorConstructorContext valueContext = DefaultDescriptorConstructorContext.clone(context);
            valueContext.setSourceType(valueType);
            return generateDefaultValueDescriptorBlueprint(valueContext, blueprint);
        }

        // TODO: Maps behandeln wir spÃ¤ter

        // TODO: AnyValueDescriptor

        /*
        ReferenceValueDescriptor - dieser wird explizit im UI gesetzt und kann hier nicht bestimmt werden
         */
        return null;
    }

    public static String generateDefaultValueDescriptorBlueprint(IDescriptorConstructorContext context, Map<String, Map<String, Object>> blueprint) {
        Class<?> valueType = context.getSourceType();
        if (valueType == null || !isScalarType(valueType)) {
            log.error("can not create defaultValueDescriptorBlueprint - no valueType found or valueType is not a scalar type in context {}!", context);
            return null;
        }

        HashMap<String,Object> valueDescriptorParameters = new HashMap<>();
        String currentUuid = "defaultValueDescriptor_" + UUID.randomUUID().toString();

        String valueDescriptorPrototypeId = getPrototypeIdentifierFromClass(DefaultValueDescriptor.class);

        // wir bauen die ValueFunction
        DefaultDescriptorConstructorContext valueFunctionDescriptorContext = DefaultDescriptorConstructorContext.clone(context);
        valueFunctionDescriptorContext.setSourceType(valueType);
        String valueFunctionDescriptorFullBeanId = generateValueFunctionDescriptorBlueprint(valueFunctionDescriptorContext, blueprint);
        if (valueFunctionDescriptorFullBeanId != null)
            valueDescriptorParameters.put("valueFunction", valueFunctionDescriptorFullBeanId);

        blueprint.put(currentUuid, valueDescriptorParameters);

        return createFullBeanId(valueDescriptorPrototypeId, currentUuid, null);
    }

    public static String generateListValueDescriptorBlueprint(IDescriptorConstructorContext context, Map<String, Map<String, Object>> blueprint) {
        Field sourceField = context.getSourceField();
        Class<?> valueType = context.getSourceType();

        // Grundlegende PrÃ¼fung
        if (valueType == null || !(List.class.isAssignableFrom(valueType) || valueType.isArray())) {
            log.error("can not create listValueDescriptorBlueprint - no valid list/array type found in context {}!", context);
            return null;
        }
        // NEUE, ENTSCHEIDENDE PRÃœFUNG: Ohne Feld kÃ¶nnen wir den Element-Typ nicht bestimmen.
        if (sourceField == null) {
            log.error("can not create listValueDescriptorBlueprint - no sourceField in context. Cannot determine element type for {}.", valueType.getName());
            return null;
        }

        HashMap<String,Object> valueDescriptorParameters = new HashMap<>();
        String currentUuid = "listValueDescriptor_" + UUID.randomUUID().toString();

        String valueDescriptorPrototypeId = getPrototypeIdentifierFromClass(DefaultCollectionValueDescriptor.class);

        //Wir verwenden das GenericType des Feldes, nicht den rohen Klassentyp.
        Class<?> elementType = getElementType(sourceField.getGenericType());
        if (elementType == null && valueType.isArray()) {
            // Fallback fÃ¼r Arrays, falls getElementType fehlschlÃ¤gt (z.B. bei primitiven Arrays)
            elementType = valueType.getComponentType();
        }

        if (elementType == null) {
            log.error("Could not determine element type for list/array field '{}'. Blueprint generation for this element stops.", sourceField.getName());
            // Wir erzeugen trotzdem einen List-Deskriptor, aber ohne Element-Deskriptor,
            // damit die UI zumindest weiÃŸ, dass es eine Liste ist.
        } else {
            // FÃ¼r Listen bauen wir ein Descriptor Template, welches die Elemente der Liste beschreibt.
            DefaultDescriptorConstructorContext elementContext = DefaultDescriptorConstructorContext.clone(context);
            // Wichtig: Das Feld wird auf null gesetzt, da der Element-Typ selbst kein Feld ist.
            elementContext.setSourceField(null);
            elementContext.setSourceType(elementType);
            elementContext.setProcessorParameterAnnotation(null);

            valueDescriptorParameters.put("elementValueDescriptor", generateValueDescriptorBlueprint(elementContext, blueprint));
        }

        // wir bauen die ValueFunction
        DefaultDescriptorConstructorContext valueFunctionDescriptorContext = DefaultDescriptorConstructorContext.clone(context);
        valueFunctionDescriptorContext.setSourceType(valueType);
        String valueFunctionDescriptorFullBeanId = generateValueFunctionDescriptorBlueprint(valueFunctionDescriptorContext, blueprint);
        // VerknÃ¼pfe den ParameterDescriptor mit seinem ValueDescriptor
        if (valueFunctionDescriptorFullBeanId != null)
            valueDescriptorParameters.put("valueFunction", valueFunctionDescriptorFullBeanId);

        blueprint.put(currentUuid, valueDescriptorParameters);

        return createFullBeanId(valueDescriptorPrototypeId, currentUuid, null);
    }

    public static String generateValueFunctionDescriptorBlueprint(IDescriptorConstructorContext context, Map<String, Map<String, Object>> blueprint) {
        // 1. Ermittle die konkrete Klasse der zustÃ¤ndigen ValueFunction.
        // Die createValueFunctionForContext-Methode bleibt unser "Finder" fÃ¼r die richtige Implementierung.

        DefaultTransformationContext transformationContext = new DefaultTransformationContext();
        transformationContext.setRuntimeContext(context.getRuntimeContext());
        transformationContext.setTargetField(context.getSourceField());
        transformationContext.setTargetType(context.getSourceType());
        transformationContext.setTargetObject(context.getSourceObject());
        transformationContext.setProcessorParameterAnnotation(context.getProcessorParameterAnnotation());

        //TODO: zukÃ¼nftig kÃ¶nnte auch ein potentiell vorhandenes sourceObject zur Erzeugung der richtigen valueFunction herangezogen werden. Siehe Kommentar in createValueFunctionForContext()
        IValueFunction<ITransformationContext, ?, ?> valueFunctionInstance = createValueFunctionForContext(transformationContext);
        if (valueFunctionInstance == null) {
            log.error("can not create valueFunctionDescriptorBlueprint - no valueFunction found for context!", context);
            return null;
        }
        Class<?> valueFunctionClass = valueFunctionInstance.getClass();

        HashMap<String,Object> valueFunctionDescriptorParameters = new HashMap<>();
        String currentUuid = "valueFunctionDescriptor_" + UUID.randomUUID().toString();

        //Wenn nÃ¶tig kÃ¶nnen fÃ¼r spezialisierte FÃ¤lle weitere Parameter hinzugefÃ¼gt werden. Aktuell ist die ValueFunction die letzte schmale Einheit
        // und valueFunctionDescriptorParameters ist zunÃ¤chst leer

        String valueFunctionDescriptorPrototypeId = getPrototypeIdentifierFromClass(valueFunctionClass);

        blueprint.put(currentUuid, valueFunctionDescriptorParameters);

        return createFullBeanId(valueFunctionDescriptorPrototypeId, currentUuid, null);
    }

    /**
     * Erzeugt einen "lazy" Instanz-Deskriptor fÃ¼r ein gegebenes, konkretes Prozessor- oder ValueObject.
     * Der erzeugte Deskriptor enthÃ¤lt die aufgelÃ¶sten Werte, aber die untergeordneten ValueDescriptoren
     * werden nur als Stubs erzeugt, um die Ladezeit zu optimieren.
     *
     * @param context   Der Kontext, der das Quell-Objekt (sourceObject) enthÃ¤lt.
     * @param blueprint Die Map, die mit den Deskriptor-Definitionen befÃ¼llt wird.
     * @return Der vollstÃ¤ndige Referenz-Identifier des erzeugten Instanz-Deskriptors.
     */
    public static String generateDescriptorInstance(IDescriptorConstructorContext context, Map<String, Map<String, Object>> blueprint) {
        Object sourceObject = context.getSourceObject();
        if (sourceObject == null) {
            log.error("Cannot generate instance descriptor without a source object in the context.");
            return null;
        }

        context.setSourceType(sourceObject.getClass());

        // 1. Erzeuge den vollstÃ¤ndigen Blueprint als Basis im Hintergrund.
        // Wir verwenden eine temporÃ¤re Map, um die eigentliche 'blueprint'-Map nicht zu verschmutzen.
        Map<String, Map<String, Object>> templateBlueprint = new HashMap<>();
        String blueprintRootId = generateDescriptorBlueprint(context, templateBlueprint);
        if (blueprintRootId == null) {
            log.error("Failed to generate the underlying blueprint for instance creation.");
            return null;
        }
        // FÃ¼ge die erzeugten Blueprint-Definitionen zur Haupt-Map hinzu.
        blueprint.putAll(templateBlueprint);

        // 2. Starte die rekursive Instanziierung.
        //    // Die Methode 'instantiateFromBlueprint' wird die Strategie aus dem Kontext lesen.
        return instantiateFromBlueprint(blueprintRootId, sourceObject, context, blueprint);
    }

    /**
     * Interne Hilfsmethode, die einen Blueprint rekursiv instanziiert.
     */
    private static String instantiateFromBlueprint(String blueprintId, Object instanceValue, IDescriptorConstructorContext context, Map<String, Map<String, Object>> instanceBlueprint) {
        // Hole die Definition des Blueprints
        Map<String, Object> blueprintParams = instanceBlueprint.get(getIdentifier(blueprintId));
        if (blueprintParams == null) return null;

        // Erstelle eine Kopie fÃ¼r die Instanz
        Map<String, Object> instanceParams = new HashMap<>(blueprintParams);
        String instanceUuid = getPrototypeId(blueprintId) + "_instance_" + UUID.randomUUID().toString();

        // Setze die entscheidenden Instanz-Felder
        instanceParams.put("resolvedValue", instanceValue);
        instanceParams.put("prototypeValueDescriptor", blueprintId);

        // Rekursiver Teil: Die neue, saubere Logik basierend auf der Strategie
        if (instanceParams.containsKey("parameterDescriptors")) {
            LoadStrategy childStrategy;
            switch (context.getLoadStrategy()) {
                case DEEP:
                    // Wenn die Eltern DEEP geladen werden, werden die Kinder auch DEEP geladen.
                    childStrategy = LoadStrategy.DEEP;
                    break;

                case SHALLOW:
                    // Wenn die Eltern SHALLOW geladen werden, werden die Kinder LAZY.
                    // Das ist der SchlÃ¼ssel fÃ¼r die UI!
                    childStrategy = LoadStrategy.LAZY;
                    break;

                case LAZY:
                default:
                    // HIER ENTSTEHT DER "SMARTE STUB":
                    // Wir sind im LAZY-Modus. Wir entfernen die rekursiven Kinder,
                    // aber behalten den korrekten Typ (prototypeValueDescriptor) und die valueFunction bei.
                    instanceParams.remove("parameterDescriptors");
                    instanceParams.remove("elementValueDescriptor"); // FÃ¼r Listen
                    childStrategy = null; // Signalisiert, dass die Schleife nicht ausgefÃ¼hrt wird
                    break;
            }

            if (childStrategy != null) {
                // Die Schleife zur Instanziierung der Parameter bleibt fast gleich.
                List<String> newParameterInstanceIds = new ArrayList<>();
                //TODO: das kÃ¶nnte auch eine Liste oder ein Array von Strings sein!
                String[] parameterBlueprintIds = ((String) instanceParams.get("parameterDescriptors")).split(",");

                for (String paramBlueprintId : parameterBlueprintIds) {
                    Map<String, Object> paramBlueprintParams = instanceBlueprint.get(getIdentifier(paramBlueprintId));
                    String propertyName = (String) paramBlueprintParams.get("propertyName");
                    try {
                        Field field = getDeclaredFieldForProcessorClass(instanceValue.getClass(), propertyName);
                        field.setAccessible(true);
                        Object fieldValue = field.get(instanceValue);
                        if (fieldValue == null) {
                            continue;
                        }

                        // Erstelle einen neuen Kontext fÃ¼r das Kind-Feld
                        DefaultDescriptorConstructorContext childContext = DefaultDescriptorConstructorContext.clone(context);
                        childContext.setSourceField(field);
                        childContext.setSourceType(field.getType());
                        childContext.setSourceObject(fieldValue);
                        // HIER IST DIE MAGIE: Setze die korrekte Strategie fÃ¼r die nÃ¤chste Ebene.
                        childContext.setLoadStrategy(childStrategy);

                        // Erzeuge eine Instanz des Parameter-Deskriptors selbst
                        String paramInstanceId = instantiateFromBlueprint(paramBlueprintId, fieldValue, childContext, instanceBlueprint);

                        // Wir holen die Parameter-Map der gerade erzeugten Instanz.
                        Map<String, Object> paramInstanceParams = instanceBlueprint.get(getIdentifier(paramInstanceId));

                        // Finde den ValueDescriptor des Parameters im Blueprint
                        String valueDescriptorBlueprintId = (String) paramBlueprintParams.get("valueDescriptor");

                        // Wir rufen instantiateFromBlueprint rekursiv auf.
                        // Da der childContext die Strategie LAZY hat, wird dieser Aufruf
                        // automatisch einen "smarten Stub" des korrekten Typs erzeugen.
                        String valueDescriptorInstanceId = instantiateFromBlueprint(valueDescriptorBlueprintId, fieldValue, childContext, instanceBlueprint);


                        // Wir ersetzen die Blueprint-Referenz des ValueDescriptors in der Parameter-Instanz
                        // durch die ID des neu erzeugten "lazy stubs".
                        paramInstanceParams.put("valueDescriptor", valueDescriptorInstanceId);

                        newParameterInstanceIds.add(paramInstanceId);

                    } catch (Exception e) {
                        log.error("Error accessing field {} on instance {} for lazy instantiation", propertyName, instanceValue, e);
                    }
                }
                instanceParams.put("parameterDescriptors", StringUtils.collectionToCommaDelimitedString(newParameterInstanceIds));
            }
        }

        instanceBlueprint.put(instanceUuid, instanceParams);
        return createFullBeanId(getPrototypeId(blueprintId), instanceUuid, null);
    }

    public static String createTypeIdentifier(ITransformationContext context) {
        Class<?> clazz = context.getTargetType();
        if (clazz == null) {
            return null;
        }
        return clazz.getName();
    }

    /**
     * PrÃ¼ft, ob ein gegebener Typ als "skalar" (einfach, atomar) betrachtet wird.
     */
    public static boolean isScalarType(Class<?> type) {
        return type.isPrimitive() ||
                Number.class.isAssignableFrom(type) ||
                Boolean.class.isAssignableFrom(type) ||
                String.class.isAssignableFrom(type) ||
                Double.class.isAssignableFrom(type) ||
                type.isEnum() ||
                Class.class.isAssignableFrom(type) ||
                ArtifactIdentifier.class.isAssignableFrom(type);
    }

    /**
     * ZukÃ¼nftige Erweiterung von createValueFunctionForContext Aktuell nutzt createValueFunctionForContext das sourceObject hauptsÃ¤chlich, um den TransformationContext zu befÃ¼llen, damit die isResponsibleForSubject-Methode der ValueFunctions eine Entscheidung treffen kann.Die eigentliche Logik zur Auswahl der Funktion kÃ¶nnte aber noch viel intelligenter werden. Wenn eine Instanz vorhanden ist, kÃ¶nnen wir Entscheidungen treffen, die auf dem Inhalt des Objekts basieren, nicht nur auf seinem Typ.Ein konkretes Beispiel, was dadurch mÃ¶glich wird:Stellen Sie sich ein Feld vom Typ Object vor, das mal eine String-URL, mal eine Integer-ID enthalten kann.Java@ProcessorParameter
     * private Object identifier;Eine "intelligentere" UrlValueFunction kÃ¶nnte so aussehen:Javapublic class UrlValueFunction extends AbstractValueFunction<...> {
     *     @Override
     *     public boolean isResponsibleForSubject(ITransformationContext context) {
     *         // PrÃ¼fe nicht nur den Typ...
     *         if (!super.isResponsibleForSubject(context)) {
     *             return false;
     *         }
     *
     *         // ...sondern auch den Inhalt, wenn eine Instanz da ist!
     *         Object sourceObject = context.getTargetObject();
     *         if (sourceObject instanceof String) {
     *             String value = (String) sourceObject;
     *             // Ich bin nur verantwortlich, wenn der String wie eine URL aussieht.
     *             return value.startsWith("http://") || value.startsWith("https://");
     *         }
     *
     *         return false;
     *     }
     *     // ...
     * }Und eine IdValueFunction:Javapublic class IdValueFunction extends AbstractValueFunction<...> {
     *     @Override
     *     public boolean isResponsibleForSubject(ITransformationContext context) {
     *         // ...
     *         Object sourceObject = context.getTargetObject();
     *         if (sourceObject instanceof String) {
     *             // Ich bin verantwortlich, wenn der String nur aus Zahlen besteht.
     *             return ((String) sourceObject).matches("\\d+");
     *         }
     *         // ...
     *     }
     * }
     * @param context
     * @return
     */
    public static IValueFunction<ITransformationContext, ?, ?> createValueFunctionForContext(ITransformationContext context) {
        // Zuerst versuchen, die ValueFunction Ã¼ber die Annotationen direkt zu finden
        ProcessorParameter processorParameter = context.getProcessorParameterAnnotation();
        if (processorParameter != null) {
            String valueFunctionFullBeanId = createFullBeanId(processorParameter.valueFunctionPrototypeIdentifier(), processorParameter.valueFunctionIdentifier(), null);
            if (StringUtils.hasLength(valueFunctionFullBeanId)) {
                try {
                    //TODO: der Identifier der ValueFnction kann auch als UUID gesetzt werden
                    IValueFunction<ITransformationContext, ?, ?> fp = context.getRuntimeContext().getProcessorProvider().getBeanForId(IValueFunction.class, valueFunctionFullBeanId);
                    if (fp != null) {
                        log.debug("Found ValueFunction {} via annotation for context {}", valueFunctionFullBeanId, context);
                        // wir prÃ¼fen jetzt nochmal die Verantwortlichkeit
                        if (fp.isResponsibleForSubject(context)) {
                            fp.setTypeIdentifier(createTypeIdentifier(context));
                            return fp;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not instantiate ValueFunction {} from annotation for context {}: {}", valueFunctionFullBeanId, context, e.getMessage());
                    // Fallback zur dynamischen Ermittlung, falls die Annotation fehlerhaft ist
                }
            }
        }

        // Wenn keine explizite ValueFunction Ã¼ber Annotationen gefunden wurde oder ein Fehler auftrat,
        // versuchen wir, sie dynamisch zu ermitteln.
        // Hier nutzen wir die Logik von getValueFunction, die alle Implementierungen von IValueFunction durchsucht.
        // Wir mÃ¼ssen zunÃ¤chst einen neuen ITransformationContext erstellen.
        // Die IValueFunctionConstructorContext enthÃ¤lt bereits alle nÃ¶tigen Informationen.

        // Hole alle registrierten IValueFunction-Beans
        List<IValueFunction> fpList = context.getRuntimeContext().getProcessorProvider().getBeansOfType(IValueFunction.class);

        IValueFunction<ITransformationContext, ?, ?> function = null;

        if (fpList != null) {
            // Finde die erste ValueFunction, die fÃ¼r das gegebene Feld verantwortlich ist
            function = fpList.stream()
                    .filter(fp -> fp.isResponsibleForSubject(context))
                    .findFirst().orElse(null);
            if (function != null) {
                function.setTypeIdentifier(createTypeIdentifier(context));
            }
        }

        //Diesen Schritt benÃ¶tigen wir nur fÃ¼r die initBeanParameter Methode nicht fÃ¼r den blueprint, TODO: evtl trennen
        // 2. Spezialbehandlung fÃ¼r kompositionale Funktionen (rekursive)
        if (function instanceof DefaultListValueFunction) {
            // Erzeuge einen neuen Kind-Kontext fÃ¼r das Listenelement
            Class<?> elementType = getElementType(context.getTargetField().getGenericType());
            DefaultTransformationContext elementContext = new DefaultTransformationContext();
            elementContext.setTargetType(elementType);
            elementContext.setRuntimeContext(context.getRuntimeContext());

            // Rekursiver Aufruf, um die Kind-Funktion zu erzeugen!
            IValueFunction elementFunction = createValueFunctionForContext(elementContext);

            // Injiziere die Kind-Funktion in die Eltern-Funktion
            // (Hier ist eine Injektion sauber, da es sich um eine temporÃ¤re Hierarchie handelt)
            ((DefaultListValueFunction) function).setElementFunction(elementFunction);
        }

        //TODO: besser machen
        if (function instanceof DefaultArrayValueFunction) {
            // Erzeuge einen neuen Kind-Kontext fÃ¼r das Listenelement
            Class<?> elementType = getElementType(context.getTargetField().getGenericType());
            DefaultTransformationContext elementContext = new DefaultTransformationContext();
            elementContext.setTargetType(elementType);
            elementContext.setRuntimeContext(context.getRuntimeContext());

            // Rekursiver Aufruf, um die Kind-Funktion zu erzeugen!
            IValueFunction elementFunction = createValueFunctionForContext(elementContext);

            // Injiziere die Kind-Funktion in die Eltern-Funktion
            // (Hier ist eine Injektion sauber, da es sich um eine temporÃ¤re Hierarchie handelt)
            ((DefaultArrayValueFunction) function).setElementFunction(elementFunction);
        }

        //TODO: noch die Map Struktur implementieren

        return function;
    }

    public static List<Class<?>> findSpringClassesImplementingInterface(String packageName, Class<?> interfaceClass) {
        if (packageName == null) packageName = "de.starima.pfw";
        List<Class<?>> implementingClasses = new ArrayList<>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(interfaceClass));
        Set<BeanDefinition> candidates = scanner.findCandidateComponents(packageName);

        for (BeanDefinition beanDefinition : candidates) {
            try {
                implementingClasses.add(Class.forName(beanDefinition.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                log.error("No class found for name {}", beanDefinition.getBeanClassName(), e);
            }
        }

        return implementingClasses;
    }

    public static List<Class<?>> findSpringClassesWithProcessorAnnotation(String packageName) {
        if (packageName == null) packageName = "de.starima.pfw";
        List<Class<?>> implementingClasses = new ArrayList<>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(IProcessor.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Processor.class));
        Set<BeanDefinition> candidates = scanner.findCandidateComponents(packageName);

        for (BeanDefinition beanDefinition : candidates) {
            try {
                implementingClasses.add(Class.forName(beanDefinition.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                log.error("No class found for name {}", beanDefinition.getBeanClassName(), e);
            }
        }

        return implementingClasses;
    }

    public static IProcessor findParentProcessorForBeanId(IProcessor processor, String beanId) {
        if (processor == null) return null;
        if (beanId.equals(processor.getProtoTypeIdentifier())) return processor;
        return findParentProcessorForBeanId(processor.getRuntimeContext().getInitializedProcessor(), beanId);
    }

    public static IProcessor findParentProcessorForFullBeanId(IProcessorContext ctx, IProcessor processor, String fullBeanId) {
        if (processor == null) return null;
        if (fullBeanId.equals(processor.getFullBeanId())) return processor;
        if (processor.getRuntimeContext() != null && processor.getRuntimeContext().getParentContext() != null)
            return findParentProcessorForFullBeanId(ctx, processor.getRuntimeContext().getParentContext().getInitializedProcessor(), fullBeanId);
        if (ctx != null && ctx.getParentContext() != null)
            return findParentProcessorForFullBeanId(ctx, ctx.getParentContext().getInitializedProcessor(), fullBeanId);
        return null;
    }

    public static IProcessorDescriptor findProcessorDescriptorInParents(IProcessor parentProcessor) {
        if (parentProcessor == null) return null;
        if (parentProcessor instanceof IProcessorDescriptor) return (IProcessorDescriptor) parentProcessor;
        if (parentProcessor.getRuntimeContext().getParentContext() != null)
            return findProcessorDescriptorInParents(parentProcessor.getRuntimeContext().getParentContext().getInitializedProcessor());
        return null;
    }

    public static boolean isProcessorCreationPossible(IProcessorContext ctx, IProcessor parentProcessor, IProcessor childProcessor) {
        if (parentProcessor == null) return true;
        if (childProcessor instanceof IProcessorDescriptor) {
            /**
             * Jeder Processor kann durch einen ProcessorDescriptor beschrieben werden. Da die Descriptoren selbst wieder Processoren sind, werden auch sie
             * letztendlich durch einen ProcessorDescriptor beschrieben. Diese 2. Ebene sollte dann aber final sein!
             * Das bedeutet: Ein ProcessorDescriptor wird nicht mehr benÃ¶tigt, wenn sich in der zugehÃ¶rigen Parent Hierarchie
             * bereits 2 ProcessorDescriptoren befinden.
             */
            IProcessorDescriptor pd = ProcessorUtils.findProcessorDescriptorInParents(parentProcessor);
            if (pd != null && ProcessorUtils.findProcessorDescriptorInParents(pd.getRuntimeContext().getInitializedProcessor()) != null) {
                log.trace("can not create processor descriptor {} in level 3. Used parent processor = {}", childProcessor.getFullBeanId(), parentProcessor.getFullBeanId());
                return false;
            }
            return true;
        }

        //generell Rekursionen vermeiden (lediglich ProcessorDescriptoren dÃ¼rfen level 2 erreichen
        if (ProcessorUtils.findParentProcessorForFullBeanId(ctx, parentProcessor, childProcessor.getFullBeanId()) != null) {
            log.info("can not create processor {} in parent processor {}, recursion detected!",childProcessor.getFullBeanId(), parentProcessor.getFullBeanId());
            return false;
        }
        return true;
    }

    public static Field getDeclaredFieldForProcessorClass(Class<?> clazz, String propertyName) {
        try {
            Field field = clazz.getDeclaredField(propertyName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            log.trace("No field defined for property {} in class {}. Try super class {}", propertyName, clazz.getName(), clazz.getSuperclass().getName());
            return getDeclaredFieldForProcessorClass(clazz.getSuperclass(),propertyName);
        }
    }

    /**
     * Ermittelt den generischen "Wert"-Typ eines Feldes.
     * - FÃ¼r ein Feld {@code List<String>} wird {@code String.class} zurÃ¼ckgegeben.
     * - FÃ¼r ein Feld {@code Map<String, Integer>} wird {@code Integer.class} zurÃ¼ckgegeben.
     *
     * @param field Das zu analysierende Feld.
     * @return Der generische Wert-Typ oder null, wenn keiner ermittelt werden kann.
     */
    public static Class<?> getGenericType(Field field) {
        if (field == null) {
            return null;
        }
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type[] typeArguments = parameterizedType.getActualTypeArguments();

            Type typeArgument = null;
            if (Map.class.isAssignableFrom(field.getType()) && typeArguments.length == 2) {
                // For Maps, we are interested in the value type
                typeArgument = typeArguments[1];
            } else if (List.class.isAssignableFrom(field.getType()) && typeArguments.length == 1) {
                // For Lists, we are interested in the element type
                typeArgument = typeArguments[0];
            }

            if (typeArgument instanceof Class) {
                return (Class<?>) typeArgument;
            }
            // Handle nested generics like List<Map<String, MyObject>>
            if (typeArgument instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) typeArgument).getRawType();
            }
        }
        return null;
    }

    /**
     * Extrahiert einen Parameterwert aus der Konfigurations-Map und berÃ¼cksichtigt dabei
     * sowohl den aktuellen Feldnamen als auch definierte Aliase aus der @ProcessorParameter-Annotation.
     *
     * @param parameters Die Konfigurations-Map.
     * @param field Das Feld, fÃ¼r das der Wert gesucht wird.
     * @return Der gefundene Wert oder null, wenn weder der Feldname noch ein Alias gefunden wurde.
     */
    private static Object getParameterValueWithAliasSupport(Map<String, Object> parameters, Field field) {
        // 1. Versuche es mit dem primÃ¤ren, aktuellen Feldnamen. Das ist der Standardfall.
        String primaryName = field.getName();
        if (parameters.containsKey(primaryName)) {
            return parameters.get(primaryName);
        }

        // 2. Wenn nicht erfolgreich, prÃ¼fe die Aliase.
        ProcessorParameter annotation = field.getAnnotation(ProcessorParameter.class);
        if (annotation != null && annotation.aliases().length > 0) {
            for (String alias : annotation.aliases()) {
                if (parameters.containsKey(alias)) {
                    // Optional: Eine Warnung loggen, um auf die Verwendung veralteter Namen hinzuweisen.
                    log.warn("Veralteter Parametername '{}' fÃ¼r Feld '{}' in Konfiguration verwendet. Bitte auf '{}' aktualisieren.",
                            alias, field.getName(), primaryName);
                    return parameters.get(alias);
                }
            }
        }

        // 3. Nichts gefunden.
        return null;
    }

    public static Object getParameterValue(Object bean, Field field, Map<String, Object> parameters) {
        if (parameters == null) parameters = new HashMap<>();
        String parameterName = getParameterName(field);
        Object parameterValue = parameters.get(parameterName);

        if (parameterValue == null) {
            //wir prÃ¼fen als Konvention, ob es nicht einen Parameter mit dem suffix Identifier gibt. Z.B. kann
            //dann ein Parameter differenceProcessor auch als differenceProcessorIdentifier benutzt werden
            parameterName = parameterName + "Identifier";
            parameterValue = parameters.get(parameterName);
        }

        if (parameterValue == null) {
            //wir benutzen den default Value
            parameterValue = getDefaultValue(bean.getClass(), field);
        }
        return parameterValue;
    }

    public static String getParameterName(Field field) {
        if (field == null) return null;
        if (field.isAnnotationPresent(ProcessorParameter.class)) {
            ProcessorParameter p = field.getAnnotation(ProcessorParameter.class);
            return "".equals(p.name()) ? field.getName() : p.name();
        }
        return field.getName();
    }

    public static String getDefaultValue(Class<?> clazz, Field field) {
        String defaultValue = getOverrideDefaultValue(clazz, field);
        if (defaultValue == null) {
            if (field.isAnnotationPresent(ProcessorParameter.class)) {
                ProcessorParameter p = field.getAnnotation(ProcessorParameter.class);
                defaultValue = "".equals(p.value()) ? null : p.value();
            }
        }
        return defaultValue;
    }

    public static String getOverrideDefaultValue(Class<?> clazz, Field field) {
        if (field == null || clazz == null) return null;

        String defaultValue = null;
        if (clazz.isAnnotationPresent(Processor.class)) {
            Processor p = clazz.getAnnotation(Processor.class);
            defaultValue = getParameterValue(p.defaultValues(), getParameterName(field));
            if (defaultValue == null) {
                //wir fragen die Superklasse
                defaultValue = getOverrideDefaultValue(clazz.getSuperclass(), field);
            }
        }
        return defaultValue;
    }

    /**
     * Wandelt ein Array von Strings im Format "key|value" in eine Map um.
     * Diese Methode ist ideal, um das `defaultValues`-Attribut aus der
     * {@link Processor}-Annotation zu parsen.
     *
     * @param defaultValues Ein Array von Strings, z.B. {"param1|wertA", "param2|wertB"}.
     * @return Eine {@code Map<String, String>}, die die geparsten Paare enthÃ¤lt.
     *         Gibt eine leere Map zurÃ¼ck, wenn das Input-Array null oder leer ist.
     *         EintrÃ¤ge mit ungÃ¼ltigem Format werden Ã¼bersprungen und eine Warnung wird geloggt.
     */
    public static Map<String, Object> convertDefaultValuesToMap(String[] defaultValues) {
        if (defaultValues == null || defaultValues.length == 0) {
            return Collections.emptyMap();
        }

        return Stream.of(defaultValues)
                .map(pair -> pair.split("\\|", 2)) // Splitte in maximal 2 Teile
                .filter(parts -> {
                    if (parts.length == 2 && !parts[0].trim().isEmpty()) {
                        return true;
                    }
                    log.warn("UngÃ¼ltiger Eintrag im defaultValues-Array: '{}'. Erwartetes Format ist 'key|value'. Eintrag wird Ã¼bersprungen.", String.join("|", parts));
                    return false;
                })
                .collect(Collectors.toMap(
                        parts -> parts[0].trim(), // Key
                        parts -> parts[1].trim(), // Value
                        (existingValue, newValue) -> newValue // Bei doppelten Keys gewinnt der letzte
                ));
    }

    public static String getParameterValue(String[] overrides, String parameterName) {
        try {
            if (overrides == null || parameterName == null) return null;
            String result = Arrays.stream(overrides).filter(override -> parameterName.equalsIgnoreCase(override.split("\\|")[0])).findFirst().orElse(null);
            return result != null ? result.split("\\|")[1] : null;
        } catch (Exception e) {
            log.error("Can not get parameter value from overrides for parameter {}", parameterName, e);
            return null;
        }
    }

    public static void initBeanParameter(Object bean, Field field, Object parameterValue, IProcessorContext ctx) {
        try {
            field.setAccessible(true);
            DefaultTransformationContext transformationContext = new DefaultTransformationContext();
            transformationContext.setTargetObject(bean);
            transformationContext.setTargetField(field);
            transformationContext.setRuntimeContext(ctx);

            // Finde die zustÃ¤ndige Funktion
            IValueFunction<ITransformationContext, ?, ?> typedValueFunction = createValueFunctionForContext(transformationContext);
            if (typedValueFunction == null) {
                log.error("No ValueFunction found for field {}. Cannot set parameter.", field.getName());
                return;
            }

            // Wir umgehen die Compiler-PrÃ¼fung, indem wir explizit einen Raw-Type verwenden.
            // Das signalisiert: "Die Typsicherheit wird an dieser Stelle zur Laufzeit sichergestellt."
            @SuppressWarnings({"rawtypes", "unchecked"})
            IValueFunction rawValueFunction = typedValueFunction;

            // Der Aufruf von transformValue auf dem Raw-Type gibt fÃ¼r den Compiler ein 'Object' zurÃ¼ck.
            Object transformedValue = rawValueFunction.transformValue(transformationContext, parameterValue);

            field.set(bean, transformedValue);
            if (isParameterRequired(field) && field.get(bean) == null) {
                log.error("field {} is required but is null", field.getName());
            }
        } catch (Exception e) {
            log.error("can not set field for property {}", field.getName(), e);
        }
    }

    public static Class<?> getElementType(Type type) {
        // Extrahiert den generischen Typ
        Class<?> elementType = null;
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length > 0) {
                Type listElementType = typeArguments[0];
                if (listElementType instanceof Class) {
                    elementType = (Class<?>) listElementType;
                } else if (listElementType instanceof ParameterizedType) {
                    elementType = (Class<?>) ((ParameterizedType) listElementType).getRawType();
                }
            }
        }
        return elementType;
    }

    public static IValueFunctionConstructorContext createChildContextForListElement(IDescriptorConstructorContext parentContext) {
        Class<?> elementType = getElementType(parentContext.getSourceField().getGenericType());
        if (elementType == null) {
            log.warn("Could not determine element type for List field {}. Cannot create child context.", parentContext.getSourceField().getName());
            return null;
        }

        DefaultValueFunctionConstructorContext childContext = new DefaultValueFunctionConstructorContext();
        childContext.setSourceType(elementType);
        childContext.setRuntimeContext(parentContext.getRuntimeContext());

        return childContext;
    }

    /**
     * @deprecated
     * @param bean
     * @param field
     * @param ctx
     * @return
     */
    public static IValueFunction getValueFunction(Object bean, Field field, IProcessorContext ctx) {
        if (field == null || ctx == null) return null;
        //wir prÃ¼fen zunÃ¤chst, ob eine ValueFunction in den Annotationen angegeben wurde
        if (field.isAnnotationPresent(ProcessorParameter.class)) {
            ProcessorParameter p = field.getAnnotation(ProcessorParameter.class);
            String valueFunctionFullBeanId = createFullBeanId(p.valueFunctionPrototypeIdentifier(), p.valueFunctionIdentifier(), null);
            if (StringUtils.hasLength(valueFunctionFullBeanId)) {
                try {
                    IValueFunction fp = ctx.getProcessorProvider().getBeanForId(IValueFunction.class, valueFunctionFullBeanId);
                    if (fp != null) return fp;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        //TODO: cache einbauen
        //Falls es keine Annotation gab suchen wir eine passende Funktion in der Menge aller Implementierungen von IValueFunction
        List<IValueFunction> fpList = ctx.getProcessorProvider().getBeansOfType(IValueFunction.class);
        if (fpList != null) {
            IValueFunction funcP = fpList.stream().filter(fp -> fp.isResponsibleForSubject(Pair.of(bean, field))).findFirst().orElse(null);
            if (funcP == null) log.error("No ValueFunction found for bean {} and field {}", bean, field.getName());
            return funcP;
        }
        return null;
    }

    public static void initBeanParameters(Object bean, Map<String, Object> parameters, IProcessorContext ctx) {
        initBeanParameters(bean, bean.getClass(), parameters, ctx);
    }

    public static void initBeanParameters(Object bean, Class<?> clazz, Map<String, Object> parameters, IProcessorContext ctx) {
        if (!isConsideredStructure(clazz)) return;

        for (Map.Entry<Field, ProcessorParameter> entry : getAllAnnotatedParameterFields(clazz).entrySet()) {
            Field field = entry.getKey();

            if (isParameterInitializationAllowed(field))
                initBeanParameter(bean, field, getParameterValue(bean, field, parameters), ctx);
        }
    }

    public static void refreshBeanParameters(Object bean, Map<String, Object> parameters, IProcessorContext ctx) {
        refreshBeanParameters(bean, bean.getClass(), parameters, ctx);
    }

    public static void refreshBeanParameters(Object bean, Class clazz, Map<String, Object> parameters, IProcessorContext ctx) {
        if (!isConsideredStructure(clazz)) return;

        for (Map.Entry<Field, ProcessorParameter> entry : getAllAnnotatedParameterFields(clazz).entrySet()) {
            Field field = entry.getKey();
            if (isParameterRefreshAllowed(bean, field, parameters))
                initBeanParameter(bean, field, getParameterValue(bean, field, parameters), ctx);
        }
    }

    public static boolean isParameterRefreshAllowed(Object bean, Field field, Map<String, Object> parameters) {
        if (field == null) return false;
        if (parameters == null) return false;
        if (field.isAnnotationPresent(ProcessorParameter.class)) {
            ProcessorParameter p = field.getAnnotation(ProcessorParameter.class);
            return !p.ignoreRefresh() && getParameterValue(bean, field, parameters) != null;
        }
        return false;
    }

    public static boolean isParameterInitializationAllowed(Field field) {
        if (field == null) return false;
        if (field.isAnnotationPresent(ProcessorParameter.class)) {
            ProcessorParameter p = field.getAnnotation(ProcessorParameter.class);
            return !p.ignoreInitialization();
        }
        return false;
    }

    public static boolean isParameterRequired(Field field) {
        if (field == null) return false;
        if (field.isAnnotationPresent(ProcessorParameter.class)) {
            ProcessorParameter p = field.getAnnotation(ProcessorParameter.class);
            return p.required();
        }
        return false;
    }

    //Start andere Methode zum Extrahieren der parameterMap
    /**
     * Extrahiert die vollstÃ¤ndige, verschachtelte Parameter-Map fÃ¼r ein gegebenes Bean.
     * Dies ist der zentrale Einstiegspunkt fÃ¼r die Serialisierung eines Prozessor-Zustands.
     *
     * @param context Der Transformation Kontext, der das zu serialisierende Bean enthÃ¤lt.
     * @return Eine Map, die die Parameter-Maps aller Kind-Strukturen enthÃ¤lt.
     */
    public static Map<String, Map<String, Object>> extractEffectiveParameterMap(ITransformationContext context) {
        Map<String, Map<String, Object>> beanParameterMap = new HashMap<>();
        if (context == null || context.getTargetObject() == null) {
            log.warn("extractEffectiveParameterMap aufgerufen mit leerem Kontext oder Zielobjekt.");
            return beanParameterMap;
        }
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        extractParametersRecursively(context, beanParameterMap, visited, LoadStrategy.DEEP); // <-- Modus DEEP
        return beanParameterMap;
    }

    /**
     * Extrahiert eine FLACHE Map der Parameter nur fÃ¼r das gegebene Top-Level-Objekt (Processor, ValueObject).
     * Kind-Objekte werden zu ihren Identifiern serialisiert, aber ihre eigenen Parameter werden nicht extrahiert.
     * @param context Der Transformation Kontext, der das zu serialisierende Bean enthÃ¤lt.
     * @return Eine Map, die die Parameter des Objektes enthÃ¤lt.
     */
    public static Map<String, Object> extractEffectiveParameters(ITransformationContext context) {
        Map<String, Map<String, Object>> temporaryBeanMap = new HashMap<>();
        if (context == null || context.getTargetObject() == null) {
            log.warn("extractEffectiveParameters aufgerufen mit leerem Kontext oder Zielobjekt.");
            return new HashMap<>();
        }
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        // Wir rufen die Rekursion im SHALLOW-Modus auf.
        Object rawValueIdentifier = extractParametersRecursively(context, temporaryBeanMap, visited, LoadStrategy.SHALLOW); // <-- Modus SHALLOW
        String identifier = getIdentifier(Objects.toString(rawValueIdentifier, null));

        // Wir geben nur die Parameter des Top-Level-Objekts zurÃ¼ck.
        return temporaryBeanMap.getOrDefault(identifier, new HashMap<>());
    }

    /**
     * NEU: Extrahiert den "rohen" Wert (typischerweise die ID) fÃ¼r ein einzelnes Objekt im Kontext.
     * Dies ist die atomare Operation, die von den anderen Methoden wiederverwendet wird.
     */
    public static Object getParameterObject(ITransformationContext context) {
        if (context == null || context.getTargetObject() == null) {
            return null;
        }
        // Wir nutzen eine private Helper-Methode, die auch die Rekursion verwendet.
        return getRawValueForContext(context);
    }

    /**
     * Private Helper-Methode, die den Kern der RÃ¼cktransformation kapselt.
     * Sie findet die ValueFunction und wendet sie an. Kein Seiteneffekt, keine Rekursion.
     */
    private static Object getRawValueForContext(ITransformationContext context) {
        Object sourceObject = context.getTargetObject();
        if (sourceObject == null) {
            return null;
        }

        IValueFunction<ITransformationContext, ?, ?> valueFunction = createValueFunctionForContext(context);
        // 2. Strikte PrÃ¼fung: Wenn keine Funktion gefunden wurde, ist das ein Fehler.
        if (valueFunction == null) {
            log.error("Keine IValueFunction fÃ¼r den Typ {} im Kontext {} gefunden. Der Typ ist nicht korrekt im Framework registriert. Kann den raw value nicht bestimmen.", sourceObject.getClass().getName(), context);
            return null;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        Object rawValue = ((IValueFunction) valueFunction).reverseTransformValue(context, sourceObject);
        return rawValue;
    }

    /**
     * Private, rekursive Helper-Methode, die die eigentliche Arbeit der Serialisierung leistet.
     * Sie nutzt den ITransformationContext als zentrales Vehikel fÃ¼r Informationen.
     *
     * @param context          Der Transformationskontext fÃ¼r das aktuell zu verarbeitende Objekt.
     * @param beanParameterMap Die Haupt-Map, die mit den Ergebnissen befÃ¼llt wird (Seiteneffekt).
     * @param visited          Ein Set zur Erkennung von Zyklen, um Endlosschleifen zu verhindern.
     * @param mode             gibt die Tiefe der Rekursion an
     * @return Der "rohe" Wert (typischerweise der Identifier) des Objekts im Kontext, der vom Eltern-Objekt verwendet wird.
     */
    private static Object extractParametersRecursively(ITransformationContext context, Map<String, Map<String, Object>> beanParameterMap, Set<Object> visited, LoadStrategy mode) {
        Object sourceObject = context.getTargetObject();
        if (sourceObject == null) {
            return null;
        }

        // 1. Finde die zustÃ¤ndige ValueFunction. Dank des vollstÃ¤ndigen Kontexts ist diese Auswahl nun maximal prÃ¤zise.
        IValueFunction<ITransformationContext, ?, ?> valueFunction = createValueFunctionForContext(context);

        // 2. Strikte PrÃ¼fung: Wenn keine Funktion gefunden wurde, ist das ein Fehler.
        if (valueFunction == null) {
            log.error("Keine IValueFunction fÃ¼r den Typ {} im Kontext {} gefunden. Der Typ ist nicht korrekt im Framework registriert. Kann nicht serialisiert werden.", sourceObject.getClass().getName(), context);
            return null;
        }

        // 2. Ermittle den rohen Wert (Identifier) des aktuellen Objekts durch RÃ¼cktransformation.
        @SuppressWarnings({"rawtypes", "unchecked"})
        Object rawValueIdentifier = ((IValueFunction) valueFunction).reverseTransformValue(context, sourceObject);
        String identifier = getIdentifier(Objects.toString(rawValueIdentifier, null));

        // 3. Zykluserkennung: Wenn wir dieses Objekt schon besucht haben, stoppen wir die Rekursion.
        if (!visited.add(sourceObject)) {
            log.trace("Zyklus erkannt fÃ¼r Objekt '{}' mit Identifier '{}'. Stoppe Rekursion.", sourceObject.getClass().getSimpleName(), identifier);
            return rawValueIdentifier;
        }

        log.debug("Serialisiere Objekt '{}' mit Identifier '{}'", sourceObject.getClass().getSimpleName(), identifier);

        // 4. Wenn es sich nicht um eine komplexe Struktur handelt oder kein Identifier existiert, sind wir hier fertig.
        if (!isConsideredStructure(sourceObject.getClass()) || identifier == null) {
            return rawValueIdentifier;
        }

        // 5. Es ist eine komplexe Struktur. Sammle die Parameter der Kinder.
        Map<String, Object> parameters = new HashMap<>();
        for (Map.Entry<Field, ProcessorParameter> entry : getAllAnnotatedParameterFields(sourceObject.getClass()).entrySet()) {
            Field field = entry.getKey();
            ProcessorParameter p = entry.getValue();

            if (p.ignoreExtractParameter()) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object childObject = field.get(sourceObject);

                // 6. Erzeuge einen NEUEN, KORREKTEN Kontext fÃ¼r das Kind-Objekt.
                DefaultTransformationContext childContext = new DefaultTransformationContext();
                childContext.setTargetObject(childObject); // Das Kind ist das neue Zielobjekt.
                childContext.setTargetField(field);        // Das Feld, in dem das Kind lebt.
                childContext.setTargetType(field.getType()); // Der Typ des Feldes.
                childContext.setRuntimeContext(context.getRuntimeContext()); // Der RuntimeContext bleibt gleich.
                childContext.setProcessorParameterAnnotation(p); // Die Annotation des Feldes.

                // 7. Rekursiver Aufruf mit dem neuen, prÃ¤zisen Kind-Kontext.
                Object childRawValue;
                if (mode == LoadStrategy.DEEP) {
                    // Im DEEP-Modus: Rekursiver Aufruf, um die volle Tiefe zu erfassen.
                    childRawValue = extractParametersRecursively(childContext, beanParameterMap, visited, LoadStrategy.DEEP);
                } else {
                    // Im SHALLOW-Modus (oder jedem anderen):
                    // Wir holen uns nur den rohen Wert des Kindes, ohne weiter zu traversieren.
                    // DafÃ¼r brauchen wir eine neue, kleine Helper-Methode.
                    childRawValue = getRawValueForContext(childContext);
                }

                // 8. FÃ¼ge den "flachen" Wert des Kindes zur Parameter-Map des aktuellen Objekts hinzu.
                parameters.put(getParameterName(field), childRawValue);

            } catch (Exception e) {
                log.error("Fehler beim Zugriff auf Feld '{}' fÃ¼r Parameter-Extraktion in Klasse '{}'.", field.getName(), sourceObject.getClass().getName(), e);
            }
        }

        // 9. FÃ¼ge die fertig gesammelten Parameter des aktuellen Objekts zur Haupt-Map hinzu.
        log.debug("Parameter fÃ¼r Identifier '{}' gesammelt: {}", identifier, parameters.keySet());
        beanParameterMap.put(identifier, parameters);

        // 10. Gib den Identifier des aktuellen Objekts an den aufrufenden Eltern-Prozess zurÃ¼ck.
        //    Dieser Wert wird in der Parameter-Map des Eltern-Objekts gespeichert.
        return rawValueIdentifier;
    }

    //End andere Methode

    /*
     * In case a file is being copied into the deploy directory, the file is created first and then written afterwards.
     * Therefore we need to wait until the copy-process is completed before unmarshalling.
     */
    private static void waitUntilFileIsReleased(File file) {
        if (file == null) return;
        LocalDateTime current = LocalDateTime.now();
        LocalDateTime end = current.plusSeconds(10);
        while(isLocked(file) && current.isBefore(end)) { current = LocalDateTime.now(); }
    }

    private static boolean isLocked(File file) {
        try (FileReader fr = new FileReader(file)) {
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    public static MultiValueMap<String, Object> createProcessorHttpFormData(IProcessor processor) {
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("requestProcessorIdentifier", new HttpEntity<>(processor.getFullBeanId(), createTextHeaders(processor.getFullBeanId() != null ? processor.getFullBeanId().length() : 0)));
        formData.add("beanParameterMap", new HttpEntity<>(MapUtils.getJsonRepresentation(processor.extractEffectiveProcessorParameterMap()), createJsonHeaders(MapUtils.getJsonRepresentation(processor.extractEffectiveProcessorParameterMap()).length())));
        return formData;
    }

    public static HttpHeaders createTextHeaders(long length) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.TEXT_PLAIN);
        httpHeaders.setContentLength(length);
        return httpHeaders;
    }

    public static HttpHeaders createJsonHeaders(long length) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setContentLength(length);
        return httpHeaders;
    }

    public static HttpHeaders createMultipartHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        return httpHeaders;
    }

    public static HttpHeaders createOCTETHeaders(long length, String attachmentName, String filename) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        httpHeaders.setContentLength(length);
        httpHeaders.setContentDisposition(ContentDisposition.attachment().name(attachmentName).filename(Objects.requireNonNull(filename)).build());
        return httpHeaders;
    }

    public static HttpHeaders createXmlHeaders(long length, String attachmentName, String filename) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_XML);
        httpHeaders.setContentLength(length);
        httpHeaders.setContentDisposition(ContentDisposition.attachment().name(attachmentName).filename(Objects.requireNonNull(filename)).build());
        return httpHeaders;
    }

    /**
     * Generiert eine sprachunabhÃ¤ngige Typsignatur fÃ¼r ein gegebenes Java-Feld.
     * BerÃ¼cksichtigt Listen und Maps.
     */
    public static String generateTypeSignature(Field field) {
        if (field == null) return "unknown";

        Class<?> fieldType = field.getType();

        if (List.class.isAssignableFrom(fieldType)) {
            // Annahme: getGenericType(field) existiert und extrahiert den Typ aus List<T>
            Class<?> genericType = getGenericType(field);
            String elementSignature = generateTypeSignatureForClass(genericType);
            return "list<" + elementSignature + ">";
        }
        if (Map.class.isAssignableFrom(fieldType)) {
            // Vereinfacht fÃ¼r dieses Beispiel
            return "map";
        }

        return generateTypeSignatureForClass(fieldType);
    }

    /**
     * Generiert die Typsignatur fÃ¼r eine einzelne Klasse.
     * Hier wird zwischen processor und valueObject unterschieden.
     */
    private static String generateTypeSignatureForClass(Class<?> clazz) {
        if (clazz == null) return "unknown";

        // Skalare Typen
        if (String.class.isAssignableFrom(clazz)) return "string";
        if (Integer.class.isAssignableFrom(clazz) || int.class.isAssignableFrom(clazz)) return "integer";
        if (Long.class.isAssignableFrom(clazz) || long.class.isAssignableFrom(clazz)) return "long";
        if (Double.class.isAssignableFrom(clazz) || double.class.isAssignableFrom(clazz)) return "double";
        if (Boolean.class.isAssignableFrom(clazz) || boolean.class.isAssignableFrom(clazz)) return "boolean";
        if (Date.class.isAssignableFrom(clazz)) return "datetime";

        // Strukturierte Typen (die wichtige Unterscheidung)
        if (IProcessor.class.isAssignableFrom(clazz)) {
            return "processor";
        }
        if (clazz.isAnnotationPresent(ValueObject.class)) {
            return "valueObject";
        }

        return "object"; // Fallback
    }
}