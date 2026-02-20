package de.starima.pfw.base.processor.request.api;

import de.starima.pfw.base.processor.api.IProcessor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartHttpServletRequest;

//TODO: zu Ã¼berarbeiten, ist momentan nur eine andere Bezeichnung fÃ¼r den RequestProzessor
public interface IRequestConsumerProcessor extends IProcessor {
    public Object processRequest(Object requestInput);
}