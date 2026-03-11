package de.starima.pfw.base.processor.request;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.request.api.IRequestGatewayProcessor;
import de.starima.pfw.base.processor.request.api.IUrlProviderProcessor;
import de.starima.pfw.base.processor.request.domain.AssetHttpRequest;
import de.starima.pfw.base.util.ProcessorUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * HTTP-Transport-Adapter für das Gateway.
 *
 * <p>Zwei Endpoints:
 * <ul>
 *   <li>POST /api/process — beanParameterMap als JSON Body</li>
 *   <li>GET /api/docs/{provider}/{name} — Asset-Download</li>
 * </ul>
 *
 * <p>Alles geht durch das Gateway. Der Controller ist nur der
 * HTTP-Eingang — die Logik steckt in der Dispatcher-Chain.
 *
 * <p>EINE Stelle für REST: PfwRestController. Keine Ausnahmen.
 */
@Slf4j
@Getter
@Setter
@RestController
@Processor(
        description = "HTTP-Transport-Adapter. Leitet HTTP-Requests " +
                "an das Communication Gateway weiter. EINE Stelle für alle REST-Endpoints.",
        categories = {"request", "rest"},
        tags = {"rest", "http", "spring", "transport"}
)
public class PfwRestController extends AbstractProcessor {

    @ProcessorParameter(value = "requestGateway@provided",
            description = "Das Communication Gateway — muss vom RunLevel RUNTIME bereitgestellt sein.")
    private IRequestGatewayProcessor gateway;

    @ProcessorParameter(description = "Stellt die URL dieses Controllers für Clients bereit.")
    private IUrlProviderProcessor urlProvider;

    // =================================================================
    // POST: beanParameterMap als JSON
    // =================================================================

    @CrossOrigin(maxAge = 60)
    @PostMapping(path = "/api/process", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> processRequest(
            @RequestBody Map<String, Map<String, Object>> beanParameterMap) {
        if (gateway == null) {
            log.warn("PfwRestController: kein Gateway konfiguriert");
            return ResponseEntity.internalServerError().body("Gateway nicht verfügbar.");
        }
        log.debug("PfwRestController: POST /api/process");
        Object response = gateway.processRequest(beanParameterMap);
        return ResponseEntity.ok(response);
    }

    // =================================================================
    // GET: Asset-Download (Dokumentation, Bilder, etc.)
    // =================================================================

    @CrossOrigin(maxAge = 60)
    @GetMapping(path = {
            "/api/docs/{assetProviderProcessor}/{assetName}",
            "/api/docs/{assetProviderProcessor}/{assetName}/"
    }, consumes = MediaType.ALL_VALUE)
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
        assetHttpRequest.setAssetProcessorId(ProcessorUtils.fromUrlSafe(assetProviderProcessor));
        assetHttpRequest.setRequest(request);
        assetHttpRequest.setType(type);

        log.debug("PfwRestController: GET /api/docs/{}/{}", assetProviderProcessor, assetName);
        return (ResponseEntity<byte[]>) gateway.processRequest(assetHttpRequest);
    }
}