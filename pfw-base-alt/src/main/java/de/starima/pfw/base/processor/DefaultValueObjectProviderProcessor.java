package de.starima.pfw.base.processor;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ValueObject;
import de.starima.pfw.base.processor.api.IBeanProvider;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.context.api.ITaskContext;
import de.starima.pfw.base.processor.description.api.IStructureValueDescriptor;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ein spezialisierter IBeanProvider, der ausschlieÃŸlich fÃ¼r die Erzeugung und
 * Initialisierung von Instanzen zustÃ¤ndig ist, die mit @ValueObject annotiert sind.
 * Er nutzt das "prototypeId:identifier@scope"-Muster und fÃ¼hrt eine rekursive
 * Initialisierung der Bean-Parameter durch.
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Processor(
    description = "Erzeugt und initialisiert @ValueObject-Instanzen basierend auf einer Bean-ID und Kontextparametern.",
    categories = {"provider", "valueObject"}
)
public class DefaultValueObjectProviderProcessor extends AbstractProcessor implements IBeanProvider, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public <T> T getBeanForId(Class<T> clazz, String beanId) throws Exception {
        log.debug("DefaultValueObjectProvider getting bean for id: '{}' and class: '{}'", beanId, clazz.getName());
        if (!StringUtils.hasLength(beanId)) {
            throw new IllegalArgumentException("Specified beanId is null or empty.");
        }

        String prototypeId = ProcessorUtils.getPrototypeId(beanId);
        Object prototypeBean = applicationContext.getBean(prototypeId);

        if (!ProcessorUtils.isConsideredValueObject(prototypeBean.getClass())) {
            throw new IllegalArgumentException("Prototype '" + prototypeId + "' is not a @ValueObject. This provider can only handle ValueObjects.");
        }

        if (!clazz.isAssignableFrom(prototypeBean.getClass())) {
             throw new ClassCastException(String.format("Prototype bean '%s' of type %s cannot be cast to requested type %s",
                prototypeId, prototypeBean.getClass().getName(), clazz.getName()));
        }

        // Es ist ein ValueObject, wir Ã¼bernehmen die Initialisierung.
        T beanInstance = (T) prototypeBean;
        initBean(beanInstance, beanId, getRuntimeContext());
        return beanInstance;
    }

    //TODO: stattdessen getBeanForIdAndContext analog zum UI einfÃ¼hren
    @Override
    public Object getBeanForId(String beanId, IStructureValueDescriptor<?> descriptor) throws Exception {
        String prototypeId = ProcessorUtils.getPrototypeId(beanId);
        Object prototypeBean = applicationContext.getBean(prototypeId);
        return getBeanForId(prototypeBean.getClass(), beanId, descriptor);
    }

    @Override
    public <T> T getBeanForId(Class<T> clazz, String beanId, IStructureValueDescriptor<?> descriptor) throws Exception {
        log.debug("DefaultValueObjectProvider getting bean for id: '{}' and class: '{}'", beanId, clazz.getName());
        if (!StringUtils.hasLength(beanId)) {
            throw new IllegalArgumentException("Specified beanId is null or empty.");
        }

        String prototypeId = ProcessorUtils.getPrototypeId(beanId);
        T prototypeBean = (T)applicationContext.getBean(prototypeId);
        if (descriptor != null && descriptor.isResponsibleFor(prototypeBean)) {
            descriptor.initBeanParameters(prototypeBean, getRuntimeContext().getContextMergedBeanParameters(ProcessorUtils.getIdentifier(beanId)));
            return prototypeBean;
        }

        if (!prototypeBean.getClass().isAnnotationPresent(ValueObject.class)) {
            throw new IllegalArgumentException("Prototype '" + prototypeId + "' is not a @ValueObject. This provider can only handle ValueObjects.");
        }

        if (!clazz.isAssignableFrom(prototypeBean.getClass())) {
            throw new ClassCastException(String.format("Prototype bean '%s' of type %s cannot be cast to requested type %s",
                    prototypeId, prototypeBean.getClass().getName(), clazz.getName()));
        }

        // Es ist ein ValueObject, wir Ã¼bernehmen die Initialisierung.
        T beanInstance = (T) prototypeBean;
        initBean(beanInstance, beanId, getRuntimeContext());
        return beanInstance;
    }

    @Override
    public Object getBeanForId(String beanId) throws Exception {
        String prototypeId = ProcessorUtils.getPrototypeId(beanId);
        Object prototypeBean = applicationContext.getBean(prototypeId);
        return getBeanForId(prototypeBean.getClass(), beanId);
    }

    @Override
    public String[] getBeanIdentifiers(Object characteristic) {
        if (characteristic instanceof Class<?>)
            return applicationContext.getBeanNamesForType((Class<?>)characteristic);
        if (characteristic instanceof String) {
            try {
                return applicationContext.getBeanNamesForType(Class.forName(characteristic.toString()));
            } catch (ClassNotFoundException e) {
                log.error("{} can not find class for name {}",getIdentifier(), characteristic.toString());
            }
        }
        return new String[0];
    }

    @Override
    public String getBeanIdentifier(Object characteristic) {
        if (characteristic instanceof Class<?>) {
            String[] names = applicationContext.getBeanNamesForType((Class<?>)characteristic);
            if (names != null && names.length > 0) return names[0];
        }
        if (characteristic instanceof String) {
            try {
                String[] names =  applicationContext.getBeanNamesForType(Class.forName(characteristic.toString()));
                if (names != null && names.length > 0) return names[0];
            } catch (ClassNotFoundException e) {
                log.error("{} can not find class for name {}",getIdentifier(), characteristic.toString());
            }
        }
        return null;
    }

    @Override
    public <T> List<T> getBeansOfType(Class<T> clazz) {
        Map<String, T> beans = applicationContext.getBeansOfType(clazz);
        if (beans != null) return new ArrayList<>(beans.values());
        return null;
    }

    @Override
    public <T> T createIsolatedBean(ITaskContext taskContext) {
        return null;
    }

    /**
     * Initialisiert ein @ValueObject rekursiv mit den Parametern aus dem Kontext.
     *
     * @param bean Das zu initialisierende Bean-Objekt.
     * @param beanId Die vollstÃ¤ndige Bean-ID (prototype:instance@scope).
     * @param ctx Der Kontext, der die Parameter enthÃ¤lt.
     */
    public void initBean(Object bean, String beanId, IProcessorContext ctx) {
        if (bean == null || ctx == null) {
            return;
        }

        log.debug("Initializing ValueObject '{}' of type {}", beanId, bean.getClass().getName());

        String identifier = ProcessorUtils.getIdentifier(beanId);
        Map<String, Object> parameters = ctx.getContextMergedBeanParameters(identifier);

        if (parameters == null || parameters.isEmpty()) {
            log.debug("No parameters found for ValueObject '{}'. Initialization skipped.", identifier);
            return;
        }

        // Nutze die bewÃ¤hrte Utility-Methode, um die Parameter zu setzen.
        ProcessorUtils.initBeanParameters(bean, parameters, ctx);

        log.debug("Successfully initialized ValueObject '{}'.", beanId);
    }
}