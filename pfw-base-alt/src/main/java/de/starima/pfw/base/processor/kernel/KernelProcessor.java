package de.starima.pfw.base.processor.kernel;

import de.dzbank.components.utils.log.LogOutputHelper;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.IRuntimeContextProviderProcessor;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.context.api.LoadStrategy;
import de.starima.pfw.base.processor.context.domain.DefaultProcessorContext;
import de.starima.pfw.base.processor.description.api.IProcessorDescriptor;
import de.dzbank.recon.ms.base.processor.description.incubator.api.*;
import de.dzbank.recon.ms.base.processor.description.incubator.domain.*;
import de.starima.pfw.base.processor.description.incubator.api.*;
import de.starima.pfw.base.processor.description.incubator.domain.*;
import de.starima.pfw.base.processor.kernel.api.IKernelBeanProvider;
import de.starima.pfw.base.processor.request.api.IRequestProcessor;
import de.starima.pfw.base.processor.request.domain.AssetHttpRequest;
import de.starima.pfw.base.processor.service.api.IRuntimeServiceProcessor;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Processor
@Service
@RestController
public class KernelProcessor implements IProcessor, IIncubator, BeanNameAware {
    private String protoTypeIdentifier;
    protected IProcessorContext reconContext;

    @ProcessorParameter(ignoreInitialization = true, description = "Der ContextProvider fÃ¼r den Kernel. Dieser wird explizit im Konstruktor gestezt")
    private final IRuntimeContextProviderProcessor contextProviderProcessor;
    @ProcessorParameter(ignoreInitialization = true, description = "Der ConstructionManager fÃ¼r den Kernel. Dieser wird explizit im Konstruktor gestezt")
    private final IConstructionManager constructionManager;

    private final IKernelBeanProvider beanProvider;

    //zu Ã¼berarbeiten
    @ProcessorParameter(value = "Bootstrap", ignoreInitialization = true)
    private String phaseName = "Bootstrap";
    @ProcessorParameter(value = "0", ignoreInitialization = true)
    private Integer runLevel = 0;


    @ProcessorParameter(description = "Der vollstÃ¤ndige Incubator, welcher vom kernel durch den schmaleren kernelIncubator erzeugt wird. Sobald dieser initialisiert wurde, " +
            "Ã¼bernimmt er die restliche Initialisierung des Kernels inkl. des serviceProcessor.", value = "frameworkIncubator", ignoreInitialization = true)
    private IIncubator frameworkIncubator;

    @ProcessorParameter(description = "Der fachliche Microservice.")
    private IRuntimeServiceProcessor serviceProcessor;

    @Autowired
    public KernelProcessor(KernelRuntimeContextProviderProcessor contextProviderProcessor, KernelConstructionManager constructionManager, KernelBeanProvider beanProvider) {
        this.contextProviderProcessor = contextProviderProcessor;
        this.constructionManager = constructionManager;
        this.beanProvider = beanProvider;
    }

