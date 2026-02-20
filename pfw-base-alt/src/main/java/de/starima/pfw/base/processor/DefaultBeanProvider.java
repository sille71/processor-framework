package de.starima.pfw.base.processor;

import de.dzbank.components.utils.log.LogOutputHelper;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.api.IBeanProvider;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.context.api.ITaskContext;
import de.starima.pfw.base.processor.description.BeanWrapperProcessor;
import de.starima.pfw.base.processor.description.api.IStructureValueDescriptor;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.type.LogicalType.Collection;

@Slf4j
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Processor
public class DefaultBeanProvider extends AbstractProcessor implements IBeanProvider, ApplicationContextAware {
    private ApplicationContext applicationContext;
    private BeanWrapperProcessor beanWrapperProcessor;

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultBeanProvider(BeanWrapperProcessor beanWrapperProcessor) {
        this.beanWrapperProcessor = beanWrapperProcessor;
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

    /**
     * Holt die beans aus dem Spring Application Kontext.
     * Hier kann auch eine andere Registry benutzt werden.
     * @param beanId
     * @return
     * @throws Exception
     */
    @Override
    public Object getBeanForId(String beanId) throws Exception {
        log.debug("getBeanForId({})", beanId);
        try {
            if (beanId != null && !beanId.trim().isEmpty()) {
                beanId = beanId.trim();
                return applicationContext.getBean(ProcessorUtils.getIdentifier(beanId));
            } else {
                throw new Exception("Specified beanId is null or empty.");
            }
        } catch (Exception e) {
            String msg = "Failed: getBeanForId(" + beanId + ") Msg: " + e.toString();
            log.warn(msg);
            throw new Exception(msg);
        }
    }

    public <T> T getBeanForId(Class<T> clazz, String beanId) throws Exception {
        log.debug("getBeanForId({}, {})", clazz.getName(),  beanId);
        try {
            if (beanId != null && !beanId.trim().isEmpty()) {
                beanId = beanId.trim();
                String prototypeId = ProcessorUtils.getPrototypeId(beanId);
                return (T)applicationContext.getBean(ProcessorUtils.getIdentifier(prototypeId));
            } else {
                throw new Exception("Specified beanId is null or empty.");
            }
        } catch (Exception e) {
            String msg = "Failed: getBeanForId(" + beanId + ") Msg: " + e.toString();
            log.warn(msg);
            throw new Exception(msg);
        }
    }

    @Override
    public Object getBeanForId(String beanId, IStructureValueDescriptor<?> descriptor) throws Exception {
        log.debug("getBeanForId({}) with descriptor", beanId);
        try {
            if (beanId != null && !beanId.trim().isEmpty()) {
                beanId = beanId.trim();
                String prototypeId = ProcessorUtils.getPrototypeId(beanId);
                Object bean = applicationContext.getBean(prototypeId);
                if (descriptor != null && descriptor.isResponsibleFor(bean)) descriptor.initBeanParameters(bean, getRuntimeContext().getContextMergedBeanParameters(ProcessorUtils.getIdentifier(beanId)));
                return bean;
            } else {
                throw new Exception("Specified beanId is null or empty.");
            }
        } catch (Exception e) {
            String msg = "Failed: getBeanForId(" + beanId + ") Msg: " + e.toString();
            log.warn(msg);
            throw new Exception(msg);
        }
    }

    @Override
    public <T> T getBeanForId(Class<T> clazz, String beanId, IStructureValueDescriptor<?> descriptor) throws Exception {
        log.debug("getBeanForId({}, {})", clazz.getName(),  beanId);
        try {
            if (beanId != null && !beanId.trim().isEmpty()) {
                beanId = beanId.trim();
                String prototypeId = ProcessorUtils.getPrototypeId(beanId);
                T bean = (T)applicationContext.getBean(ProcessorUtils.getIdentifier(prototypeId));
                if (descriptor != null && descriptor.isResponsibleFor(bean)) descriptor.initBeanParameters(bean, getRuntimeContext().getContextMergedBeanParameters(ProcessorUtils.getIdentifier(beanId)));
                return bean;
            } else {
                throw new Exception("Specified beanId is null or empty.");
            }
        } catch (Exception e) {
            String msg = "Failed: getBeanForId(" + beanId + ") Msg: " + e.toString();
            log.warn(msg);
            throw new Exception(msg);
        }
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

    public Object getBeanForIdWithType(String beanid, String type, IProcessorContext ctx) {
        log.debug("getBeanForIdWithType({}, {})", beanid, type);

        try {
            // Note: the beanId may consist of the prototype bean name and the
            // real bean identifier separated by colon
            String identifier = ProcessorUtils.getIdentifier(beanid);
            String prototypeId = ProcessorUtils.getPrototypeId(beanid);
            ProcessorScope scope = ProcessorUtils.getProcessorScope(beanid);
            /**
            IReconProcessor processor = ProcessorUtils.getProcessorFromScope(beanid, ctx);

            if (processor != null) {
                log.info("Found processor {} in scope {} and context {}", processor.getFullBeanId(), processor.getScope().name(), ctx.getName());
                return processor;
            }
             */
            Object bean = null;
            try {
                bean = getBeanForId(prototypeId);
            } catch (Exception e) {
                log.debug("{} is probably not a prototyp id!", prototypeId);
            }

            // may be the beanid is just the identifier without the prototypeId or an processor type
            // in this cases we ask the processor type map
            if (bean == null && prototypeId.equals(identifier)) {
                log.debug("{} may be the identifier without the prototypeId or an processor type! In this cases we ask the processor type map",
                        prototypeId);
                bean = getBeanForType(prototypeId, ctx);
            } else if (bean != null) {
                //init bean
                BeanWrapperProcessor beanWrapperProcessor = new BeanWrapperProcessor();
                beanWrapperProcessor.setBean(bean);
                beanWrapperProcessor.setIdentifier(identifier);
                beanWrapperProcessor.setProtoTypeIdentifier(prototypeId);
                beanWrapperProcessor.setScope(scope);
                beanWrapperProcessor.init(ctx);
            } else {
                log.debug("Failed: getBeanForIdWithType({}, {}) No processor found!", beanid, ctx);
            }
            return beanWrapperProcessor.getBean();
        } catch (Exception e) {
            log.warn("Failed: getProcessorForBeanIdWithType({}, {}): Msg {}", beanid, ctx, e);
        }

        return null;
    }

    public Object getBeanForType(String beanType, IProcessorContext ctx) {
        log.trace("getProcessorForType({}, {})", beanType, ctx);

        try {
            String beanId = ctx.getContextMergedBeanIdForType(beanType);

            if (beanId != null) {
                Object bean = getBeanForIdWithType(beanId, null, ctx);
                if (bean != null) {
                    log.debug("Success: getBeanForType({}, {}): Created  bean [id, class] = [{} : {}]",
                            beanType, ctx, beanId, bean.getClass().getCanonicalName());
                } else {
                    log.trace("Failed: getProcessorForType({}, {}) No processor found!", beanType, ctx);
                }
                return bean;
            } else {
                log.trace("Failed: getProcessorForType({}, {}) No beanId defined!", beanType, ctx);
            }

        } catch (Exception e) {
            log.error("Failed: getProcessorForType({}, {}): Msg {}", beanType, ctx, e.toString());
        }

        return null;
    }

    protected Map<String, Object> getParametersForIdentifier(String identifier, IProcessorContext ctx) {
        if (ctx == null) return null;
        return ctx.getContextMergedBeanParameters(identifier);
    }
}