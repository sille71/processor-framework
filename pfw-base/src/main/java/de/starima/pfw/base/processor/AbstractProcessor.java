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
import de.starima.pfw.base.util.LogOutputHelper;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.util.Assert;

@Slf4j
@Getter @Setter @SuperBuilder
@NoArgsConstructor
@Processor
public abstract class AbstractProcessor implements IProcessor, IParameterChangeListener, BeanNameAware {

    private String identifier;
	private String protoTypeIdentifier;
	private String fullBeanId;

	protected IProcessorContext runtimeContext;

	//private IParameterFunctionProcessor creatorParameterFunctionProcessor;

	//private IProcessor parentProcessor;

	private ProcessorScope scope;

	/**
	 * Stellt die Beschreibung/Dokumentation fÃ¼r Prozessoren zur VerfÃ¼gung. Diese Beschreibung dient als Ausgangspunkt fÃ¼r:
	 * 1. die Processor Initialisierung
	 * 2. die Processor Dokumentation
	 * 3. Bauplan im ReconLight
	 * Hier handelt es sich um eine Erweiterung der Beschreibung mittels @Processor, da die konkrete Beschreibung
	 * vom jeweiligen Einsatz abhÃ¤ngen kann.
	 * Der Descriptor ist ebenfalls ein Parameter und erhÃ¤lt einen speziellen ParameterDescriptor, so dass er von der Parameterinitialisierung ausgeschlossen wird, da er explizit initialisiert wird!
	 * Er sollte nach dem ContextProvider initialisiert werden, da der ContextProvider alle relevanten Parameter bereitstellt. Allerdings sollte er auch den
	 * ContextProvider beschreiben?!
	 * TODO: diese Beschreibung als Parameterbeschreibung hinterlegen?
	 */
	@ProcessorParameter(processorDescriptor = true, ignoreInitialization = true)
	protected IProcessorDescriptor processorDescriptor;

	/**
	 * Die Initialisierung des ContextProviders muss immer vor der Initialisierung der anderen Parameter erfolgen. Er wird daher bereits in der initContextProvider(ctx) Methode
	 * initialisiert und in der initParameters Methode ignoriert!
	 * Der Parameter hier ist doppelt gemoppelt, da jeder ContextProvider wÃ¤hrend der Erzeugung des Kontextes sich an diesem Kontext registriert und somit jedem Prozessor zur VerfÃ¼gung steht.
	 * Die Annotation und damit der Parameter sind aber dennoch wichtig, damit der ProcessorDescriptor diesen auch als Parameter ausweisen kann!
	 * Er sollte vor dem IProcessorDescriptor initialisiert werden, da der ContextProvider alle relevanten Parameter bereitstellt.
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

	public Map<String,Map<String,Object>>  initDefaultBeanParameterMap() {
		return ProcessorUtils.loadProcessorDefaults(this.getClass());
	}

	/**
	 * Die aktuelle init Methode des AbstractReconProcessor s kommt ohne ProcessorDescriptor aus! Diese ist fÃ¼r alle Prozessoren auf den Descriptor umzustellen
	 * auÃŸer denjenigen Prozessoren, die keine Descriptoren haben dÃ¼rfen, um Rekursionen zu vermeiden. HauptsÃ¤chlich sind dies Prozessoren,
	 * die in Descriptoren selbst verwendet werden.
	 */
	//region IProcessor Implementation
	//----------------------------------------------------------------------------------------------------------------------------
	public void init(IProcessorContext ctx) throws Exception {
		//wird vom ProcessorInstanceProvider übernommen
	}

