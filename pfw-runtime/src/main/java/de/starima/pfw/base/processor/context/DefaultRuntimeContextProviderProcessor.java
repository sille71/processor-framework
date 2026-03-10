package de.starima.pfw.base.processor.context;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.api.IProcessorProvider;
import de.starima.pfw.base.processor.context.api.IContextCreationContext;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.context.api.IRuntimeContextProviderProcessor;
import de.starima.pfw.base.processor.context.domain.DefaultProcessorContext;
import de.starima.pfw.base.processor.parameter.api.IBeanTypeMapProcessor;
import de.starima.pfw.base.processor.parameter.api.IParameterProviderProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Standard-Implementierung: Erzeugt einen neuen {@link IProcessorContext}.
 *
 * <p><b>Reine Creation — keine Resolution.</b> Dieser Prozessor erstellt Kontexte,
 * er entscheidet nicht, ob er zuständig ist. Die Zuständigkeitsfindung
 * übernimmt der {@link de.starima.pfw.base.processor.context.api.IContextProviderResolver}.
 *
 * <p>Der neue Context wird als Kind des Parent-Kontextes angelegt und optional
 * mit einer eigenen beanParameterMap ausgestattet (via {@link IParameterProviderProcessor}).
 */
@Getter @Setter
@Slf4j
@Processor(description = "Erstellt einen neuen ProcessorContext als Kind des Parent-Kontextes.")
public class DefaultRuntimeContextProviderProcessor extends AbstractProcessor implements IRuntimeContextProviderProcessor {

    @ProcessorParameter(description = "Name des zu erstellenden Kontextes. Wird keiner angegeben, so wird der Identifier dieses Prozessors verwendet.")
    private String name;

    @ProcessorParameter
    private IParameterProviderProcessor parameterProviderProcessor;

    @ProcessorParameter(value = "defaultProcessorProvider")
    private IProcessorProvider processorProvider;

    @ProcessorParameter(value = "defaultBeanTypeMapProcessor")
    private IBeanTypeMapProcessor beanTypeMapProcessor;

    @ProcessorParameter(value = "true")
    private boolean useDefaultBeanParameterMap = true;

    // -------------------------------------------------------------------------
    // Context-Name
    // -------------------------------------------------------------------------

