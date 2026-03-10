package de.starima.pfw.base.processor.request.api;

import de.starima.pfw.base.processor.api.IProcessor;

//TODO: zu Ã¼berarbeiten, ist momentan nur eine andere Bezeichnung fÃ¼r den RequestProzessor
public interface IRequestConsumerProcessor extends IProcessor {
    public Object processRequest(Object requestInput);
}