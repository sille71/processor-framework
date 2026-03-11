package de.starima.pfw.base.processor;

import java.util.*;

import de.starima.pfw.base.domain.ProcessorScope;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.IDescriptorConstructorContext;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.context.api.LoadStrategy;
import de.starima.pfw.base.processor.description.api.IProcessorDescriptor;
import de.starima.pfw.base.processor.context.api.IRuntimeContextProviderProcessor;
import de.starima.pfw.base.processor.parameter.api.IParameterChangeListener;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanNameAware;

@Slf4j
@Getter @Setter @SuperBuilder
@NoArgsConstructor
@Processor
public abstract class AbstractProcessor implements IProcessor, IParameterChangeListener, BeanNameAware {

    private String identifier;
	private String protoTypeIdentifier;
	private String fullBeanId;

	protected IProcessorContext runtimeContext;

	private ProcessorScope scope;

	/**
	 * Stellt die Beschreibung/Dokumentation für Prozessoren zur Verfügung.
	 */
	@ProcessorParameter(processorDescriptor = true, ignoreInitialization = true)
	protected IProcessorDescriptor processorDescriptor;

	/**
	 * Die Initialisierung des ContextProviders muss immer vor der Initialisierung der anderen Parameter erfolgen.
	 */
	@ProcessorParameter(contextProvider = true, ignoreInitialization = true)
	protected IRuntimeContextProviderProcessor contextProviderProcessor;

	@Override
	public void setRuntimeContext(IProcessorContext ctx) {
		this.runtimeContext = ctx;
	}

	public ProcessorScope getScope() {
		return scope;
	}

	public void setScope(ProcessorScope scope) {
		this.scope = scope;
	}

	@Override
	public String getFullBeanId() {
		if (fullBeanId == null) {
			fullBeanId = ProcessorUtils.createFullBeanId(this.protoTypeIdentifier, this.identifier, this.scope);
		}
		return fullBeanId;
	}

	@Override
	public String toString() {
		return getFullBeanId();
	}

	public Map<String,Map<String,Object>> initDefaultBeanParameterMap() {
		return ProcessorUtils.loadProcessorDefaults(this.getClass());
	}

	// =========================================================================
	// IProcessor Lifecycle
	// =========================================================================

	/**
	 * Wird vom ProcessorInstanceProvider aufgerufen, nachdem Context,
	 * Descriptor und Parameter bereits gesetzt sind.
	 */
	public void init(IProcessorContext ctx) throws Exception {
		this.runtimeContext = ctx;
		processorOnInit();
	}

	/**
	 * Hot-Reload der Konfiguration. Aktualisiert den Kontext und
	 * benachrichtigt den Prozessor via processorOnRefresh().
	 */
	public void refreshParameters(Map<String,Map<String, Object>> beanParameterMap) {
		if (this.runtimeContext != null) {
			this.runtimeContext.refreshBeanParameterMap(beanParameterMap);
			this.processorOnRefresh();
		}
	}

	/** Override-Hook: Wird nach der Initialisierung aufgerufen. */
	public void processorOnInit() {
	}

	/** Override-Hook: Wird nach einem Parameter-Refresh aufgerufen. */
	public void processorOnRefresh() {
	}

	/** Override-Hook: Wird vor dem Deregistrieren/Ersetzen aufgerufen. */
	public void processorOnDestroy() {
	}

	/**
	 * Hook: Parameterinitialisierung aus der beanParameterMap.
	 * Wird vom ProcessorInstanceProvider aufgerufen.
	 * Kann in Subklassen überschrieben werden um zusätzliche Objekte zu initialisieren.
	 */
	protected void initParameters(Map<String, Object> parameters) {
		if (this.processorDescriptor != null) {
			this.processorDescriptor.initBeanParameters(this, parameters);
		} else {
			ProcessorUtils.initBeanParameters(this, parameters, this.getRuntimeContext());
		}
	}

	@Override
	public void setBeanName(String name) {
		this.identifier = name;
		this.protoTypeIdentifier = name;
	}

	// =========================================================================
	// Descriptor Generation
	// =========================================================================

	/**
	 * Factory method — override in pfw-runtime module to return a concrete IDescriptorConstructorContext instance.
	 */
	protected IDescriptorConstructorContext newDescriptorConstructorContext() {
		return null;
	}

	/**
	 * Factory method — override in pfw-runtime module to return a concrete ITransformationContext instance.
	 */
	protected ITransformationContext newTransformationContext() {
		return null;
	}

	@Override
	public IProcessorDescriptor generatePrototypeProcessorDescriptor() {
		IDescriptorConstructorContext context = newDescriptorConstructorContext();
		if (context == null) return null;
		context.setSourceType(this.getClass());
		context.setRuntimeContext(getRuntimeContext());
		context.setParentProcessor(Optional.of(this));
		Map<String, Map<String, Object>> bluePrint = new HashMap<>();
		String fullBeanId = ProcessorUtils.generateDescriptorBlueprint(context, bluePrint);
		return this.createProcessor(IProcessorDescriptor.class, fullBeanId, null, Collections.singletonList(bluePrint));
	}