    /**
     * Der Wertegang der Konstruktion eines Processors.
     * Ausgangspunkt ist die fullBeanId = <prototypeId>:<identifier>@scope:
     * 1. Instanziierung: Erfolgt Ã¼ber den BeanProvider, dieser kapselt eine Registry (siehe Typescript) oder einen anderen Mechanismus (wie hier Spring Beans).
     *    Die Registrierung erfolgt Ã¼ber die prototypeId (aus der fullbeanId)
     * 2. Setzen des Identifiers und Scopes (aus der fullBeanId).
     * 3. Initialisierung des Kontextes (muss vor allen anderen Punkten erfolgen)
     * 4. Initialisierung des ProcessorDescriptors (falls es der Zustand/RunLevel zulÃ¤sst)
     * 5. Initialisierung der Parameter:
     * 6. WarmUp: alles, was nach der Initialisierung erfolgen muss, z.B. bei den Descriptoren mÃ¼ssen ParentBeziehungen gesetzt werden (optional) - ist so was wie onInit
     * 7. Validierung: (optional - evtl. Selbsttests? oder nur einfache Constraint Validierung)
     * 8. Start: evtl. werden hier Methoden automatisch gestartet. Zu mindest wird der Zustand Betriebsbereit gesetzt.
     */
    @PostConstruct
    public void initProcessor() {
        //INSTANTIATET: der Kernel liegt jetzt im Zustand/RunLevel instanziert vor
        // der nÃ¤chste Zustand muss sein ContextAvailable
        try {
            log.info("{}: Start {} phase ...", getIdentifier(), getPhaseName());
            //Step 1: context initialisation
            log.info("{}:{} Create kernel context ...", getIdentifier(), getPhaseName());
            this.reconContext = this.contextProviderProcessor.createContext(null);
            ProcessorUtils.registerProcessorInScope(this, this.reconContext);
            //CONTEXTAVAILABLE: jetzt liegt der Zustand ContextAvailable vor

            // das nÃ¤chste Ziel ist es, den frameworkIncubator zu initialisieren
            log.info("{}:{} Create frameworkIncubator ...", getIdentifier(), getPhaseName());
            this.frameworkIncubator = constructFrameworkIncubator();
            // FRAMEWORK_READY: das Framework kann jetzt benutzt werden, um die Services zu initialisieren. Abhier kÃ¶nnen nun auch die Descriptren erzuegt werden.






            //Jetzt sind alle Kernel Parameter gesetzt. Die Phasen liegen nur als Prototyp vor (sind noch nicht initialisiert!). Wie erreichen wir das Verhalten?
            // die Phase aus der Liste mÃ¼ssen jetzt initialisiert werden
            //an dieser Stelle wird es nur eine Phase sein, welche den fullIncubator initialisiert (FullIncubatorPhaseProcessor,
            // dieser hat dann die ApplicationPhase, welche ausschlieÃŸlich den fullIncubator benutzt, um alle weiteren Prozessoren zu initialisieren. Der kernelIncubator
            // wird dann nicht mehr benÃ¶tigt, an seiner Stelle wird der fullIncubator im Context gesetzt (Das sollte im Application Context geschehen, aber um den Kernel auch
            // mit Descriptoren beschreiben zu kÃ¶nnen, muss er auch den fullIncubator benutzen. Es ist noch zu klÃ¤ren, wie das zu bewerkstelligen ist.
            // Beachte: der kernelIncubator kann nur minimal konstruieren, er kann nicht beschreiben!)


            //Step 2: descriptor initialisation
            //ein ProcessorDescriptor wird hier nicht initialisisert!

            //Step 3: serviceIncubator initialisation
            //log.info("{}: Create serviceIncubator ...", getIdentifier());
            //erfolgt in der Application Phase
            //this.serviceIncubator = initServiceIncubator();

            //wir setzen den serviceIncubator in den context. Ab jetzt kann Ã¼ber diesen serviceIncubator der Rest der Parameter initialisiert werden!

            // wir initialisieren die restlichen Parameter (serviceProcessor, beanTypeMapProcessor, useDefaultBeanParameterMap)
            //brauchen wir so nicht mehr, da dies schon durch den kernelIncubator erledigt wurde
            //finalizeInitialisation();

        } catch (Exception e) {
            log.error("Can not initialize instance processor!", e);
        }
    }

