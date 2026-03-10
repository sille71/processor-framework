package de.starima.pfw.base.processor.context.domain;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.api.IProcessorProvider;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.context.api.IRuntimeContextProviderProcessor;
import de.starima.pfw.base.processor.parameter.api.IBeanTypeMapProcessor;
import de.starima.pfw.base.processor.parameter.api.IParameterProviderProcessor;
import de.starima.pfw.base.util.MapUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Getter @Setter
public class DefaultProcessorContext implements IProcessorContext {
	private String name;
	//private String beanTypeMapProcessorBeanId = "defaultBeanTypeMapProcessor";
	private IProcessorContext parentContext;
	private Map<String, IProcessorContext> childContextMap;
	private Map<String, Map<String, Object>> beanParameterMap;
	private Map<String, String> beanIdTypeMap;
	private IRuntimeContextProviderProcessor contextProviderProcessor;
	private Map<String, IProcessor> beanIdProcessorMap;
	private IProcessor initializedProcessor;
	private Locale locale = Locale.ENGLISH;
	//may be a global, cluster or recon configuration
	//will be removed in future releases when the configuration is completely replaced with the beanParameterMap and beanTypeMap
	//private ReconConfiguration reconConfiguration;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public IProcessorContext getParentContext() {
		return parentContext;
	}

	public void setParentContext(IProcessorContext parentContext) {
		this.parentContext = parentContext;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj instanceof IProcessorContext) return ((IProcessorContext)obj).getName().equals(this.getName());
		return false;
	}

	public Map<String, Map<String, Object>> getBeanParameterMap() {
		if (this.beanParameterMap == null) {
			this.beanParameterMap = this.getContextProviderProcessor().getParameterProviderProcessor().getBeanParameterMap();
		}
		return beanParameterMap;
	}

	public Map<String, Map<String, Object>> refreshBeanParameterMap(Map<String, Map<String, Object>> beanParameterMap) {
		setBeanParameterMap(MapUtils.mergeBeanIdParameterMap(getBeanParameterMap(), beanParameterMap));
		return getBeanParameterMap();
	}

	public Map<String, Map<String, Object>> getContextMergedBeanParameterMap() {
		Map<String, Map<String, Object>> beanIdParameterMap = new HashMap<>();
		if (getParentContext() != null) {
			beanIdParameterMap = MapUtils.mergeBeanIdParameterMap(beanIdParameterMap,getParentContext().getContextMergedBeanParameterMap());
		}

		return MapUtils.mergeBeanIdParameterMap(beanIdParameterMap, getBeanParameterMap());
	}

	public void setBeanParameterMap(Map<String, Map<String, Object>> beanParameterMap) {
		this.beanParameterMap = beanParameterMap;
	}

	/**
	 * Die BeanTypeMap ist dann gefordert, wenn keine beanIds bekannt sind, sondern nur der Typ des gewÃ¼nschten Processors.
	 * So ist beispielsweise beim Deployment nicht klar, welche Konfiguration die gegebene Datei enthÃ¤lt
	 * (global Konfig, Cluster Konfig oder Recon Konfig). Es kann aber nach einem bean vom typ deployment in der Konfig gesucht werden.
	 * Aus der BeanTypMap erhÃ¤lt man dann den dort hinterlegten Deploymentprozessor. (globalCfgDeploymentProcessor,
	 * clusterCfgDeploymentProcessor, clusterCfgDeploymentProcessor)
	 * @return
	 */
	public Map<String, String> getBeanIdTypeMap() {
		if (beanIdTypeMap == null) {
			IBeanTypeMapProcessor beanTypeMapProcessor = this.getContextProviderProcessor() != null ? this.getContextProviderProcessor().getBeanTypeMapProcessor() : null;
			if (beanTypeMapProcessor != null) {
				beanIdTypeMap = beanTypeMapProcessor.getBeanIdTypeMap();
			} else {
				log.warn("No beanTypeMapProcessor defined in ctx {}", getName());
				beanIdTypeMap = new HashMap<>();
			}
		}
		return beanIdTypeMap;
	}

	public void setBeanIdTypeMap(Map<String, String> beanIdTypeMap) {
		this.beanIdTypeMap = beanIdTypeMap;
	}

	/**
	 * Liefert die vereinigte Bean Typ Map des aktuellen und der Eltern Kontexte. Dabei kÃ¶nnen Typdefinitionen
	 * im Elternkontext Ã¼berschrieben werden.
	 * @return
	 */
	public Map<String, String> getContextMergedBeanIdTypeMap() {
		Map<String, String> beanTypeMap = new HashMap<>();
		if (getParentContext() != null) {
			beanTypeMap = MapUtils.mergeMaps(new HashMap<>(),getParentContext().getContextMergedBeanIdTypeMap());
		}
		return MapUtils.mergeMaps(beanTypeMap, getBeanIdTypeMap());
	}

	/**
	 * Liefert die bean Id zu einem gegebenen Typ aus der Map in diesem Kontext.
	 * @param type
	 * @return
	 */
	public String getBeanIdForType(String type) {
		return this.getBeanIdTypeMap() != null ? this.getBeanIdTypeMap().get(type) : null;
	}

	public String getContextMergedBeanIdForType(String type) {
		Map<String, String> ctxMergedMap = getContextMergedBeanIdTypeMap();
		return ctxMergedMap != null ? ctxMergedMap.get(type) : null;
	}

	/**
	 * Liefert die Parameter zu einer gegebenen beanId aus der Map in diesem Kontext.
	 * @param beanId
	 * @return
	 */
	public Map<String, Object> getBeanParameters(String beanId) {
		return this.beanParameterMap != null ? this.beanParameterMap.get(beanId) : null;
	}

	public void addBeanParameters
            (String beanId, Map<String, Object> parameters) {
		if (this.beanParameterMap == null) this.beanParameterMap = new HashMap<>();
		this.beanParameterMap.put(beanId, parameters);
	}

	public void addBeanIdType(String type, String beanId) {
		getBeanIdTypeMap().put(type, beanId);
	}

	public void removeBeanParameters(String beanId) {
		if (this.beanParameterMap == null) this.beanParameterMap = new HashMap<>();
		this.beanParameterMap.remove(beanId);
	}

	/**
	 * Liefert die Parameter zu einer gegebenen beanId. Dabei werden die Parameter zu dieser beanId der Eltern Kontexte verschmolzen.
	 * Wurden Parameter im Eltern Kontext definiert, dann kÃ¶nnen diese im Kind Kontext Ã¼berschrieben werden.
	 * So kÃ¶nnen auch Parameter erreicht werden, die nicht im Kind Kontext definiert wurden.
	 * @param beanId
	 * @return
	 */
	public Map<String, Object> getContextMergedBeanParameters(String beanId) {
		Map<String, Object> parentMap = null;
		if (getParentContext() != null) {
			parentMap = getParentContext().getContextMergedBeanParameters(beanId);
		}
		return MapUtils.mergeMaps(parentMap, getBeanParameters(beanId));
	}

	public IRuntimeContextProviderProcessor getContextProviderProcessor() {
		return contextProviderProcessor != null ? contextProviderProcessor : getParentContext().getContextProviderProcessor();
	}

	public void setContextProviderProcessor(IRuntimeContextProviderProcessor contextProviderProcessor) {
		this.contextProviderProcessor = contextProviderProcessor;
	}

	public IParameterProviderProcessor getParameterProviderProcessor() {
		return this.getContextProviderProcessor() != null ? this.getContextProviderProcessor().getParameterProviderProcessor() : null;
	}



	public IProcessorProvider getProcessorProvider() {
		return this.getContextProviderProcessor() != null ? this.getContextProviderProcessor().getProcessorProvider() : null;
	}

	public Map<String, IProcessor> getBeanIdProcessorMap() {
		return beanIdProcessorMap;
	}

	public void setBeanIdProcessorMap(Map<String, IProcessor> beanIdProcessorMap) {
		this.beanIdProcessorMap = beanIdProcessorMap;
	}

	public void addProcessor(IProcessor processor) {
		if (isInitializerContext()) getFinalContext().addProcessor(processor);
		if (this.beanIdProcessorMap == null) this.beanIdProcessorMap = new HashMap<>();
		this.beanIdProcessorMap.put(processor.getFullBeanId(), processor);
	}

	public void addProcessorToParentContext(IProcessor processor) {
		if (isInitializerContext()) getFinalContext().addProcessorToParentContext(processor);
		if (getParentContext() != null) getParentContext().addProcessor(processor);
		else addProcessor(processor);
	}

	public void removeProcessor(IProcessor processor) {
		if (this.beanIdProcessorMap == null) this.beanIdProcessorMap = new HashMap<>();
		this.beanIdProcessorMap.remove(processor.getFullBeanId());
	}

	public IProcessor getProcessor(String beanId) {
		if (isInitializerContext()) return getFinalContext().getProcessor(beanId);
		if (this.beanIdProcessorMap != null) {
			IProcessor p = this.beanIdProcessorMap.get(beanId);
			if (p != null) {
				log.debug("found processor {} in context {}", beanId, this.getName());
				return p;
			}
			//wir suchen in den Kind Kontexten weiter
			/*
			if (this.getChildContextMap() != null) {
				for (Map.Entry<String, ReconContext> ctxEntry : this.getChildContextMap().entrySet()) {
					p = ctxEntry.getValue().getProcessor(ProcessorUtils.getIdentifier(beanId));
					if (p != null) return p;
				}
			}
			 */
		}
		log.debug("no processor {} registered in context {}", beanId, this.getName());
		return null;
	}

	public IProcessor getProcessorFromHierarchy(String beanId) {
		if (isInitializerContext()) return getFinalContext().getProcessorFromHierarchy(beanId);
		IProcessor p = getProcessor(beanId);
		if (p != null) return p;
		if (getParentContext() != null) return getParentContext().getProcessorFromHierarchy(beanId);
		return null;
	}

	public IProcessor findProcessor(String contextIdentifier, String processorIdentifier) {
		IProcessorContext context = findContext(contextIdentifier);
		if (context == null) return null;
		return context.getProcessor(processorIdentifier);
	}

	public void addReconContext(IProcessorContext ctx) {
		if (this.childContextMap == null) this.childContextMap = new HashMap<>();
		this.childContextMap.put(ctx.getName(), ctx);
		ctx.setParentContext(this);
	}

	public void removeReconContext(IProcessorContext ctx) {
		if (ctx == null) return;
		ctx.setParentContext(null);
		if (this.childContextMap == null) this.childContextMap = new HashMap<>();
		this.childContextMap.remove(ctx.getName());
	}

	public IProcessorContext getChildContext(String identifier) {
		if (this.childContextMap != null) {
			IProcessorContext ctx = this.childContextMap.get(identifier);
			if (ctx != null) return ctx;
			for (IProcessorContext context : this.childContextMap.values()) {
				ctx = context.getChildContext(identifier);
				if (ctx != null) return ctx;
			}
		}
		return null;
	}

	public IProcessorContext getRootContext() {
		return this.getParentContext() != null ? this.getParentContext().getRootContext() : this;
	}

	public IProcessorContext findContext(String identifier) {
		IProcessorContext root = this.getRootContext();
		return root.getChildContext(identifier);
	}

	@Override
	public boolean isInitializerContext() {
		return initializedProcessor != null;
	}

	@Override
	public boolean isFinalContext() {
		return !isInitializerContext();
	}

	public IProcessorContext getFinalContext() {
		if (isFinalContext()) return this;
		if (getParentContext() != null) return getParentContext().getFinalContext();
		return null;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public IProcessorContext cleanUpInitializerContext(IProcessorContext finalContext) {
		if (!this.isInitializerContext()) return this;

		//wir bauen den Initializer Kontext aus seinem parent zurÃ¼ck, falls vorhanden
		if (this.getParentContext() != null) {
			//alle Kind Kontexte des initializerContext werden unter den Parent des initializerContext gehangen
			if (this.childContextMap != null && childContextMap.values() != null) {
				this.childContextMap.values().forEach(child -> this.getParentContext().addReconContext(child));
			}
			this.getParentContext().removeReconContext(this);
		}
		return finalContext;
	}
}