	@Override
	public IProcessorDescriptor generateProcessorDescriptorInstance(LoadStrategy loadStrategy) {
		IDescriptorConstructorContext context = newDescriptorConstructorContext();
		if (context == null) return null;
		if (loadStrategy != null) context.setLoadStrategy(loadStrategy);
		context.setSourceObject(this);
		context.setRuntimeContext(getRuntimeContext());
		context.setParentProcessor(Optional.of(this));
		Map<String, Map<String, Object>> instanceMap = new HashMap<>();
		String fullBeanId = ProcessorUtils.generateDescriptorInstance(context, instanceMap);
		return this.createProcessor(IProcessorDescriptor.class, fullBeanId, null, Collections.singletonList(instanceMap));
	}

	// =========================================================================
	// Parameter Extraction
	// =========================================================================

	/**
	 * Liefert die tatsächlich gesetzten Parameter dieses Prozessors.
	 */
	public Map<String, Object> extractEffectiveProcessorParameters() {
		if (processorDescriptor != null) return processorDescriptor.extractEffectiveParameters(this);
		ITransformationContext transformationContext = newTransformationContext();
		if (transformationContext == null) return new HashMap<>();
		transformationContext.setObjectToResolve(this);
		transformationContext.setRuntimeContext(getRuntimeContext());
		return ProcessorUtils.extractEffectiveParameters(transformationContext);
	}

	public Map<String,Map<String, Object>> extractEffectiveProcessorParameterMap() {
		IProcessorDescriptor instanceDescriptor = this.processorDescriptor;
		instanceDescriptor = instanceDescriptor != null ? instanceDescriptor : this.generateProcessorDescriptorInstance(LoadStrategy.DEEP);
		if (instanceDescriptor == null) {
			log.error("Konnte keinen Instanz-Deskriptor für {} erzeugen. Extraktion nicht möglich.", this.getFullBeanId());
			return new HashMap<>();
		}

		ITransformationContext startContext = newTransformationContext();
		if (startContext == null) return new HashMap<>();
		startContext.setObjectToResolve(this);
		startContext.setRuntimeContext(getRuntimeContext());
		startContext.setLoadStrategy(LoadStrategy.DEEP);

		Map<String, Map<String, Object>> beanParameterMap = new HashMap<>();
		Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());

		instanceDescriptor.extractEffectiveParameterMap(startContext, beanParameterMap, visited);

		return beanParameterMap;
	}

	public IProcessorContext getRuntimeContext() {
		return runtimeContext;
	}

	// =========================================================================
	// Processor Creation (delegate to Incubator in future)
	// =========================================================================

	@Override
	public <T extends IProcessor> T createProcessor(Class<T> clazz, String processorIdentifier, String processorType,
			IProcessor parentProcessor, List<Map<String,Map<String, Object>>> beanParameterMaps) {
		IRuntimeContextProviderProcessor cp = contextProviderProcessor != null ? contextProviderProcessor :
				(runtimeContext != null ? runtimeContext.getContextProviderProcessor() : null);
		if (cp == null) {
			log.warn("{}: createProcessor — kein ContextProvider verfügbar", getFullBeanId());
			return null;
		}
		IProcessorContext newCtx = cp.createContext(this.runtimeContext, beanParameterMaps);
		if (processorIdentifier != null) {
			log.info("Try to create processor for identifier {}", processorIdentifier);
			return newCtx.getContextProviderProcessor().getProcessorProvider().getProcessorForBeanId(clazz, processorIdentifier, newCtx, parentProcessor);
		}
		if (processorType == null) processorType = "requestProcessor";
		log.info("Try to create processor for type {}", processorType);
		return newCtx.getContextProviderProcessor().getProcessorProvider().getProcessorForType(clazz, processorType, newCtx, parentProcessor);
	}

	@Override
	public <T extends IProcessor> T createProcessor(Class<T> clazz, String processorIdentifier, String processorType,
			List<Map<String,Map<String, Object>>> beanParameterMaps) {
		return createProcessor(clazz, processorIdentifier, processorType, this, beanParameterMaps);
	}

	// =========================================================================
	// IParameterChangeListener
	// =========================================================================

	@Override
	public void parameterChanged(String parameterName, Object value, String identifier) {
		log.debug("{}: parameterChanged {}={}", getIdentifier(), parameterName, value);
		processorOnDestroy();
	}

	@Override
	public void parametersChanged(Map<String, Object> parameters, String identifier) {
		log.debug("{}: parametersChanged for {}", getIdentifier(), identifier);
		processorOnDestroy();
	}

	@Override
	public void parametersChanged() {
		log.debug("{}: parametersChanged", getIdentifier());
		processorOnDestroy();
	}
}