	public void refreshParameters(Map<String,Map<String, Object>> beanParameterMap) {
		if (this.runtimeContext != null) {
			log.debug("{}: refresh context with parameters {}", getIdentifier(), LogOutputHelper.toLogString(beanParameterMap));
			this.runtimeContext.refreshBeanParameterMap(beanParameterMap);
			IProcessorContext finalContext =  this.runtimeContext != null ? this.runtimeContext.getFinalContext() : null;
			Assert.notNull(getUsedContextProviderProcessor(finalContext), "No context provider available!");
			this.runtimeContext = getUsedContextProviderProcessor(finalContext).createInitialzerContext(this, this.runtimeContext);
			initProcessorDescriptor(this.runtimeContext);
			//an dieser Stelle sind die Parameter schon durch einen zusÃ¤tzlichen ParameterProvider angereichert.
			//Das wÃ¤re der Fall, wenn fÃ¼r diesen Prozessor ein ContextProvider mit einem ParameterProvider konfiguriert wurde (also initContextProvider() einen neuen Kontext liefert).
			initParameters(getParameters(this.runtimeContext));
			this.runtimeContext = this.runtimeContext.cleanUpInitializerContext(finalContext);

			this.processorOnRefresh();

			if (this.getClass().isAnnotationPresent(Processor.class))
				log.debug("refreshed context {} with parameters {}",
						getIdentifier(), LogOutputHelper.toLogString(getParameters(this.runtimeContext)));
		}
	}

	public void processorOnInit() {

	}

	public void processorOnRefresh() {

	}

	@Override
	public void setBeanName(String name) {
		this.identifier = name;
		this.protoTypeIdentifier = name;
	}

	/**
	 * Erzeugung des Prozessordescriptors - Ablauf der initProcessorDescriptor Methode
	 */
	protected void initProcessorDescriptor(IProcessorContext ctx) {
		log.info("{}: init processor descriptor ", getFullBeanId());

		Map<String, Object> ctxParameters = getParameters(ctx);

		if (ctxParameters != null) {
			Object processorDescriptorParam = ctxParameters.get("processorDescriptor") != null ? ctxParameters.get("processorDescriptor") : ctxParameters.get("processorDescriptorIdentifier");
			if (processorDescriptorParam != null) {
				if (ctx.getProcessorProvider().isProcessorCreationPossible(processorDescriptorParam.toString(), ctx, this)) {
					log.info("{}: try to init processor descriptor from parameter {}", getFullBeanId(), processorDescriptorParam);
					this.processorDescriptor = ctx.getProcessorProvider().getProcessorForBeanId(IProcessorDescriptor.class, processorDescriptorParam.toString(), ctx, this);
					if (this.processorDescriptor != null)
						this.processorDescriptor.setPrototypeValueDescriptor(this.generatePrototypeProcessorDescriptor());
				}
			}
		}
	}