    private String getContextName() {
        return (this.name != null ? this.name : getIdentifier()) + "-" + UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // Neue primäre Methode: createContext(IContextCreationContext)
    // -------------------------------------------------------------------------

    @Override
    public IProcessorContext createContext(IContextCreationContext creationContext) {
        IProcessorContext parentCtx = creationContext != null
                ? creationContext.getRuntimeContext()
                : null;

        // 1. Neuen Context als Kind des Parent-Kontextes erzeugen
        IProcessorContext newCtx = createNewContext(parentCtx);

        // 2. Falls ein ParameterProvider konfiguriert ist, dessen beanParameterMap übernehmen
        if (this.parameterProviderProcessor != null) {
            newCtx.setBeanParameterMap(this.parameterProviderProcessor.getBeanParameterMap());
            log.debug("{}: beanParameterMap vom ParameterProvider '{}' übernommen",
                    getIdentifier(), this.parameterProviderProcessor.getIdentifier());
        }

        // 3. Falls ein BeanTypeMapProcessor konfiguriert ist, die beanIdTypeMap setzen
        if (this.beanTypeMapProcessor != null && newCtx.getBeanParameterMap() != null) {
            newCtx.setBeanIdTypeMap(
                    this.beanTypeMapProcessor.getBeanIdTypeMapFromParameterMap(newCtx.getBeanParameterMap()));
            log.debug("{}: beanIdTypeMap vom BeanTypeMapProcessor '{}' gesetzt",
                    getIdentifier(), this.beanTypeMapProcessor.getIdentifier());
        }

        // 4. Default-beanParameterMap einmischen, falls gewünscht
        if (this.useDefaultBeanParameterMap && creationContext != null) {
            Map<String, Map<String, Object>> defaults = getDefaultsFromCreationContext(creationContext);
            if (defaults != null && !defaults.isEmpty()) {
                mergeDefaults(newCtx, defaults);
            }
        }

        return newCtx;
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden
    // -------------------------------------------------------------------------

    protected IProcessorContext createNewContext(IProcessorContext parentCtx) {
        DefaultProcessorContext newCtx = new DefaultProcessorContext();
        newCtx.setName(getContextName());
        if (parentCtx != null) {
            parentCtx.addReconContext(newCtx);
        }
        newCtx.setContextProviderProcessor(this);
        return newCtx;
    }

    /**
     * Versucht, Default-Parameter aus dem CreationContext zu laden.
     * Falls der zu erstellende Prozessor eine Default-beanParameterMap hat,
     * wird diese zurückgegeben.
     */
    private Map<String, Map<String, Object>> getDefaultsFromCreationContext(IContextCreationContext context) {
        // Die eigene beanParameterMap des CreationContext kann Defaults enthalten
        return context.getOwnBeanParameterMap();
    }

    /**
     * Mischt Default-Parameter in den neuen Context ein.
     */
    private void mergeDefaults(IProcessorContext ctx, Map<String, Map<String, Object>> defaults) {
        Map<String, Map<String, Object>> existing = ctx.getBeanParameterMap();
        if (existing == null || existing.isEmpty()) {
            ctx.setBeanParameterMap(defaults);
        } else {
            // Defaults nur dort eintragen, wo noch nichts konfiguriert ist
            defaults.forEach((beanId, params) -> {
                if (!existing.containsKey(beanId)) {
                    ctx.addBeanParameters(beanId, params);
                }
            });
        }
    }

    // -------------------------------------------------------------------------
    // Deprecated Methoden — Abwärtskompatibilität während Migration
    // -------------------------------------------------------------------------

    @Override
    public IParameterProviderProcessor getParameterProviderProcessor() {
        return this.parameterProviderProcessor;
    }

    @Override
    public IProcessorProvider getProcessorProvider() {
        return this.processorProvider;
    }

    @Override
    public IBeanTypeMapProcessor getBeanTypeMapProcessor() {
        return this.beanTypeMapProcessor;
    }

    /**
     * @deprecated Verwende {@link #createContext(IContextCreationContext)} stattdessen.
     */
    @Deprecated
    @Override
    public IProcessorContext createContext(IProcessorContext parentCtx, List<Map<String, Map<String, Object>>> parameterMaps) {
        if (parameterMaps == null || parameterMaps.isEmpty()) {
            return parentCtx != null ? parentCtx : createNewContext(null);
        }
        IProcessorContext newCtx = parentCtx;
        for (Map<String, Map<String, Object>> pm : parameterMaps) {
            newCtx = createContext(newCtx, pm);
        }
        return newCtx;
    }

    /**
     * @deprecated Verwende {@link #createContext(IContextCreationContext)} stattdessen.
     */
    @Deprecated
    @Override
    public IProcessorContext createContext(IProcessorContext parentCtx, Map<String, Map<String, Object>> parameterMap) {
        if (parameterMap == null || parameterMap.isEmpty()) {
            return parentCtx != null ? parentCtx : createNewContext(null);
        }
        IProcessorContext newCtx = createNewContext(parentCtx);
        newCtx.setBeanParameterMap(parameterMap);
        if (this.getBeanTypeMapProcessor() != null) {
            newCtx.setBeanIdTypeMap(this.getBeanTypeMapProcessor().getBeanIdTypeMapFromParameterMap(parameterMap));
        }
        return newCtx;
    }

    /**
     * @deprecated Verwende {@link #createContext(IContextCreationContext)} stattdessen.
     */
    @Deprecated
    @Override
    public IProcessorContext createInitialzerContext(IProcessor processor, IProcessorContext parentCtx) {
        DefaultProcessorContext iCtx = new DefaultProcessorContext();
        iCtx.setName("init-" + getContextName());
        iCtx.setInitializedProcessor(processor);
        if (parentCtx != null) parentCtx.addReconContext(iCtx);
        iCtx.setContextProviderProcessor(this);
        return iCtx;
    }
}