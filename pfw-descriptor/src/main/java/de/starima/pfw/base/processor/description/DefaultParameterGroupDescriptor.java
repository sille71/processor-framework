package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.api.IBeanProvider;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.api.IParameterDescriptor;
import de.starima.pfw.base.processor.description.api.IParameterGroupDescriptor;
import de.starima.pfw.base.util.MapUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@Getter
@Setter
@Slf4j
@Processor
public class DefaultParameterGroupDescriptor extends DefaultParameterDescriptor implements IParameterGroupDescriptor {
    @ProcessorParameter
    private List<IParameterDescriptor> parameterDescriptors;
    @ProcessorParameter(value = "defaultBeanProvider")
    private IBeanProvider beanProvider;

    @Override
    public void processorOnInit() {
        if (this.parameterDescriptors != null) {
            this.parameterDescriptors.forEach(descriptor -> descriptor.setParentDescriptor(this));
        }
    }

    public void mergeGroupDescriptor(IParameterGroupDescriptor prototype) {
        if (prototype == null) return;
        if (parameterDescriptors == null) {
            parameterDescriptors = prototype.getParameterDescriptors();
        } else {
            //wir fÃ¼gen zuerst alle descriptoren aus dem prototyp hinzu, die nicht in der Liste der parameterDescriptors enthalten sind
            //TODO
        }
    }

    @Override
    public IParameterDescriptor getParameterDescriptor(String parameterName) {
        if (parameterName == null) return null;
        if (parameterDescriptors != null) {
            try {
                return parameterDescriptors.stream().filter(parameterDescriptor -> parameterName.equalsIgnoreCase(parameterDescriptor.getParameterName())).findFirst().get();
            } catch (Exception e) {
                log.warn("{}: Can not find parameter {}", this.getIdentifier(), parameterName);
            }
        }
        return null;
    }

    @Override
    public void addParameterDescriptor(IParameterDescriptor parameterDescriptor) {
        if (parameterDescriptors == null) parameterDescriptors = new ArrayList<>();
        parameterDescriptors.add(parameterDescriptor);
    }

    @Override
    public void initBeanParameters(Object bean, Map<String, Object> parameters) {
        if (!StringUtils.isEmpty(getParameterName())) {
            if (getValueDescriptor() != null && getValueDescriptor().getValueFunction() != null) {
                /**der GroupDescriptor fungiert wie ein Parameterdescriptor
                 * z.B. kann hie der JacksonParameterFunctionProcessor verwendet werden, um das RcnAttribute zu erzeugen
                 */
                super.initBeanParameters(bean, parameters);
            } else if (parameterDescriptors != null && beanProvider != null) {
                // der Parameter ist eine Bean (der parameterName entspricht dem Bean Namen), welches mit den hier definierten parameterDescriptoren initiiert wird
                try {
                    Object parameterBean = beanProvider.getBeanForId(getParameterName());
                    if (parameterBean != null) {
                        parameterDescriptors.forEach(descriptor -> descriptor.initBeanParameters(parameterBean, parameters));
                    }
                } catch (Exception e) {
                    log.error("Can not create bean {} for property {} of processor {}", getParameterName(), getPropertyName(), bean, e);
                }
            }
        }

        if (parameterDescriptors == null) return;

        parameterDescriptors.forEach(descriptor -> descriptor.initBeanParameters(bean, parameters));
    }

    @Override
    public void bindEffectiveParameterValues(Object bean) {
        if (bean == null) return;
        if (!StringUtils.isEmpty(getParameterName())) {
            if (getValueDescriptor() != null && getValueDescriptor().getValueFunction() != null) {
                /**der GroupDescriptor fungiert wie ein Parameterdescriptor
                 * z.B. kann hie der JacksonParameterFunctionProcessor verwendet werden, um das RcnAttribute zu erzeugen
                 */
                super.bindEffectiveParameterValues(bean);
            } else if (parameterDescriptors != null && beanProvider != null) {
                // der Parameter ist eine Bean (der parameterName entspricht dem Bean Namen), welches mit den hier definierten parameterDescriptoren initiiert wird
                try {
                    Object parameterBean = beanProvider.getBeanForId(getParameterName());
                    if (parameterBean != null) {
                        parameterDescriptors.forEach(descriptor -> descriptor.bindEffectiveParameterValues(parameterBean));
                    }
                } catch (Exception e) {
                    log.error("Can not create bean {} for property {} of processor {}", getParameterName(), getPropertyName(), bean, e);
                }
            }
        }

        if (parameterDescriptors == null) return;
        parameterDescriptors.forEach(parameterDescriptor -> parameterDescriptor.bindEffectiveParameterValues(bean));
    }

    @Override
    public Map<String, Object> extractEffectiveParameters(Object bean) {
        if (bean == null) return null;
        //TODO hier noch den Fall parameterName != null behandeln

        if (parameterDescriptors == null) return null;
        HashMap<String, Object> parameters = new HashMap<>();
        parameterDescriptors.forEach(parameterDescriptor -> MapUtils.mergeMaps(parameters, parameterDescriptor.extractEffectiveParameters(bean)));
        return parameters;
    }

    /**
     * Falls der Parameter des Ã¼bergebenen sourceProcessors wieder Prozessoren beschreibt, so wird hier die parameterMap dieser Prozessoren zurÃ¼ckgegeben.
     * Diese vervollstÃ¤ndigt dann die parameterMap des sourceProcessors. Der sourceProcessor kann dann damit wiederhergestellt werden.
     * @param bean
     * @return
     */
    @Override
    public Map<String, Map<String, Object>> extractEffectiveParameterMap(Object bean) {
        if (bean == null) return null;
        //TODO hier noch den Fall parameterName != null behandeln

        if (parameterDescriptors == null) return null;
        Map<String, Map<String, Object>> parameterMap = new HashMap<>();
        parameterDescriptors.forEach(parameterDescriptor -> MapUtils.mergeBeanIdParameterMap(parameterMap, parameterDescriptor.extractEffectiveParameterMap(bean)));
        return parameterMap;
    }

    @Override
    public String getJsonRepresentation(Object value) {
        return null;
    }

    @Override
    public String getPath() {
        // Ein ParameterGroupDescriptor hat keinen eigenen Pfad-Anteil. Er "lebt" im Pfad seines Eltern-Deskriptors.
        return getParentDescriptor() != null ? getParentDescriptor().getPath() : "";
    }

    @Override
    public Optional<IDescriptorProcessor> findDescriptor(String path) {
        if (path == null || path.isEmpty() || getParameterDescriptors() == null) {
            return Optional.empty();
        }

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

        return Optional.empty();
    }
}