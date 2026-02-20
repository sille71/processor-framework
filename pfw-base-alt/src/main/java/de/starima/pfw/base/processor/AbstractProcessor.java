package de.starima.pfw.base.processor;

import java.util.*;

import de.starima.pfw.base.domain.ProcessorScope;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.context.api.LoadStrategy;
import de.starima.pfw.base.processor.context.domain.DefaultDescriptorConstructorContext;
import de.starima.pfw.base.processor.context.domain.DefaultTransformationContext;
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
import org.springframework.util.Assert;

@Slf4j
@Getter @Setter @SuperBuilder
@NoArgsConstructor
@Processor
public abstract class AbstractProcessor implements IProcessor, IParameterChangeListener, BeanNameAware {

    private String identifier;
	private String protoTypeIdentifier;
	private String fullBeanId;

	protected IProcessorContext reconContext;

	//private IParameterFunctionProcessor creatorParameterFunctionProcessor;

	//private IReconProcessor parentProcessor;

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
	@ProcessorParameter(ignoreInitialization = true)
	protected IProcessorDescriptor processorDescriptor;

	/**
	 * Die Initialisierung des ContextProviders muss immer vor der Initialisierung der anderen Parameter erfolgen. Er wird daher bereits in der initContextProvider(ctx) Methode
	 * initialisiert und in der initParameters Methode ignoriert!
	 * Der Parameter hier ist doppelt gemoppelt, da jeder ContextProvider wÃ¤hrend der Erzeugung des Kontextes sich an diesem Kontext registriert und somit jedem Prozessor zur VerfÃ¼gung steht.
	 * Die Annotation und damit der Parameter sind aber dennoch wichtig, damit der ProcessorDescriptor diesen auch als Parameter ausweisen kann!
	 * Er sollte vor dem IProcessorDescriptor initialisiert werden, da der ContextProvider alle relevanten Parameter bereitstellt.
	 */
	@ProcessorParameter(ignoreInitialization = true)
	protected IRuntimeContextProviderProcessor contextProviderProcessor;

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
			/**
			if (identifier != null && protoTypeIdentifier != null && !identifier.equals(protoTypeIdentifier))
				fullBeanId = getProtoTypeIdentifier() + ":" + getIdentifier() + "@" + getScope().name();
			else if (getScope() != null) {
				fullBeanId = getProtoTypeIdentifier() + "@" + getScope().name();
			} else {
				fullBeanId = getProtoTypeIdentifier();
			}
			 */
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
	//region IReconProcessor Implementation
	//----------------------------------------------------------------------------------------------------------------------------
	public void init(IProcessorContext ctx) throws Exception {
		if (ProcessorUtils.isConsideredProcessor(this.getClass()))
			log.debug("init processor {} for context {}", getIdentifier(), ctx);
		else log.warn("can not init processor {} no annotation Processor present!", getIdentifier());

		IProcessorContext context = initContextProvider(ctx);
		IProcessorContext finalContext =  context != null ? context.getFinalContext() : null;

		ProcessorUtils.registerProcessorInScope(this, context);

		if (context != null) {
			//Jetzt sollte ein ContextProvider zur VerfÃ¼gung stehen
			Assert.notNull(getUsedContextProviderProcessor(finalContext), "No context provider available!");
			this.reconContext = getUsedContextProviderProcessor(finalContext).createInitialzerContext(this, context);
			initProcessorDescriptor(this.reconContext);
			//an dieser Stelle sind die Parameter schon durch einen zusÃ¤tzlichen ParameterProvider angereichert.
			//Das wÃ¤re der Fall, wenn fÃ¼r diesen Prozessor ein ContextProvider mit einem ParameterProvider konfiguriert wurde (also initContextProvider() einen neuen Kontext liefert).

			initParameters(getParameters(this.reconContext));
			this.reconContext = this.reconContext.cleanUpInitializerContext(finalContext);

			this.processorOnInit();

			if (this.getClass().isAnnotationPresent(Processor.class))
				log.debug("initialized processor {} with parameters {}",
						getIdentifier(), LogOutputHelper.getModelAsStringBuffer(getParameters(this.reconContext), null));
		}
	}