    //Beispielmethode, um aus einer FullBeanId einen Processor zu bauen. Die beanParameterMap liegt dabei im ctx vor.
    private IProcessor constructProcessor(String fullBeanId) {
        // 1. Instanziierung: Erfolgt Ã¼ber den BeanProvider, dieser kapselt eine Registry (siehe Typescript) oder einen anderen Mechanismus (wie hier Spring Beans).
        // Die Registrierung erfolgt Ã¼ber die prototypeId (aus der fullbeanId)
        IProcessor draftProcessor = this.beanProvider.getBeanForId(IProcessor.class, ProcessorUtils.getPrototypeId(fullBeanId));
        if (draftProcessor == null) return null;

        //2. Setzen des Identifiers und Scopes (aus der fullBeanId).
        draftProcessor.setIdentifier(ProcessorUtils.getIdentifier(fullBeanId));
        draftProcessor.setScope(ProcessorUtils.getProcessorScope(fullBeanId));

        //3. Initialisierung des Kontextes (muss vor allen anderen Punkten erfolgen)
        // Jeder Processor kann von Haus aus einen contextProvider haben. Dies erfolgt in der Regel konfigurativ. Falls einer vorhanden ist, dann muss dieser zunÃ¤chst
        // bestimmt werden (fullBeanId aus beanParameterMap) und dann initialisiert werden. Danach kann dieser einen neuen Kontext auf Basis des bestehenden Kontextes erzeugen.
        // Es ist also nicht unbedingt ratsam dies im Processor selbst zu tun (so wie jetzt), sondern es von auÃŸen (hier) zu machen.
        // Wie stellen wir das am besten an? Das ProcessorInterface benÃ¶tigt dann lediglich eine Schnittstelle onInitContext(newCtx) oder so Ã¶hnlich.
        // draftProcessor.initContext(this.reconContext);

        //4. Initialisierung des ProcessorDescriptors (falls es der Zustand/RunLevel zulÃ¤sst)
        // Ein ProcessorDescriptor ist nur im RunLevel 2 mÃ¶glich, wenn also der fullIncubator (bzw der DescriptorIncubator) zur VerfÃ¼gung steht!
        // Dazu ist folgendes zu beachten:
        // Es kann immer ein Descriptor aus dem Processor Typ (Klasse) erzeugt werden. Dies wird der Prototyp Descriptor (bzw. BluePrint).
        // ZusÃ¤tzlich kann aber auch eine Descriptor Konfiguration in der beanParameterMap hinterlegt sein. Denn jeder Processor hat einen Parameter processorDescriptor.
        // Dieser konfigurierte Descriptor muss nicht vollstÃ¤ndig sein, sondern kann lediglich nur Fragmente enthalten. Beispielsweise kann lediglich eine fachliche
        // Beschreibung spezialDescription hinzugefÃ¼gt oder ein Range eines Parameters eingeschrÃ¤nkt werden. Trotzdem mÃ¼ssen wir diesen Descriptor als Processor innerhalb dieser
        // Konstruktionslinie erzeugen! Und dann mit dem Prototyp Descriptor vereinen (beachte: hierzu hat zunÃ¤chst jeder IValueDescriptor eine Eigenschaft prototypeValueDescriptor!
        //
        // Danach kann der Descriptor dem draftProcessor Ã¼bergeben werden. onInitDescriptor(descriptor)
        // draftProcessor.initProcessorDescriptor

        //5. Initialisierung der Parameter:
        // Wenn wir im Schritt 4 einen Descriptor Erzeugt haben, ist es jetzt wohl die beste Wahl, diesen Descriptor zur Initialisierung der Parameter zu verwenden,
        // d.h. der verantwortliche Processor erhÃ¤lt den Descriptor als Plan und den draftProcessor. Dann kann er Ã¼ber die Parameter im Descriptor iterieren und mit den
        // dortigen ValueFunctions die Parameter initialisieren. Hier kommt der Punkt! Irgendwann mÃ¼ssen wieder Processoren (als ParameterValues) in dieser Produktionslinie
        // erzeugt werden! D.h. es muss noch einen weiteren Einstiegspunkt als der obige constructProcessor(fullBeanId) geben!?
        //

        // 6. WarmUp: alles, was nach der Initialisierung erfolgen muss
        // draftProcessor.onInit()

        //7.
    }



