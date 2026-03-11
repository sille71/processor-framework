package de.starima.pfw.base.processor.kernel;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.context.api.IRuntimeContextProviderProcessor;
import de.starima.pfw.base.processor.context.domain.DefaultTaskContext;
import de.starima.pfw.base.processor.kernel.api.IKernelBeanProvider;
import de.starima.pfw.base.processor.kernel.api.IRunLevelManager;
import de.starima.pfw.base.processor.kernel.domain.RunLevels;
import de.starima.pfw.base.util.ProcessorUtils;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * Der Kernel.
 *
 * <p>Erstellt den RootContext (Bootstrap) und delegiert alles Weitere
 * an den RunLevelManager. Kennt keine fachliche Logik, keine Services,
 * keine Requests.
 *
 * <p>Unix-Analogie: KernelProcessor = PID 1, startet systemd (RunLevelManager).
 * Ab RunLevel INCUBATION übernimmt der RunLevelManager alles.
 */
@Slf4j
@Getter
@Setter
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Processor(
        description = "Der Kernel. Erstellt den RootContext (Bootstrap) " +
                "und delegiert alles Weitere an den RunLevelManager. " +
                "Kennt keine fachliche Logik, keine Services, keine Requests.",
        categories = {"kernel"},
        tags = {"kernel", "bootstrap", "lifecycle"}
)
@Service
public class KernelProcessor extends AbstractProcessor {

    @ProcessorParameter(ignoreInitialization = true,
            description = "ContextProvider für den Kernel-Bootstrap")
    private IRuntimeContextProviderProcessor kernelContextProvider;

    @ProcessorParameter(ignoreInitialization = true,
            description = "BeanProvider für den Kernel-Bootstrap")
    private IKernelBeanProvider beanProvider;

    @ProcessorParameter(description = "Der RunLevelManager — organisiert " +
            "alle Systemzustände ab RunLevel INCUBATION")
    private IRunLevelManager runLevelManager;

    @ProcessorParameter(value = RunLevels.APPLICATION,
            description = "Das Ziel-RunLevel beim Start (Name als String). " +
                    "Per CLI: --pfw.target-runlevel=INCUBATION")
    private String targetRunLevel = RunLevels.APPLICATION;

    @Autowired
    public KernelProcessor(
            KernelRuntimeContextProviderProcessor contextProviderProcessor,
            KernelBeanProvider beanProvider) {
        this.kernelContextProvider = contextProviderProcessor;
        this.beanProvider = beanProvider;
    }

    @PostConstruct
    public void initProcessor() {
        try {
            // === RunLevel 0: BOOTSTRAP (hardcoded, kein Incubator) ===
            log.info("=== Kernel Bootstrap ===");
            this.runtimeContext = kernelContextProvider.createContext(null);
            ProcessorUtils.registerProcessorInScope(this, this.runtimeContext);

            // RunLevelManager aus BeanProvider holen (pragma: er ist Spring-Bean)
            if (runLevelManager == null) {
                runLevelManager = beanProvider.getBeanForId(IRunLevelManager.class, "defaultRunLevelManager");
            }

            // === RunLevel 1+: RunLevelManager übernimmt ===
            if (runLevelManager != null) {
                DefaultTaskContext taskCtx = new DefaultTaskContext();
                taskCtx.setRuntimeContext(this.runtimeContext);
                runLevelManager.advanceTo(targetRunLevel, taskCtx);
            } else {
                log.info("Kernel: kein RunLevelManager konfiguriert — bleibe bei RunLevel {}", RunLevels.BOOTSTRAP);
            }

            log.info("=== Kernel bereit. RunLevel: '{}' (rank={}) ===",
                    runLevelManager != null ? runLevelManager.getCurrentRunLevelName() : RunLevels.BOOTSTRAP,
                    runLevelManager != null ? runLevelManager.getCurrentRank() : RunLevels.RANK_BOOTSTRAP);

        } catch (Exception e) {
            log.error("Kernel-Bootstrap fehlgeschlagen!", e);
        }
    }
}