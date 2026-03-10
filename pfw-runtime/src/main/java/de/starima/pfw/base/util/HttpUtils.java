package de.starima.pfw.base.util;

import de.starima.pfw.base.processor.api.IProcessor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Objects;

public class HttpUtils {
    public static MultiValueMap<String, Object> createProcessorHttpFormData(IProcessor processor) {
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("requestProcessorIdentifier", new HttpEntity<>(processor.getFullBeanId(), createTextHeaders(processor.getFullBeanId() != null ? processor.getFullBeanId().length() : 0)));
        formData.add("beanParameterMap", new HttpEntity<>(MapUtils.getJsonRepresentation(processor.extractEffectiveProcessorParameterMap()), createJsonHeaders(MapUtils.getJsonRepresentation(processor.extractEffectiveProcessorParameterMap()).length())));
        return formData;
    }

    public static HttpHeaders createTextHeaders(long length) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.TEXT_PLAIN);
        httpHeaders.setContentLength(length);
        return httpHeaders;
    }

    public static HttpHeaders createJsonHeaders(long length) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setContentLength(length);
        return httpHeaders;
    }

    public static HttpHeaders createMultipartHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        return httpHeaders;
    }

    public static HttpHeaders createOCTETHeaders(long length, String attachmentName, String filename) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        httpHeaders.setContentLength(length);
        httpHeaders.setContentDisposition(ContentDisposition.attachment().name(attachmentName).filename(Objects.requireNonNull(filename)).build());
        return httpHeaders;
    }

    public static HttpHeaders createXmlHeaders(long length, String attachmentName, String filename) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_XML);
        httpHeaders.setContentLength(length);
        httpHeaders.setContentDisposition(ContentDisposition.attachment().name(attachmentName).filename(Objects.requireNonNull(filename)).build());
        return httpHeaders;
    }
}