    private IIncubator constructFrameworkIncubator() {
        try {
            ConstructSource cSource = new ConstructSource();
            cSource.setRuntimeContext(this.reconContext);
            cSource.setSourceField(this.getClass().getDeclaredField("frameworkIncubator"));
            return this.construct(IIncubator.class, cSource, null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setBeanName(String name) {
        this.protoTypeIdentifier = name;
    }

    //Start IReconProcessor Implementierung
    @Override
    public void init(IProcessorContext ctx) throws Exception {

    }

    @Override
    public void refreshParameters(Map<String, Map<String, Object>> beanParameterMap) {
        //nicht im Kernel
    }

    @Override
    public String getIdentifier() {
        return this.protoTypeIdentifier;
    }

    @Override
    public void setIdentifier(String identifier) {
        //nicht im Kernel
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.instance;
    }

    @Override
    public void setScope(ProcessorScope scope) {
        //nicht im Kernel
    }

    @Override
    public String getProtoTypeIdentifier() {
        return this.protoTypeIdentifier;
    }

    @Override
    public String getFullBeanId() {
        return ProcessorUtils.createFullBeanId(getProtoTypeIdentifier(), getIdentifier(), getScope());
    }

    @Override
    public IProcessorDescriptor getProcessorDescriptor() {
        return null;
    }

    @Override
    public IProcessorContext getRuntimeContext() {
        return this.reconContext;
    }

    @Override
    public void setRuntimeContext(IProcessorContext ctx) {
        //nicht von auÃŸen im Kernel
    }

    @Override
    public Map<String, Map<String, Object>> initDefaultBeanParameterMap() {
        return Map.of();
    }

    @Override
    public Map<String, Object> extractEffectiveProcessorParameters() {
        return Map.of();
    }

    @Override
    public Map<String, Map<String, Object>> extractEffectiveProcessorParameterMap() {
        return Map.of();
    }

    @Override
    public IProcessorDescriptor generatePrototypeProcessorDescriptor() {
        return null;
    }

    @Override
    public IProcessorDescriptor generateProcessorDescriptorInstance(LoadStrategy loadStrategy) {
        return null;
    }

    @Override
    public <T extends IProcessor> T createProcessor(Class<T> clazz, String processorIdentifier, String processorType, IProcessor parentProcessor, List<Map<String, Map<String, Object>>> beanParameterMaps) {
        return null;
    }

    @Override
    public <T extends IProcessor> T createProcessor(Class<T> clazz, String processorIdentifier, String processorType, List<Map<String, Map<String, Object>>> beanParameterMaps) {
        return null;
    }
    //End IReconProcessor Implementierung


    private IIncubator initServiceIncubator() {
        if (this.kernelIncubator == null) {
            log.error("{}: Can not initialize serviceIncubator! No kernelIncubator set!", getIdentifier());
            throw  new RuntimeException("Can not initialize serviceIncubator!");
        }

        try {
            ConstructSource cSource = new ConstructSource();
            cSource.setRuntimeContext(this.reconContext);
            cSource.setSourceField(this.getClass().getDeclaredField("serviceIncubator"));
            return this.kernelIncubator.construct(IIncubator.class, cSource, null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private void finalizeInitialisation() {
        ConstructSource cSource = new ConstructSource();
        cSource.setDraftProcessor(this);
        this.serviceIncubator.construct(cSource, null);
    }



    @Override
    protected IProcessorContext initContextProvider(IProcessorContext ctx) {
        return this.createContext(ctx);
    }

    //Beginn ContextProvider Implementierung
    @Override
    public IProcessorContext createContext(IProcessorContext parentCtx) {
        IProcessorContext newCtx = createNewContext(parentCtx);

        if (this.getParameterProviderProcessor() != null) {
            log.info("{}: get parameters from parameter provider {}", getIdentifier(), this.getParameterProviderProcessor().getIdentifier());
            newCtx.setBeanParameterMap(this.getParameterProviderProcessor().getBeanParameterMap());
        }

        //Achtung: zur Zeit der Initialisierung des RootContextes (initContextProvider()) exisitert der Prozessor noch nicht, da er erst nach der ContextInitialisierung
        //gesetzt wird. Wir benÃ¶tigen ihn auch nicht direkt im Kernel, sondern spÃ¤ter dann bei der Initialisierung anderer Prozessoren (z.B. des ServiceProcessors)
        if (this.getBeanTypeMapProcessor() != null) {
            log.info("{}: get beanIdTypeMap from beanTypeMap processor {}", getIdentifier(), this.getBeanTypeMapProcessor().getIdentifier());
            newCtx.setBeanIdTypeMap(this.getBeanTypeMapProcessor().getBeanIdTypeMap());
        }
        return newCtx;
    }



    protected IProcessorContext createNewContext(IProcessorContext parentCtx) {
        IProcessorContext newCtx = new DefaultProcessorContext();
        newCtx.setName(getIdentifier() + UUID.randomUUID());
        if (parentCtx != null) {
            log.info("create new child context {} in parent context {}", newCtx.getName(), parentCtx.getName());
            parentCtx.addReconContext(newCtx);
        } else {
            log.info("create new root context {}", newCtx.getName());
        }
        newCtx.setContextProviderProcessor(this);
        return newCtx;
    }

    @Override
    public IProcessorContext createContext(IProcessorContext parentCtx, List<Map<String, Map<String, Object>>> parameterMaps) {
        if (parameterMaps == null || parameterMaps.isEmpty()) {
            return parentCtx != null ? parentCtx : this.createContext(null);
        }
        IProcessorContext newCtx = parentCtx;
        for (Map<String, Map<String, Object>> pm : parameterMaps) {
            newCtx = createContext(newCtx, pm);
        }

        return newCtx;
    }

    @Override
    public IProcessorContext createContext(IProcessorContext parentCtx, Map<String, Map<String, Object>> parameterMap) {
        if (parameterMap == null || parameterMap.isEmpty()) {
            return parentCtx != null ? parentCtx : this.createContext(null);
        }

        IProcessorContext newCtx = createNewContext(parentCtx);
        //wir benutzen hier nicht den ParameterProvider, sondern die Ã¼bergebenen Parameter, um den neuen Kontext mit der ParameterMap
        // auszustatten
        newCtx.setBeanParameterMap(parameterMap);
        //falls ein BeanTypeMap Processor vorhanden ist, benutzen wir dessen identifier, um einen neuen aus der parameterMap zu erzeugen, der die BeanIdType Map des neuen Prozessors setzt
        //Das benÃ¶tigen wir beispielsweise beim Deployment von Recon Konfigurationen, dort steckt der Deployment Prozessor in der Konfig
        if (this.getBeanTypeMapProcessor() != null) {
            log.info("{}: get beanIdTypeMap from parameterMap with beanTypeMap processor {}", getIdentifier(), this.getBeanTypeMapProcessor().getIdentifier());
            newCtx.setBeanIdTypeMap(this.getBeanTypeMapProcessor().getBeanIdTypeMapFromParameterMap(parameterMap));
        }
        return newCtx;
    }

    public IProcessorContext createInitialzerContext(IProcessor processor, IProcessorContext parentCtx) {
        IProcessorContext iCtx = createNewContext(parentCtx);
        iCtx.setInitializedProcessor(processor);
        iCtx.setContextProviderProcessor(this);
        return iCtx;
    }

    //TODO: die origins sind zu spezifizieren (am besten konfigurativ durch einen Prozessor - dieser bekommt einen httpRequest und liefert den passenden Header)
    //siehe rcn_backlog
    @CrossOrigin(maxAge = 60)
    @PostMapping(path = "/processBeanParameterMapRequest", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String,Map<String, Object>>> processBeanParameterMapRequest(@RequestBody Map<String,Map<String, Object>> beanParameterMap) {
        log.info("call processBeanParameterMapRequest with data {}", LogOutputHelper.getModelAsStringBuffer(beanParameterMap, null));
        IRequestProcessor requestProcessor = createProcessor(IRequestProcessor.class,
                null, "requestProcessor", Collections.singletonList(beanParameterMap));
        //TODO: soll der requestProcessor in den Kontext gehangen werden (zumindest fÃ¼r die Zeit, die er gebraucht wird (also innerhalb dieser Methode)?
        //Wie kann dieser Prozessor in der Dokumentation erfasst werden? Muss er als Parameter hinterlegt werden? Oder scannt die Docu auch den Kontext?
        log.info("found request processor {}", requestProcessor != null ? requestProcessor.getIdentifier() : null);

        //TODO: Exception handling
        return requestProcessor != null ? ResponseEntity.ok().body(requestProcessor.processRequest()) : null;
    }

    //TODO: hier wird sich noch etwas Ã¤ndern, z.B. wenn mehrere Dateien hochgeladen werden. Das Ã¤ndert aber am Prinzip nichts
    //TODO - wird durch processMultipartHttpServletRequest abgelÃ¶st
    //das file kÃ¶nnte auch als Base64 kodiert in die beanParameterMap aufgenommen werden, dann kÃ¶nnte der allgemeine Aufruf processRequest() aufgerufen werden
    @CrossOrigin(maxAge = 60)
    @PostMapping(path = "/processMultipartRequest", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Map<String,Map<String, Object>>> processMultipartRequest(@RequestPart String requestProcessorIdentifier ,@RequestPart Map<String,Map<String, Object>> beanParameterMap, @RequestPart MultipartFile file) {
        log.info("call processMultipartRequest with request processor {} and beanParameterMap data {}",requestProcessorIdentifier, LogOutputHelper.getModelAsStringBuffer(beanParameterMap, null));
        if (file != null) log.info("got file {}", file.getName());
        //wir versuchen zunÃ¤chst den request Prozessor aus dem requestProcessorIdentifier zu erzeugen
        IRequestProcessor requestProcessor = createProcessor(IRequestProcessor.class,
                requestProcessorIdentifier, null, Collections.singletonList(beanParameterMap));
        //falls keiner gefunden wurde schauen wir nach dem typ requestProcessor
        if (requestProcessor == null) {
            requestProcessor = createProcessor(IRequestProcessor.class,
                    null, "requestProcessor", Collections.singletonList(beanParameterMap));
        }

        //TODO: soll der requestProcessor in den Kontext gehangen werden (zumindest fÃ¼r die Zeit, die er gebraucht wird (also innerhalb dieser Methode)?
        //Wie kann dieser Prozessor in der Dokumentation erfasst werden? Muss er als Parameter hinterlegt werden? Oder scannt die Doku auch den Kontext?
        //LÃ¶sung: die Instanz bekommt als Parameter die Menge aller mÃ¶glichen RequestProcessor Prototypeidentifier
        log.info("found request processor {}", requestProcessor != null ? requestProcessor.getIdentifier() : null);

        //TODO: Exception handling
        return requestProcessor != null ? ResponseEntity.ok().body(requestProcessor.processMultipartRequest(file)) : null;
    }

    @CrossOrigin(maxAge = 60, origins = {"http://localhost:4200"})
    @PostMapping(path = {"/processMultipartHttpServletRequest", "/processMultipartHttpServletRequest/"}, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public Object processMultipartHttpServletRequest(MultipartHttpServletRequest request) {
        return this.serviceProcessor.processRequest(request);
    }

    @CrossOrigin(maxAge = 60, origins = {"http://localhost:4200"})
    @GetMapping(path = {"/api/docs/{assetProviderProcessor}/{assetName}", "/api/docs/{assetProviderProcessor}/{assetName}/"}, consumes = {MediaType.ALL_VALUE})
    public ResponseEntity<byte[]> getAsset(
            @PathVariable String assetProviderProcessor,
            @PathVariable String assetName,
            @PathVariable(required = false) String type,
            HttpServletRequest request
    )  {
        AssetHttpRequest assetHttpRequest = new AssetHttpRequest();
        assetHttpRequest.setAssetName(assetName);
        assetHttpRequest.setRequest(request);
        assetHttpRequest.setAssetProcessorId(ProcessorUtils.fromUrlSafe(assetProviderProcessor));
        //TODO: evtl den Type aus dem contentType = request.getContentType(); bestimmen
        assetHttpRequest.setType(type);

        return (ResponseEntity<byte[]>) this.serviceProcessor.processRequest(assetHttpRequest);
    }

    //Start Incubator Interface Implementierung

    @Override
    public IDescribeSession startDescribe(IDescribeSource source, IDescribePolicy policy) {
        return null;
    }

    @Override
    public IConstructSession<Object> startConstruct(IConstructSource source, IConstructPolicy policy) {
        return null;
    }

    @Override
    public <T> IConstructSession<T> startConstruct(Class<T> clazz, IConstructSource source, IConstructPolicy policy) {
        //session erzeugen

        //BuildTask erzeugen

        //constructionManage erzeugen
        return null;
    }

    @Override
    public IEditSession startEdit(IEditSource source, IEditPolicy policy) {
        return null;
    }

    //End Incubator Interface Implementierung
}