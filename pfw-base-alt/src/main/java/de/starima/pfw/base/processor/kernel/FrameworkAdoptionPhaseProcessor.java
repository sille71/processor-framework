package de.starima.pfw.base.processor.kernel;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.context.api.ITaskContext;
import de.starima.pfw.base.processor.kernel.api.IPhaseProcessor;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Kernel-Phase: Adoptiert alle Spring-bootstrappten Framework-Prozessoren
 * als vollwertige Mitglieder des Prozessorframeworks.
 *
 * <h3>Problem:</h3>
 * <p>Nach der Spring-Initialisierung (Phase 0) existieren Framework-Prozessoren
 * wie Chains, Resolver und ContextProvider als Spring-Beans — aber sie haben
 * keinen {@link IProcessorContext}. Sie sind "heimatlose" Prozessoren:
 * nicht im Kontext registriert, nicht über Descriptoren inspizierbar,
 * nicht über das UI konfigurierbar.
 *
 * <h3>Lösung:</h3>
 * <p>Diese Phase läuft direkt nach der RootContext-Erstellung (Phase 0.5 / 1)
 * und erledigt für jeden Spring-bootstrappten IProcessor:
 * <ol>
 *   <li>Setzt den {@code runtimeContext} auf den RootContext</li>
 *   <li>Registriert ihn im RootContext (via {@code addProcessor()})</li>
 *   <li>Registriert ihn im Scope (via {@code ProcessorUtils.registerProcessorInScope()})</li>
 * </ol>
 *
 * <p>Danach sind diese Prozessoren vollwertige Framework-Mitglieder:
 * <ul>
 *   <li>Über {@code context.getProcessor(identifier)} findbar</li>
 *   <li>Über Descriptoren inspizierbar ("vom Kernel aus den Aufbau inspizieren")</li>
 *   <li>Im UI als konfigurierbare Bausteine verwendbar</li>
 *   <li>Können später über die beanParameterMap umkonfiguriert werden</li>
 * </ul>
 *
 * <h3>Einordnung in die Kernel-Phasen:</h3>
 * <pre>
 *   Phase 0:   Spring Bootstrap (Konstruktor-Injection, @PostConstruct)
 *   Phase 0.5: RootContext-Erstellung (KernelProcessor.initProcessor)
 *   Phase 1:   ► DIESE PHASE: Framework-Adoption ◄
 *   Phase 2:   FrameworkIncubator-Erstellung (Descriptoren werden möglich)
 *   Phase 3:   Application-Phase (ServiceProcessor, fachliche Prozessoren)
 * </pre>
 */
@Slf4j
@Getter
@Setter
@Processor(description = "Adoptiert Spring-bootstrappte Prozessoren in den Framework-Kontext.")
public class FrameworkAdoptionPhaseProcessor extends AbstractProcessor implements IPhaseProcessor {

    /**
     * Alle Spring-registrierten IProcessor-Beans.
     * ObjectProvider liefert sie lazy — zum Zeitpunkt der Adoption sind alle da.
     */
    private final ObjectProvider<IProcessor> allProcessorBeans;

    @Autowired
    public FrameworkAdoptionPhaseProcessor(ObjectProvider<IProcessor> allProcessorBeans) {
        this.allProcessorBeans = allProcessorBeans;
    }

    @Override
    public String getPhaseName() {
        return "FrameworkAdoption";
    }

    @Override
    public Integer getRunLevel() {
        return 1; // Direkt nach RootContext-Erstellung (Phase 0)
    }

    @Override
    public List<IPhaseProcessor> getPhaseProcessors() {
        return Collections.emptyList(); // Keine Sub-Phasen
    }

    @Override
    public void initPhase(ITaskContext taskContext) {
        IProcessorContext rootContext = taskContext.getRuntimeContext();
        log.info("=== Framework-Adoption: Adoptiere Spring-Beans in RootContext '{}' ===",
                rootContext.getName());

        List<IProcessor> adopted = allProcessorBeans.orderedStream()
                .filter(this::shouldAdopt)
                .peek(processor -> adoptProcessor(processor, rootContext))
                .collect(Collectors.toList());

        log.info("=== Framework-Adoption abgeschlossen: {} Prozessoren adoptiert ===", adopted.size());

        if (log.isDebugEnabled()) {
            adopted.forEach(p -> log.debug("  Adoptiert: {} [{}] scope={}",
                    p.getFullBeanId(),
                    p.getClass().getSimpleName(),
                    p.getScope()));
        }
    }

    // -------------------------------------------------------------------------
    // Adoption-Logik
    // -------------------------------------------------------------------------

    /**
     * Prüft, ob ein Prozessor adoptiert werden soll.
     *
     * <p>Ausgeschlossen werden:
     * <ul>
     *   <li>Der KernelProcessor selbst (hat seinen eigenen Context)</li>
     *   <li>Prozessoren, die bereits einen RuntimeContext haben
     *       (z.B. zur Laufzeit erzeugte Instance-Scoped Prozessoren)</li>
     * </ul>
     */
    private boolean shouldAdopt(IProcessor processor) {
        // Kernel hat seinen eigenen Context — nicht anfassen
        if (processor instanceof KernelProcessor) {
            return false;
        }

        // Bereits initialisierte Prozessoren nicht überschreiben
        if (processor.getRuntimeContext() != null) {
            log.trace("Überspringe {} — hat bereits RuntimeContext '{}'",
                    processor.getFullBeanId(),
                    processor.getRuntimeContext().getName());
            return false;
        }

        // Nur Klassen mit @Processor-Annotation
        return ProcessorUtils.isConsideredProcessor(processor.getClass());
    }

    /**
     * Adoptiert einen einzelnen Prozessor in den RootContext.
     *
     * <p>Nach der Adoption:
     * <ul>
     *   <li>{@code processor.getRuntimeContext()} liefert den RootContext</li>
     *   <li>{@code rootContext.getProcessor(identifier)} findet den Prozessor</li>
     *   <li>Der Prozessor ist im Scope-System registriert</li>
     * </ul>
     */
    private void adoptProcessor(IProcessor processor, IProcessorContext rootContext) {
        // 1. RuntimeContext setzen
        processor.setRuntimeContext(rootContext);

        // 2. Im Kontext registrieren (findbar über identifier)
        rootContext.addProcessor(processor);

        // 3. Im Scope-System registrieren
        ProcessorUtils.registerProcessorInScope(processor, rootContext);

        log.debug("Adoptiert: {} → RootContext '{}'",
                processor.getFullBeanId(), rootContext.getName());
    }
}
