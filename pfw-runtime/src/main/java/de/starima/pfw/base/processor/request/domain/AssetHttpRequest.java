package de.starima.pfw.base.processor.request.domain;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AssetHttpRequest {
    String assetProcessorId;
    String assetName;
    String type;
    HttpServletRequest request;
}