package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.domain.ProcessorScope;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.context.api.LoadStrategy;
import de.starima.pfw.base.processor.description.api.*;
import de.starima.pfw.base.util.IdentifierUtils;
import de.starima.pfw.base.util.MapUtils;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * {@code DefaultStructureValueDescriptor} ist eine Implementierung von {@link IStructureValueDescriptor}.
 * Sie beschreibt einen komplexen, zusammengesetzten Wert, der selbst wieder Parameter enthÃ¤lt.
 * Dies ist der Basis-Deskriptor fÃ¼r Objekte, die mit {@code @Processor} oder {@code @ValueObject} annotiert sind.
 * Er fungiert als Container fÃ¼r eine Gruppe von {@link IParameterDescriptor}-Instanzen.
 *
 * <p><b>Prozessor-Beschreibung:</b></p>
 * <p><b>Name:</b> DefaultStructureValueDescriptor</p>
 * <p><b>Beschreibung:</b> Beschreibt einen komplexen Parameterwert, der selbst Parameter enthÃ¤lt (z.B. ein Prozessor oder ein Wertobjekt).</p>
 * <p><b>Kategorien:</b> descriptor/value/structure</p>
 * <p><b>Parameter:</b></p>
 * <ul>
 *     <li>{@code currentValue}: Die Instanz des beschriebenen Struktur-Objekts.</li>
 *     <li>{@code valueFunction}: Die {@link IValueFunction}, die fÃ¼r die Transformation und Validierung der Struktur zustÃ¤ndig ist.</li>
 *     <li>{@code parameterGroupDescriptor}: Der {@link IParameterGroupDescriptor}, der alle Parameter dieser Struktur enthÃ¤lt.</li>
 *     <li>{@code categories}: Die Kategorien dieser Struktur (aus der {@code @Processor} oder {@code @ValueObject} Annotation).</li>
 *     <li>{@code subCategories}: Die Sub-Kategorien dieser Struktur.</li>
 *     <li>{@code tags}: Die Tags dieser Struktur.</li>
 * </ul>
 */
@Slf4j
@Getter
@Setter
@Processor(
    description = "Beschreibt einen komplexen Parameterwert, der selbst Parameter enthÃ¤lt (z.B. ein Prozessor oder ein Wertobjekt).",
    categories = {"descriptor/value/structure"}
)
public class DefaultStructureValueDescriptor<T extends IStructureValueDescriptor<T>> extends DefaultValueDescriptor implements IStructureValueDescriptor<T> {
    //TODO: wird nicht mehr benÃ¶tigt dafÃ¼r gibt es getPrototypeValueDescriptor()
    private T prototypeDescriptor;
    @ProcessorParameter
    private Class<?> sourceClass;
    @ProcessorParameter
    private String sourceIdentifier;
    @ProcessorParameter
    private String sourcePrototypeIdentifier;
    @ProcessorParameter
    private String sourceScope;
    @ProcessorParameter(description = "Der ParameterGroupDescriptor, der alle Parameter dieser Struktur enthÃ¤lt.")
    private IParameterGroupDescriptor parameterGroupDescriptor;

    @ProcessorParameter(description = "Liste von ParameterDescriptoren. Ist eine eeinfache Alternative zu parameterGroupDescriptor")
    List<IParameterDescriptor> parameterDescriptors;

    @ProcessorParameter(description = "Die Kategorien dieser Struktur (aus der @Processor oder @ValueObject Annotation).")
    private String[] categories;

    @ProcessorParameter(description = "Die Sub-Kategorien dieser Struktur.")
    private String[] subCategories;

    @ProcessorParameter(description = "Die Tags dieser Struktur.")
    private String[] tags;

    @ProcessorParameter(description = "Liste von parameterName|value Paaren. Damit kann der value() in der ProcessorParameter Annotation Ã¼berschrieben werden.\n" +
            "     Das macht in Subklassen Sinn, die den default Wert der Super Klasse Ã¼berschreiben sollen.\n" +
            "     Zu beachten ist, das hier der Parametername angegeben wird, wie er in der ProcessorParameter Annotation des Feldes definiert ist (in der Regel wird dies der Propertyname sein)")
    private String[] defaultValues;

    @Override
    public void processorOnInit() {
        super.processorOnInit();
        if (parameterGroupDescriptor != null) {
            parameterGroupDescriptor.setParentDescriptor(this);
        }

        if (this.parameterDescriptors != null) {
            this.parameterDescriptors.forEach(descriptor -> descriptor.setParentDescriptor(this));
        }
    }

