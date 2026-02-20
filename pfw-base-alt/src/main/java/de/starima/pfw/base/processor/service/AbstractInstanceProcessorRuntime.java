package de.starima.pfw.base.processor.service;

import de.dzbank.components.utils.log.LogOutputHelper;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.context.domain.DefaultProcessorContext;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.parameter.api.IBeanTypeMapProcessor;
import de.starima.pfw.base.processor.request.api.IRequestDispatcherProcessor;
import de.starima.pfw.base.processor.request.api.IRequestProcessor;
import de.starima.pfw.base.processor.request.api.IResponseDispatcherProcessor;
import de.starima.pfw.base.processor.request.domain.AssetHttpRequest;
import de.starima.pfw.base.processor.service.api.IRuntimeServiceProcessor;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
import java.util.*;

@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Processor
@Service
@RestController
public abstract class AbstractInstanceProcessorRuntime extends AbstractProcessor implements IRuntimeServiceProcessor {
    @ProcessorParameter(value = "defaultBeanTypeMapProcessor", description = "der BeanTypMap Prozessor, der benutzt wird, um aus der beanParameterMap die beanIdTypeMap zu extrahieren und dem hier erzeugten Context zu setzen.")
    private IBeanTypeMapProcessor beanTypeMapProcessor;
    @ProcessorParameter(value = "requestDispatcherProcessorChain", description = "Der RequestDispatcher steuert, ob und wie die Instanz Requests verarbeiten kann. Hier kÃ¶nnen auch Sicherheitsaspekte, Validierung, Logging etc. hinterlegt werden.")
    private IRequestDispatcherProcessor requestDispatcherProcessor;
    @ProcessorParameter(description = "Der ResponseDispatcher kann Antworten an unterschidliche Ziele weiterleiten (REST, Eventbus, Filesystem, Cache, Test etc)")
    private IResponseDispatcherProcessor responseDispatcherProcessor;
    @ProcessorParameter(value = "true")
    private boolean useDefaultBeanParameterMap = true;

    @PostConstruct
    public void initProcessor() {
        try {
            log.info("Start bootstrap ...");
            setScope(ProcessorScope.instance);
            this.init(null);
        } catch (Exception e) {
            log.error("Can not initialize instance processor!", e);
        }
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
        return this.processRequest(request);
    }


    protected Object processRequest(Object request) {
        //TODO: Exception handling
        log.info("{}: processRequest of type {} ...", getFullBeanId(), request.getClass().getName());
        if (requestDispatcherProcessor != null) {
            Object response = requestDispatcherProcessor.dispatchRequest(request);

            if (responseDispatcherProcessor != null) {
                response = responseDispatcherProcessor.dispatchResponse(response);
            }

            if (response != null) {
                return response;
            }
        }
        log.info("{}: processRequest - no dispatcher found or is responsible for request", getFullBeanId());
        return ResponseEntity.notFound().build();
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

        return (ResponseEntity<byte[]>) this.processRequest(assetHttpRequest);
    }

}