	protected Map<String, Object> getParameters(IProcessorContext ctx) {
		if (ctx == null) return null;
		return ctx.getContextMergedBeanParameters(getIdentifier());
	}

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
		String fullBeanId = ProcessorUtils.generateDescriptorBlueprint(context,bluePrint);
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
		Map<String, Map<String, Object>> intanceMap = new HashMap<>();
		String fullBeanId = ProcessorUtils.generateDescriptorInstance(context,intanceMap);
		return this.createProcessor(IProcessorDescriptor.class, fullBeanId, null, Collections.singletonList(intanceMap));
	}

	protected void initParameters(Map<String, Object> parameters) {
		if (this.processorDescriptor != null) {
			log.trace("Use descriptor {} to init parameters for processor {}", this.processorDescriptor.getFullBeanId(), this.getFullBeanId());
			this.processorDescriptor.initBeanParameters(this, parameters);
		} else {
			ProcessorUtils.initBeanParameters(this, parameters, this.getRuntimeContext());
		}
	}

	/**
	 * Liefert die tatsÃ¤chlich gesetzten Parameter dieses Prozessors. Die Parameterwerte werden dabei wieder zurÃ¼cktransformiert, so dass sie
	 * jederzeit als json reprÃ¤sentiert werden kÃ¶nnen.
	 * @return
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
			log.error("Konnte keinen Instanz-Deskriptor fÃ¼r {} erzeugen. Extraktion nicht mÃ¶glich.", this.getFullBeanId());
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

	//TODO: ist zu Ã¼berarbeiten
	protected void resetProcessor() {
		try {
			if (this.runtimeContext.getParentContext() != null) {
				this.runtimeContext.getParentContext().removeReconContext(this.runtimeContext);
				init(this.runtimeContext.getParentContext());
			} else {
				init(this.runtimeContext);
			}
		} catch (Exception e) {
			log.error("Can not reset processor {}, Msg: {}", getIdentifier(), e.toString());
		}
	}
	//----------------------------------------------------------------------------------------------------------------------------
	//endregion IProcessor Implementation

	//region IParameterChangeListener Implementation
	//----------------------------------------------------------------------------------------------------------------------------

	/**
	 * Wir verwenden hier eine einfache Implementierung des IParameterChangelistener. Diese setzt voraus, das
	 * der ParameterProvider, der das Event triggert auch zu diesem Prozessor gehÃ¶rt.
	 * @param parameterName
	 * @param value
	 * @param identifier - Prozessoridentifier
	 */
	@Override
	public void parameterChanged(String parameterName, Object value, String identifier) {
		try {
			resetProcessor();
		} catch (Exception e) {
			log.error("Can not change parameter {} with value {} for identifier {}! Msg: {}",
					parameterName, value, identifier, e.toString());
		}
	}

	@Override
	public void parametersChanged(Map<String, Object> parameters, String identifier) {
		try {
			resetProcessor();
		} catch (Exception e) {
			log.error("Can not change parameters {} for identifier {}! Msg: {}",
					LogOutputHelper.toLogString(parameters), identifier, e.toString());
		}
	}

	@Override
	public void parametersChanged() {
		try {
			resetProcessor();
		} catch (Exception e) {
			log.error("can not change parameters! Msg: {}", e.toString());
		}
	}
	//----------------------------------------------------------------------------------------------------------------------------
	//endregion IParameterChangeListener Implementation


	/**
	 * ErmÃ¶glich das dynamische Erzeugen von Prozessoren auf Basis von Programmdaten.
	 *
	 * @param processorIdentifier - falls vorhanden, wird dieser benutzt, um aus der beanParameterMap den gewÃ¼nschten Prozessor zu erzeugen.
	 * @param processorType - wurde kein processorIdentifier definiert, so kann mit dem Typ der gewÃ¼nschte Prozessor aus der Map Ã¼ber den defaultBeanTypeMapProcessor bestimmt werden.
	 * @param beanParameterMaps
	 * @return
	 *
	 */
	protected IProcessor createProcessor(String processorIdentifier, String processorType, List<Map<String,Map<String, Object>>> beanParameterMaps) {
		IProcessorContext newCtx = this.getUsedContextProviderProcessor(null).createContext(this.runtimeContext, beanParameterMaps);
		if (processorIdentifier != null)
			return this.getUsedContextProviderProcessor(null).getProcessorProvider().getProcessorForBeanId(processorIdentifier, newCtx, this);
		if (processorType == null) processorType = "requestProzessor";
		return getUsedContextProviderProcessor(null).getProcessorProvider().getProcessorForType(processorType, newCtx, this);
	}


	public  <T extends IProcessor> T  createProcessor(Class<T> clazz, String processorIdentifier, String processorType, IProcessor parentProcessor, List<Map<String,Map<String, Object>>> beanParameterMaps) {
		IProcessorContext newCtx = this.getUsedContextProviderProcessor(null).createContext(this.runtimeContext, beanParameterMaps);
		if (processorIdentifier != null) {
			log.info("Try to create processor for identifier {}", processorIdentifier);
			return newCtx.getContextProviderProcessor().getProcessorProvider().getProcessorForBeanId(clazz, processorIdentifier, newCtx, parentProcessor);
		}
		if (processorType == null) processorType = "requestProcessor";
		log.info("Try to create processor for type {}", processorType);
		return newCtx.getContextProviderProcessor().getProcessorProvider().getProcessorForType(clazz, processorType, newCtx, parentProcessor);
	}

	public  <T extends IProcessor> T  createProcessor(Class<T> clazz, String processorIdentifier, String processorType, List<Map<String,Map<String, Object>>> beanParameterMaps) {
		return createProcessor(clazz, processorIdentifier, processorType, this, beanParameterMaps);
	}

	public IRuntimeContextProviderProcessor getUsedContextProviderProcessor(IProcessorContext ctx) {
		if (this.contextProviderProcessor != null)
			return contextProviderProcessor;
		if (this.getRuntimeContext() != null) return this.getRuntimeContext().getContextProviderProcessor();

		return ctx != null ? ctx.getContextProviderProcessor() : null;
	}
}