    @Override
    public List<IParameterDescriptor> getParameterDescriptors() {
        if (parameterGroupDescriptor != null) {
            return parameterGroupDescriptor.getParameterDescriptors();
        }
        return this.parameterDescriptors;
    }

    @Override
    public String getSourceIdentifier() {
        if (sourceIdentifier == null) {
            sourceIdentifier = createIdentifier();
        }

        if (StringUtils.trimToNull(sourceIdentifier) == null && prototypeDescriptor != null) {
            sourceIdentifier = prototypeDescriptor.getSourceIdentifier();
        }
        return sourceIdentifier;
    }

    protected String createIdentifier() {
        return ProcessorUtils.getIdentifier(this.getSourceFullBeanId());
    }

    public String getSourcePrototypeIdentifier() {
        if (sourcePrototypeIdentifier == null && prototypeDescriptor != null) {
            sourcePrototypeIdentifier = prototypeDescriptor.getSourcePrototypeIdentifier();
        }
        return sourcePrototypeIdentifier;
    }

    @Override
    public String getSourceFullBeanId() {
        return ProcessorUtils.createFullBeanId(sourcePrototypeIdentifier, sourceIdentifier, ProcessorScope.valueOf(sourceScope));
    }

    @Override
    public boolean isResponsibleFor(Object sourceProcessor) {
        return sourceProcessor != null && ProcessorUtils.isConsideredStructure(sourceProcessor.getClass());
    }

    // Die Methoden getCategories(), getSubCategories(), getTags() werden von Lombok generiert.
    // Falls sie nicht als @ProcessorParameter annotiert wÃ¤ren, mÃ¼sste man sie hier implementieren
    // und die Werte aus der Annotation des Source-Objekts (currentValue) extrahieren.
    // Da sie aber als Parameter gesetzt werden, sind die Getter direkt nutzbar.

    /**
     * Initialisiert die Parameter des beschriebenen Struktur-Objekts.
     * Diese Methode wird vom {@link DefaultParameterDescriptor} aufgerufen.
     *
     * @param bean Das zu initialisierende Struktur-Objekt (z.B. ein IProcessor).
     * @param parameters Die Parameter-Map.
     */
    public void initBeanParameters(Object bean, Map<String, Object> parameters) {
        if (!isResponsibleFor(bean)) return;
        if (parameterGroupDescriptor != null) {
            parameterGroupDescriptor.initBeanParameters(bean, parameters);
        } else if (parameterDescriptors != null) {
            parameterDescriptors.forEach(descriptor -> descriptor.initBeanParameters(bean, parameters));
        }
    }

    /**
     * Bindet die effektiven Parameterwerte des beschriebenen Struktur-Objekts an diesen Deskriptor.
     *
     * @param bean Das Struktur-Objekt, dessen Werte gebunden werden sollen.
     */
    @Override
    public void bindEffectiveParameterValues(Object bean) {
        if (parameterGroupDescriptor != null) {
            parameterGroupDescriptor.bindEffectiveParameterValues(bean);
        } else if (parameterDescriptors != null) {
            parameterDescriptors.forEach(descriptor -> descriptor.bindEffectiveParameterValues(bean));
        }
    }

    /**
     * Extrahiert die effektiven Parameterwerte des beschriebenen Struktur-Objekts.
     *
     * @param bean Das Struktur-Objekt, dessen Werte extrahiert werden sollen.
     * @return Eine Map der extrahierten Parameter.
     */
    @Override
    public Map<String, Object> extractEffectiveParameters(Object bean) {
        Object value = bean != null ? bean : getResolvedValue();
        if (value == null) return new HashMap<>();
        if (parameterGroupDescriptor != null)
            return parameterGroupDescriptor.extractEffectiveParameters(value);
        if (parameterDescriptors == null) return new HashMap<>();
        HashMap<String, Object> parameters = new HashMap<>();
        parameterDescriptors.forEach(parameterDescriptor -> MapUtils.mergeMaps(parameters, parameterDescriptor.extractEffectiveParameters(value)));
        return parameters;
    }

