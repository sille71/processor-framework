package de.starima.pfw.base.processor.locale;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.attribute.api.IAttribute;
import de.starima.pfw.base.processor.attribute.api.IAttributeProviderProcessor;
import de.starima.pfw.base.processor.locale.api.ILocalProviderProcessor;

import java.util.Locale;

@Processor
public class DefaultLocalProviderProcessor extends AbstractProcessor implements ILocalProviderProcessor {
    @ProcessorParameter(value = "de")
    private String currentLocale = "de";
    @ProcessorParameter(description = "Attribute Provider zum AuflÃ¶sen der Attributreferenzen")
    private IAttributeProviderProcessor attributeProviderProcessor;

    @Override
    public Locale getLocaleForSubjectReference(String itemReference) {
        if (this.attributeProviderProcessor != null) {
            return this.getLocaleForSubject(this.attributeProviderProcessor.getAttribute(itemReference));
        }
        return this.getLocale();
    }

    @Override
    public Locale getContextLocal() {
        return getRuntimeContext().getLocale();
    }

    @Override
    public Locale getLocaleForSubject(Object subject) {
        if (subject instanceof IAttribute)
            return ((IAttribute)subject).getLanguage() != null ? new Locale(((IAttribute)subject).getLanguage()) : this.getLocale();
        return this.getLocale();
    }

    @Override
    public Locale getLocale() {
        return new Locale(this.currentLocale);
    }
}