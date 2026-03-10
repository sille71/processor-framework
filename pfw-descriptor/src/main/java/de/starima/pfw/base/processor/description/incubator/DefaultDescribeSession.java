package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.incubator.api.IDescribeSession;
import de.starima.pfw.base.processor.description.incubator.domain.IDescribeTaskContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Standard-Implementierung von {@link IDescribeSession}.
 *
 * <p>Kapselt das Ergebnis einer describe-Operation:
 * <ul>
 *   <li>{@code root} — der Root-Descriptor (IDescriptorProcessor) des beschriebenen Objekts</li>
 *   <li>{@code extractionResult} — die serialisierte beanParameterMap (beanId → parameter-Map)</li>
 * </ul>
 *
 * <p>Das {@code extractionResult} entsteht durch den extract-Pfad der InstanceProviderChain
 * und enthält die vollständige Konfiguration des beschriebenen Objekts in serialisierter Form.
 * Es kann direkt als Input für {@code startConstruct()} verwendet werden.
 *
 * <p>{@code expand()} und {@code getContext()} sind Phase-4-Features und geben vorerst null zurück.
 */
@Slf4j
@Getter
@Processor(
        description = "Ergebnis einer startDescribe()-Operation. " +
                "Kapselt den Root-Descriptor und die serialisierte beanParameterMap.",
        categories = {"incubator", "session"},
        tags = {"describe", "session", "descriptor", "extract"}
)
public class DefaultDescribeSession extends AbstractProcessor implements IDescribeSession {

    private final IDescriptorProcessor root;
    private final Map<String, Map<String, Object>> extractionResult;

    public DefaultDescribeSession(IDescriptorProcessor root,
                                  Map<String, Map<String, Object>> extractionResult) {
        this.root = root;
        this.extractionResult = extractionResult;
    }

    @Override
    public IDescriptorProcessor getRoot() {
        return root;
    }

    @Override
    public IDescribeTaskContext getContext() {
        // Phase 4: Workspace und Cursor-Stages
        return null;
    }

    @Override
    public IDescriptorProcessor expand(String path, String policy) {
        // Phase 4: Lazy-Expansion des Descriptor-Graphs
        log.debug("expand('{}', '{}') — noch nicht implementiert (Phase 4)", path, policy);
        return null;
    }
}