    @Override
    public Map<String, Object> extractEffectiveParametersForResolvedValue() {
        if (getResolvedValue() == null) return new HashMap<>();
        if (parameterGroupDescriptor != null) parameterGroupDescriptor.extractEffectiveParametersForResolvedValue();
        if (parameterDescriptors == null) return new HashMap<>();
        HashMap<String, Object> parameters = new HashMap<>();
        parameterDescriptors.forEach(parameterDescriptor -> MapUtils.mergeMaps(parameters, parameterDescriptor.extractEffectiveParametersForResolvedValue()));
        return parameters;
    }

    public void extractEffectiveParameterMap(ITransformationContext context, Map<String, Map<String, Object>> beanParameterMap, Set<Object> visited) {
        Object bean = context.getTargetObject();
        if (bean == null || !visited.add(bean)) {
            return; // Null-Wert oder Zyklus erkannt
        }

        String identifier = IdentifierUtils.getIdentifierForValueObject(bean);
        if (identifier == null) return;

        // 1. Flache Parameter extrahieren und in die globale Map eintragen.
        beanParameterMap.put(identifier, this.extractEffectiveParameters(bean));
        log.debug("Parameter fÃ¼r '{}' extrahiert.", identifier);

        // 2. Rekursion nur im DEEP-Modus. Die LoadStrategy kommt aus dem Kontext!
        LoadStrategy loadStrategy = context.getLoadStrategy() != null ? context.getLoadStrategy() : LoadStrategy.DEEP;
        if (loadStrategy != LoadStrategy.DEEP) {
            return;
        }

        // 3. Gehe die Kind-Parameter durch und rufe die Rekursion direkt auf ihnen auf.
        if (getParameterDescriptors() != null) {
            for (IParameterDescriptor paramDescriptor : getParameterDescriptors()) {
                // Der Kontext bleibt der des Eltern-Beans, da der ParameterDescriptor
                // den Wert aus diesem Bean extrahieren muss.
                paramDescriptor.extractEffectiveParameterMap(context, beanParameterMap, visited);
            }
        }
    }

    @Override
    public Map<String, Map<String, Object>> extractEffectiveParameterMapForResolvedValue() {
        if (getResolvedValue() == null) return new HashMap<>();
        Map<String, Map<String, Object>> effectiveBeanParameterMap = new HashMap<>();
        //Im Falle von Processoren/Wertobjekten ist der rawValue der Full Identifier!
        String identifier = ProcessorUtils.getIdentifier(Objects.toString(createRawValueFromResolvedValue(), null));

        if (identifier == null) return null;

        effectiveBeanParameterMap.put(identifier, this.extractEffectiveParametersForResolvedValue());

        if (getParameterGroupDescriptor() != null) {
            Map<String, Map<String,Object>> parameters = new HashMap<>();
            parameters = getParameterGroupDescriptor().extractEffectiveParameterMapForResolvedValue();
            return MapUtils.mergeBeanIdParameterMap(effectiveBeanParameterMap, parameters);
        } else if (getParameterDescriptors() != null) {
            Map<String, Map<String,Object>> parameters = new HashMap<>();
            parameterDescriptors.forEach(parameterDescriptor -> MapUtils.mergeMaps(parameters, parameterDescriptor.extractEffectiveParameterMapForResolvedValue()));
            return MapUtils.mergeBeanIdParameterMap(effectiveBeanParameterMap, parameters);
        };
        return effectiveBeanParameterMap;
    }

    /**
     * Extrahiert die effektive Parameter-Map des beschriebenen Struktur-Objekts.
     *
     * @param bean Das Struktur-Objekt, dessen Parameter-Map extrahiert werden soll.
     * @return Eine Map von Bean-IDs zu Parameter-Maps.
     */
    @Override
    public Map<String, Map<String, Object>> extractEffectiveParameterMap(Object bean) {
        Object value = bean != null ? bean : getResolvedValue();
        if (value == null) return new HashMap<>();
        Map<String, Map<String, Object>> effectiveBeanParameterMap = new HashMap<>();
        effectiveBeanParameterMap.put(sourceIdentifier, this.extractEffectiveParameters(bean));
        if (parameterGroupDescriptor != null) return MapUtils.mergeBeanIdParameterMap(effectiveBeanParameterMap, parameterGroupDescriptor.extractEffectiveParameterMap(value));
        if (parameterDescriptors == null) return new HashMap<>();
        parameterDescriptors.forEach(parameterDescriptor -> MapUtils.mergeBeanIdParameterMap(effectiveBeanParameterMap, parameterDescriptor.extractEffectiveParameterMap(value)));
        return effectiveBeanParameterMap;
    }