	public void refreshParameters(Map<String,Map<String, Object>> beanParameterMap) {
		if (this.reconContext != null) {
			log.debug("{}: refresh context with parameters {}", getIdentifier(), LogOutputHelper.getModelAsStringBuffer(beanParameterMap, null));
			this.reconContext.refreshBeanParameterMap(beanParameterMap);
			IProcessorContext finalContext =  this.reconContext != null ? this.reconContext.getFinalContext() : null;
			Assert.notNull(getUsedContextProviderProcessor(finalContext), "No context provider available!");
			this.reconContext = getUsedContextProviderProcessor(finalContext).createInitialzerContext(this, this.reconContext);
			initProcessorDescriptor(this.reconContext);
			//an dieser Stelle sind die Parameter schon durch einen zusÃ¤tzlichen ParameterProvider angereichert.
			//Das wÃ¤re der Fall, wenn fÃ¼r diesen Prozessor ein ContextProvider mit einem ParameterProvider konfiguriert wurde (also initContextProvider() einen neuen Kontext liefert).
			initParameters(getParameters(this.reconContext));
			this.reconContext = this.reconContext.cleanUpInitializerContext(finalContext);

			this.processorOnRefresh();

			if (this.getClass().isAnnotationPresent(Processor.class))
				log.debug("refreshed context {} with parameters {}",
						getIdentifier(), LogOutputHelper.getModelAsStringBuffer(getParameters(this.reconContext), null));
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
	 * Liefert den Context in dem dieser Prozessor initialisiert wird.
	 * 
	 * @param ctx - Elternkontext
	 * @return
	 */
	protected IProcessorContext initContextProvider(IProcessorContext ctx) {
		//1. Ermittle die Parameter des Prozessors aus dem Parent Kontext. Dabei wird mit dem Identifier des Prozesors zugegriffen.
		Map<String, Object> ctxParameters = getParameters(ctx);
		//2. Initialisierung der Default beanParameterMap
		Map<String,Map<String,Object>> defaults = this.initDefaultBeanParameterMap();
		//3. Wir prÃ¼fen zunÃ¤chst, ob es einen dedizierten ContextProvider in der Konfiguration gibt.
		this.contextProviderProcessor = this.createContextProviderFromParameters(ctxParameters, ctx);
		//4. Falls kein dedizierter ContextProvider in der Konfig gefunden wurde, checken wir die Default beanParameterMap.
		if (this.contextProviderProcessor == null && defaults != null) {
			//Default beanParameterMap: ein Processor sucht immer mit seinem PrototypeIdentifier in der Default beanParameterMap
			this.contextProviderProcessor = createContextProviderFromParameters(defaults.get(getProtoTypeIdentifier()), ctx);
		}
		//Falls ein ContextProvider gefunden wurde, so erzeugt dieser einen neuen Kontext auf Basis des Elternkontextes (ctx). Dieser neue Kontext wird dann benutzt, um dan
		//neuen Kontext zu erzeugen! Auf diese Weise kÃ¶nnen wir mit ContextProvidern eine Kontext Hierarchie aufbauen, siehe z.B. GlobalConfig, ClusterConfig, ReconConfig
		//Falls es keinen gibt, reichen wir den Ã¼bergebenen Kontext durch

		//5. Falls es defaults gibt, so erzeugen wir immer einen neuen Kontext mit den defaults, wenn dies nicht im contextProviderProcessor abgeschaltet wurde (isUseDefaultBeanParameterMap())
		if (defaults != null) {
			if (this.contextProviderProcessor != null) {
				//ist isUseDefaultBeanParameterMap() = false, so wird lediglich ein neuer Kontext erzeugt, wenn es einen dedizierten contextProvider gibt.
				return this.contextProviderProcessor.isUseDefaultBeanParameterMap() ? this.contextProviderProcessor.createContext(this.contextProviderProcessor.getRuntimeContext(),defaults) : this.contextProviderProcessor.createContext(this.contextProviderProcessor.getRuntimeContext());
			}
			//gibt es keinen dedizierten contextProvider, so nutzen wir den vorhanden aus der Context Hierarchie.
			return getUsedContextProviderProcessor(ctx).isUseDefaultBeanParameterMap() ? getUsedContextProviderProcessor(ctx).createContext(ctx,defaults) : ctx;
		} else {
			//Ohne defaults wird lediglich ein neuer Kontext erzeugt, wenn es einen dedizierten contextProvider gibt. Anderenfalls wird der Elternkontext verwendet.
			return this.contextProviderProcessor != null ? this.contextProviderProcessor.createContext(this.contextProviderProcessor.getRuntimeContext()) : ctx;
		}
	}

	protected IRuntimeContextProviderProcessor createContextProviderFromParameters(Map<String, Object> parameters, IProcessorContext ctx) {
		if (parameters == null) return null;
		Object contextProviderProcessorParam = parameters.get("contextProviderProcessor") != null ? parameters.get("contextProviderProcessor") : parameters.get("contextProviderProcessorIdentifier");
		//es gibt keinen, fÃ¼r diesen Processor konfigurierten, contextProvider
		if (contextProviderProcessorParam == null) return null;

		//es gibt einen konfigurierten ContextProvider
		//wir benutzen den ProcessorProvider des Elternkontextes (wurde Ã¼bergeben), um den neuen contextProvider zu erzeugen
		return ctx.getProcessorProvider().getProcessorForBeanId(IRuntimeContextProviderProcessor.class, contextProviderProcessorParam.toString(), ctx, this);
	}

	/**
	 * Erzeugung des Prozessordescriptors - Ablauf der initProcessorDescriptor Methode
	 * ----------------------
	 * Grundsatz:
	 * 1. Jeder Prozessor, der Ã¼ber eine beanParameterMap initialisiert wird, benÃ¶tigt einen Prozessordescriptor! D.h. wird im ProcessorProvider die init(ctx) Methode mit einem ctx != null aufgerufen, so wird ein ProzessorDescriptor initialisiert. Ausnahme ist die Instanz, da hier die init Methode generell mit einem ctx == null aufgerufen wird und der RootKontext erst aufgebaut wird (ContextProvider), sowie der designierte ProcessorDescriptor.
	 * 2. Zu jedem Prozessor kann ein designierter ProzessorDescriptor erzeugt werden, der nicht der klassischen Initialisierung unterliegt (siehe initDesignatedProcessorDescriptor). Dieser ProzessorDescriptor benÃ¶tigt daher selbst keinen Descriptor!
	 * + **Start**:
	 *  Es wird versucht aus den Parametern des zu initialisierenden Prozessors im aktuellen Kontext einen ProzessorDescriptor zu initialisieren. Dabei wird die beanId aus dem Parameter processorDescriptor oder processorDescriptorIdentifier entnommen. Die folgenden FÃ¤lle werden unterschieden:
	 *     + Parameter vorhanden: es wird geprÃ¼ft, ob die Initialisierung mit der beanId mÃ¶glich ist
	 *       + nicht mÃ¶glich: um Rekursionen zu vermeiden, wird die Erzeugung eines ProcessorDescriptors in der Ebene 3 unterbunden. Es wird der designierte Descriptor erzeugt  **Ende**
	 *       + mÃ¶glich: Es wird versucht aus der beanId einen Prozessor mit dem aktuellen ProcessorProvider zu erzeugen
	 *         + ProcessorDescriptor konnte erzeugt werden: Reicht der an dieser Stelle oder muss er mit dem Prototypen verschmolzen werden???? Der so erzeugte Prozessordescriptor ist mÃ¶glicherweise nicht vollstÃ¤ndig. Er sollte mit den aus den Annotationen erzeugten designierten Descriptor verschmolzen werden!
	 *         + ProcessorDescriptor konnte nicht erzeugt werden: Aus der beanId konnte kein Prozessor erzeugt werden. Dies kann beabsichtigt sein, indem man als beanId z.B 'no' setzt. So kann man steuern, ob ein ProcessorDescriptor aus der Annotation erzeugt werden soll oder nicht (generatePrototypeProcessorDescriptor()). In diesen Fall wird nur der designierte Descriptor verwendet. **Ende**
	 *     + Parameter nicht vorhanden: Es wird ein ProcessorDescriptor aus den Annotationen erzeugt (generatePrototypeProcessorDescriptor()). **Ende**
	 * Was ist der Unterschied zwischen dem Prototyp ProcessorDescriptor und dem designierten?
	 * -------
	 * + Der Prototyp wird in einem neuen, temporÃ¤ren Kontext mit der klassischen init Methode erzeugt. Dabei wird die BeanParameterMap aus den Annotationen gewonnen. Er kann also selbst wieder einen Descriptor besitzen (wird dann der designierte sein)
	 * + Der designierte ProcessorDescriptor wird programmatisch aus den Annotationen erzeugt und benÃ¶tigt selbst keinen Descriptor mehr.
	 * Beide besitzen nicht den explizit gesetzten designated ProcessorDescriptor. Daran kann man sehen, ob der Descriptor aus der ParameterMap gewonnen oder generiert wurde. Der generierte Descriptor sollte nicht in eine ParameterMap gesichert werden, da sich die Code Basis Ã¤ndern kÃ¶nnte!
	 * @param ctx
	 */
	protected void initProcessorDescriptor(IProcessorContext ctx) {
		log.info("{}: init processor descriptor ", getFullBeanId());
		/**
		 * Jeder Processor kann durch einen ProcessorDescriptor beschrieben werden. Da die Descriptoren selbst wieder Processoren sind, werden auch sie
		 * letztendlich durch einen ProcessorDescriptor beschrieben. Dieser sollte dann aber final sein!
		 * Das bedeutet: Ein ProcessorDescriptor wird nicht mehr benÃ¶tigt, wenn sich der zugehÃ¶rige Source Processor in einer Hierarchie eines ProcessorDescriptors
		 * befindet, der wiederum einen ProcessorDescriptor beschreibt! In solchen FÃ¤llen liefert isProcessorCreationPossible des ProcessorProviders false zurÃ¼ck und es wird kein
		 * Descriptor erzeugt.
		 */
		//der Prozessordescriptor ist zukÃ¼nftig der Ausgangspunkt fÃ¼r die Parameterinitialisierung, daher muss er zuerst initialisiert werden (siehe Angular)
		//er sollte nach dem ContextProvider initialisiert werden, da der ContextProvider alle relevanten Parameter bereitstellt

		Map<String, Object> ctxParameters = getParameters(ctx);

		if (ctxParameters != null) {
			//gibt es einen spezialisierten (konkreten) descriptor aus der aktuellen Konfiguration?
			//wir kÃ¶nnen Ã¼ber den Parameter auch steuern, ob ein Descriptor gewÃ¼nscht ist (processorDescriptor = no)
			Object processorDescriptorParam = ctxParameters.get("processorDescriptor") != null ? ctxParameters.get("processorDescriptor") : ctxParameters.get("processorDescriptorIdentifier");
			if (processorDescriptorParam != null) {
				if (ctx.getProcessorProvider().isProcessorCreationPossible(processorDescriptorParam.toString(), ctx, this)) {
					log.info("{}: try to init processor descriptor from parameter {}", getFullBeanId(), processorDescriptorParam);
					//es gibt einen konfigurierten ProcessorDescriptor
					//wir benutzen den ProcessorProvider des Elternkontextes (wurde Ã¼bergeben), um den neuen ProcessorDescriptor zu erzeugen
					this.processorDescriptor = ctx.getProcessorProvider().getProcessorForBeanId(IProcessorDescriptor.class, processorDescriptorParam.toString(), ctx, this);
					// TODO der Prozessor Descriptor muss noch Methoden bekommen, um den Prototypen zu nutzen, falls Parameter nicht explizit definiert wurden
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
	 *
	 * @return
	 */
	@Override
	public IProcessorDescriptor generatePrototypeProcessorDescriptor() {
		DefaultDescriptorConstructorContext context = new DefaultDescriptorConstructorContext();
		context.setSourceType(this.getClass());
		context.setRuntimeContext(getRuntimeContext());
		context.setParentProcessor(Optional.of(this));
		Map<String, Map<String, Object>> bluePrint = new HashMap<>();
		String fullBeanId = ProcessorUtils.generateDescriptorBlueprint(context,bluePrint);
		return this.createProcessor(IProcessorDescriptor.class, fullBeanId, null, Collections.singletonList(bluePrint));
	}

	@Override
	public IProcessorDescriptor generateProcessorDescriptorInstance(LoadStrategy loadStrategy) {
		DefaultDescriptorConstructorContext context = new DefaultDescriptorConstructorContext();
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
		DefaultTransformationContext transformationContext = new DefaultTransformationContext();
		transformationContext.setTargetObject(this);
		transformationContext.setRuntimeContext(getRuntimeContext());
		return ProcessorUtils.extractEffectiveParameters(transformationContext);
	}

	public Map<String,Map<String, Object>> extractEffectiveProcessorParameterMap() {
		IProcessorDescriptor instanceDescriptor = this.processorDescriptor;
		// 1. Erzeuge den vollstÃ¤ndigen Instanz-Deskriptor fÃ¼r diesen Prozessor.
		//    Dieser Deskriptor ist der Root unseres Traversierungsbaums.
		instanceDescriptor = instanceDescriptor != null ? instanceDescriptor : this.generateProcessorDescriptorInstance(LoadStrategy.DEEP);
		if (instanceDescriptor == null) {
			log.error("Konnte keinen Instanz-Deskriptor fÃ¼r {} erzeugen. Extraktion nicht mÃ¶glich.", this.getFullBeanId());
			return new HashMap<>();
		}

		// 2. Baue den initialen Kontext fÃ¼r den Start der Extraktion.
		DefaultTransformationContext startContext = new DefaultTransformationContext();
		startContext.setTargetObject(this);
		startContext.setRuntimeContext(getRuntimeContext());
		startContext.setLoadStrategy(LoadStrategy.DEEP);

		// 3. Bereite die finale Ergebnis-Map und das Set fÃ¼r die Zykluserkennung vor.
		Map<String, Map<String, Object>> beanParameterMap = new HashMap<>();
		Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());

		// 4. Starte die dezentrale, polymorphe Extraktion.
		//    Der Aufruf geht an den Wurzel-Deskriptor, der den Prozess an seine Kinder weitergibt.
		instanceDescriptor.extractEffectiveParameterMap(startContext, beanParameterMap, visited);

		return beanParameterMap;
	}

	public IProcessorContext getRuntimeContext() {
		return reconContext;
	}

	//TODO: ist zu Ã¼berarbeiten
	protected void resetProcessor() {
		try {
			if (this.reconContext.getParentContext() != null) {
				this.reconContext.getParentContext().removeReconContext(this.reconContext);
				init(this.reconContext.getParentContext());
			} else {
				init(this.reconContext);
			}
		} catch (Exception e) {
			log.error("Can not reset processor {}, Msg: {}", getIdentifier(), e.toString());
		}
	}
	//----------------------------------------------------------------------------------------------------------------------------
	//endregion IReconProcessor Implementation

	//region IParameterChangeListener Implementation
	//----------------------------------------------------------------------------------------------------------------------------

	/**
	 * Wir verwenden hier eine einfache Implementierung des IParameterChangelistener. Diese setzt voraus, das
	 * der ParameterProvider, der das Event triggert auch zu diesem Prozessor gehÃ¶rt.
	 * //TODO: kann in Zukunft verfeinert werden. So kÃ¶nnen beispielsweise nur die Parameter aktualisiert werden, die sich auch verÃ¤ndert haben.
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
					LogOutputHelper.getModelAsStringBuffer(parameters, null), identifier, e.toString());
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
		IProcessorContext newCtx = this.getUsedContextProviderProcessor(null).createContext(this.reconContext, beanParameterMaps);
		if (processorIdentifier != null)
			return this.getUsedContextProviderProcessor(null).getProcessorProvider().getProcessorForBeanId(processorIdentifier, newCtx, this);
		if (processorType == null) processorType = "requestProzessor";
		return getUsedContextProviderProcessor(null).getProcessorProvider().getProcessorForType(processorType, newCtx, this);
	}


	public  <T extends IProcessor> T  createProcessor(Class<T> clazz, String processorIdentifier, String processorType, IProcessor parentProcessor, List<Map<String,Map<String, Object>>> beanParameterMaps) {
		IProcessorContext newCtx = this.getUsedContextProviderProcessor(null).createContext(this.reconContext, beanParameterMaps);
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