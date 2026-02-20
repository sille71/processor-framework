package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.set.api.ISetProcessor;
import de.starima.pfw.base.util.MapUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
@Setter
@Processor()
public class IntegerValueFunction extends AbstractValueFunction<Object, Integer>  {
    public static boolean isResponsibleFor(ITransformationContext transformationContext) {
        if (transformationContext == null) return false;
        Class<?> clazz = transformationContext.getTargetType();
        Field field = transformationContext.getTargetField();
        Class<?> valueType = field != null ? field.getType() : clazz;
        return valueType != null &&  (int.class.isAssignableFrom(valueType) || Integer.class.isAssignableFrom(valueType));
    }

    private final Map<String,Map<String,Object>>  defaultBeanParameterMap = new HashMap<>();

    //Beispiel fÃ¼r das implementieren von defaults
    public Map<String,Map<String,Object>>  initDefaultBeanParameterMap() {
        HashMap<String,Object> simpleTypeSetMap = new HashMap<>();
        defaultBeanParameterMap.put("coDomainSet", simpleTypeSetMap);
        simpleTypeSetMap.put("type", "Integer");
        MapUtils.mergeBeanIdParameterMap(defaultBeanParameterMap, super.initDefaultBeanParameterMap());
        return defaultBeanParameterMap;
    }

    //TODO: hier defaultwerte angeben! Die set Processoren benÃ¶tigen wir im ReconLight
    @ProcessorParameter(value = "simpleTypeSet:domainSet", description = "Definitionsbereich der Parameter. Dies kÃ¶nnen Integer oder auch Strings sein. ")
    private ISetProcessor<Object> domainProcessor;
    @ProcessorParameter(value = "simpleTypeSet:coDomainSet", description = "Wertebereich der Parameter. Dies kÃ¶nnen Integer oder auch Strings sein. ")
    private ISetProcessor<Integer> coDomainProcessor;
    @ProcessorParameter
    private ISetProcessor<Integer> imageProcessor;


    @Override
    public ISetProcessor<Object> getDomainProcessor() {
        return domainProcessor;
    }

    @Override
    public ISetProcessor<Integer> getCoDomainProcessor() {
        return coDomainProcessor;
    }

    @Override
    public ISetProcessor<Integer> getImageProcessor() {
        return imageProcessor;
    }

    @Override
    public boolean isResponsibleForSubject(ITransformationContext transformationContext) {
        return isResponsibleFor(transformationContext);
    }

    @Override
    public Integer transformValue(Object input) {
        if (!isResponsibleForInput(input)) {
            log.warn("{}.transformValue: Can not transform value {}! Value is not in domain.", this.getIdentifier(), input);
            return null;
        }
        try {
            if (input instanceof Number) {
                return ((Number)input).intValue();
            } else if (input != null && !"".equals(input))
                return Integer.parseInt(input.toString());
        } catch (NumberFormatException e) {
            log.warn("{}.transformValue: Can not transform value {}!", this.getIdentifier(), input, e);
        }
        log.warn("{}.transformValue: Can not transform value {}! Value is properly not in domain.", this.getIdentifier(), input);
        return null;
    }

    public Object transformObjectToParameter(Integer value) {
        if (value == null) return "";
        return value.toString();
    }

    @Override
    public String getTypeSignature() {
        return "integer";
    }

    @Override
    public boolean isGeneric() {
        return false;
    }
}