    /**
     * Extrahiert die effektive Parameter-Map der untergeordneten Parameter dieses Struktur-Objekts.
     *
     * @param transformationContext Der Transformationskontext.
     * @param bean Das Struktur-Objekt, dessen untergeordnete Parameter extrahiert werden sollen.
     * @return Eine Map von Bean-IDs zu den Parameter-Maps der Kind-Strukturen.
     */
    @Override
    public Map<String, Map<String, Object>> extractEffectiveParameterMap(ITransformationContext transformationContext, Object bean) {
        Object value = bean != null ? bean : getResolvedValue();
        if (value == null) {
            return new HashMap<>();
        }

        // Die einzige Verantwortung: an die Kinder delegieren und die Ergebnisse sammeln.
        Map<String, Map<String, Object>> childrenParameterMap = new HashMap<>();

        if (getParameterGroupDescriptor() != null) {
            // Delegation an die Gruppe ist der bevorzugte Weg, wenn vorhanden.
            return getParameterGroupDescriptor().extractEffectiveParameterMap(transformationContext, value);
        }

        if (getParameterDescriptors() != null) {
            for (IParameterDescriptor paramDescriptor : getParameterDescriptors()) {
                // Korrekte Delegation an jeden Kind-Parameter-Deskriptor mit vollem Kontext.
                Map<String, Map<String, Object>> childMap = paramDescriptor.extractEffectiveParameterMap(transformationContext, value);
                // Merge die Ergebnisse
                MapUtils.mergeBeanIdParameterMap(childrenParameterMap, childMap);
            }
        }

        return childrenParameterMap;
    }

    @Override
    public String getPath() {
        // Wenn es keinen Parent gibt, ist dies der Root-Deskriptor. Sein Pfad ist sein FullBeanId.
        if (getParentDescriptor() == null) {
            return ProcessorUtils.createFullBeanId(sourcePrototypeIdentifier, sourceIdentifier, ProcessorScope.valueOf(sourceScope));
        }
        // Ansonsten wird der Pfad des Parents mit dem eigenen Pfadsegment kombiniert.
        return getParentDescriptor().getPath() + "/" + ProcessorUtils.createFullBeanId(sourcePrototypeIdentifier, sourceIdentifier, ProcessorScope.valueOf(sourceScope));
    }

    @Override
    public Optional<IDescriptorProcessor> findDescriptor(String path) {
        if (path == null || path.isEmpty()) {
            return Optional.empty();
        }
        // PrÃ¼fen, ob dieser Deskriptor selbst gemeint ist.
        if (equalsPath(path)) {
            return Optional.of(this);
        }

        // Wenn der Pfad mit unserem Pfad beginnt, suchen wir in den Kindern.
        String myPath = getPath();
        if (path.startsWith(myPath + "/")) {
            String remainingPath = path.substring(myPath.length() + 1);
            // Die Suche wird an den ParameterGroupDescriptor delegiert.
            if (this.parameterGroupDescriptor != null) {
                return this.parameterGroupDescriptor.findDescriptor(remainingPath);
            }

            //falls kein GroupDescriptor vorhanden ist, fragen wir nach der ParameterList
            if (getParameterDescriptors() != null) {
                // Teile den Pfad in das nÃ¤chste Segment und den Rest.
                String[] parts = path.split("/", 2);
                String nextSegment = parts[0];
                // Finde den ParameterDescriptor, der fÃ¼r das nÃ¤chste Segment verantwortlich ist.
                for (IParameterDescriptor paramDescriptor : getParameterDescriptors()) {
                    if (nextSegment.equals(paramDescriptor.getParameterName())) {
                        // Wenn der Pfad nur aus diesem Segment bestand, haben wir ihn gefunden.
                        if (parts.length == 1) {
                            return Optional.of(paramDescriptor);
                        }
                        // Ansonsten delegieren wir die Suche nach dem restlichen Pfad an den gefundenen Parameter.
                        return paramDescriptor.findDescriptor(parts[1]);
                    }
                }
            }
        }

        return Optional.empty();
    }

    protected boolean equalsPath(String path) {
        return ProcessorUtils.equalsPath(path,sourcePrototypeIdentifier,sourceIdentifier, ProcessorScope.valueOf(sourceScope));
    }
}