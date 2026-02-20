package de.starima.pfw.base.processor.kernel;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.kernel.api.IKernelBeanProvider;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

@Slf4j
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Processor
public class KernelBeanProvider extends AbstractProcessor implements IKernelBeanProvider, ApplicationContextAware {
    private ApplicationContext applicationContext;

    /**
     * Holt die beans aus dem Spring Application Kontext.
     * Hier kann auch eine andere Registry benutzt werden.
     * @param beanId
     * @return
     */
    @Override
    public Object getBeanForId(String beanId) {
        log.debug("getBeanForId({})", beanId);
        try {
            if (beanId != null && !beanId.trim().isEmpty()) {
                beanId = beanId.trim();
                return applicationContext.getBean(ProcessorUtils.getIdentifier(beanId));
            } else {
                log.warn("Specified beanId is null or empty.");
            }
        } catch (Exception e) {
            String msg = "Failed: getBeanForId(" + beanId + ") Msg: " + e.toString();
            log.warn(msg);
        }
        return null;
    }

    public <T> T getBeanForId(Class<T> clazz, String beanId) {
        log.debug("getBeanForId({}, {})", clazz.getName(),  beanId);
        try {
            if (beanId != null && !beanId.trim().isEmpty()) {
                beanId = beanId.trim();
                String prototypeId = ProcessorUtils.getPrototypeId(beanId);
                return (T)applicationContext.getBean(ProcessorUtils.getIdentifier(prototypeId));
            } else {
                log.warn("Specified beanId is null or empty.");
            }
        } catch (Exception e) {
            String msg = "Failed: getBeanForId(" + beanId + ") Msg: " + e.toString();
            log.warn(msg);
        }
        return null;
    }
}