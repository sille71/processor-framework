package de.starima.pfw.base.processor.request;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.request.api.IRequestGatewayProcessor;
import de.starima.pfw.base.processor.request.domain.AssetHttpRequest;
import de.starima.pfw.base.util.ProcessorUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.Map;

/**
 * Spring REST-Controller für das PFW Gateway.
 *
 * <p>Leitet HTTP-Requests an das Gateway weiter.
 * EINE Stelle für alle REST-Endpoints — nicht verteilt über Kernel und Services.
 *
 * <p>Konfigurierbar: das Gateway muss vom RunLevel RUNTIME bereitgestellt sein.
 */
@Slf4j
@Getter
@Setter
@RestController
@Processor(
        description = "Spring REST-Controller. Leitet HTTP-Requests an das Gateway weiter. " +
                "EINE Stelle für alle REST-Endpoints.",
        categories = {"request", "rest"},
        tags = {"rest", "http", "spring", "controller"}
)
public class PfwRestController extends AbstractProcessor {

    @ProcessorParameter(value = "requestGateway@provided",
            description = "Das Gateway — muss vom RunLevel RUNTIME bereitgestellt sein.")
    private IRequestGatewayProcessor gateway;

    @CrossOrigin(maxAge = 60)
    @PostMapping(path = "/processBeanParameterMapRequest",
            consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> processBeanParameterMapRequest(
            @RequestBody Map<String, Map<String, Object>> beanParameterMap) {
        log.info("PfwRestController: processBeanParameterMapRequest");
        if (gateway == null) {
            log.warn("PfwRestController: kein Gateway konfiguriert");
            return ResponseEntity.internalServerError().body("Gateway nicht konfiguriert.");
        }
        return ResponseEntity.ok(gateway.processRequest(beanParameterMap));
    }

    @CrossOrigin(maxAge = 60)
    @PostMapping(path = "/processMultipartRequest",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> processMultipartRequest(
            @RequestPart String requestProcessorIdentifier,
            @RequestPart Map<String, Map<String, Object>> beanParameterMap,
            @RequestPart MultipartFile file) {
        log.info("PfwRestController: processMultipartRequest requestProcessor={}", requestProcessorIdentifier);
        if (gateway == null) {
            log.warn("PfwRestController: kein Gateway konfiguriert");
            return ResponseEntity.internalServerError().body("Gateway nicht konfiguriert.");
        }
        return ResponseEntity.ok(gateway.processRequest(beanParameterMap));
    }

    @CrossOrigin(maxAge = 60, origins = {"http://localhost:4200"})
    @PostMapping(path = {"/processMultipartHttpServletRequest", "/processMultipartHttpServletRequest/"},
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public Object processMultipartHttpServletRequest(MultipartHttpServletRequest request) {
        if (gateway == null) {
            log.warn("PfwRestController: kein Gateway konfiguriert");
            return null;
        }
        return gateway.processRequest(request);
    }

    @CrossOrigin(maxAge = 60, origins = {"http://localhost:4200"})
    @GetMapping(path = {"/api/docs/{assetProviderProcessor}/{assetName}",
            "/api/docs/{assetProviderProcessor}/{assetName}/"},
            consumes = {MediaType.ALL_VALUE})
    public ResponseEntity<byte[]> getAsset(
            @PathVariable String assetProviderProcessor,
            @PathVariable String assetName,
            @PathVariable(required = false) String type,
            HttpServletRequest request) {
        if (gateway == null) {
            log.warn("PfwRestController: kein Gateway konfiguriert");
            return ResponseEntity.internalServerError().build();
        }
        AssetHttpRequest assetHttpRequest = new AssetHttpRequest();
        assetHttpRequest.setAssetName(assetName);
        assetHttpRequest.setRequest(request);
        assetHttpRequest.setAssetProcessorId(ProcessorUtils.fromUrlSafe(assetProviderProcessor));
        assetHttpRequest.setType(type);
        return (ResponseEntity<byte[]>) gateway.processRequest(assetHttpRequest);
    }
}