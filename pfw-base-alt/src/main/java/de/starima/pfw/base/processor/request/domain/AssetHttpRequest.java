package de.starima.pfw.base.processor.request.domain;

import lombok.Getter;
import lombok.Setter;

import jakarta.servlet.http.HttpServletRequest;

@Getter @Setter
public class AssetHttpRequest {
    String assetProcessorId;
    String assetName;
    String type;
    HttpServletRequest request;
}