package de.starima.pfw.base.processor.request;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.request.api.IUrlProviderProcessor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Processor(description = "Stellt eine URL zur VerfÃ¼gung, z.B. die zentrale Schnittstelle einer Instanz.")
public class DefaultUrlProviderProcessor extends AbstractProcessor implements IUrlProviderProcessor {
    @ProcessorParameter(value = "http://localhost:8082/")
    private String url;
    @ProcessorParameter(value = "")
    private String description;
}