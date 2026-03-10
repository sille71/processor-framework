package de.starima.pfw.base.processor.service;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.domain.ProcessorScope;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.parameter.api.IBeanTypeMapProcessor;
import de.starima.pfw.base.processor.request.api.IRequestDispatcherProcessor;
import de.starima.pfw.base.processor.request.api.IRequestProcessor;
import de.starima.pfw.base.processor.request.api.IResponseDispatcherProcessor;
import de.starima.pfw.base.processor.request.domain.AssetHttpRequest;
import de.starima.pfw.base.util.LogOutputHelper;
import de.starima.pfw.base.util.ProcessorUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.Collections;
import java.util.Map;

//TODO: benötigen wir die Service Klasse noch? Oder nennen wir sie um? Früher war es der InstanceProcessor, das ändert sich aber mit dem Incubator

@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Processor
@Service
@RestController
public abstract class AbstractInstanceProcessorRuntime extends AbstractProcessor  {
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
            //TODO: wird vom Incubator gemacht
            this.init(null);
        } catch (Exception e) {
            log.error("Can not initialize instance processor!", e);
        }
    }

    //TODO: die origins sind zu spezifizieren (am besten konfigurativ durch einen Prozessor - dieser bekommt einen httpRequest und liefert den passenden Header)
    //siehe rcn_backlog
    @CrossOrigin(maxAge = 60)
    @PostMapping(path = "/processBeanParameterMapRequest", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String,Map<String, Object>>> processBeanParameterMapRequest(@RequestBody Map<String,Map<String, Object>> beanParameterMap) {
        log.info("call processBeanParameterMapRequest with data {}", LogOutputHelper.toLogString(beanParameterMap));
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
        log.info("call processMultipartRequest with request processor {} and beanParameterMap data {}",requestProcessorIdentifier, LogOutputHelper.toLogString(beanParameterMap));
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